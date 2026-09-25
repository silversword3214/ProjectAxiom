package silversword.axiom.client.render.rendersystem.axiomrenderer.minimap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Pikselipuskuri joka kirjoitetaan suoraan GPU-tekstuuriin.
 * Ei käytä RenderCorea lainkaan — ei vertex-dataa, ei draw calleja.
 *
 * Puskurin rakenne: int[] ARGB.
 * GPU:lle viedään ByteBuffer ABGR-muodossa (little-endian RGBA8).
 */
public final class MinimapSurface extends AbstractTexture {

    // Usage: USAGE_COPY_DST | USAGE_TEXTURE_BINDING = 1 | 4 = 5
    private static final int USAGE = 1 | 4;

    private final int[] pixels;
    private final ByteBuffer staging;
    private final int size;

    public MinimapSurface(int size) {
        this.size = size;
        this.pixels = new int[size * size];
        this.staging = ByteBuffer.allocateDirect(size * size * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Luo GPU-tekstuuri
        GpuTexture tex = RenderSystem.getDevice().createTexture(
                () -> "MinimapSurface",
                USAGE,
                GpuFormat.RGBA8_UNORM,
                size, size, 1, 1);

        this.texture = tex;
        this.textureView = RenderSystem.getDevice().createTextureView(tex);
        this.sampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.NEAREST,  // Pikselintarkka minimap
                FilterMode.NEAREST,
                false);
    }

    public int size() { return size; }
    public int[] pixels() { return pixels; }

    /** Tyhjentää puskurin annetulla värillä. */
    public void clear(int argb) {
        java.util.Arrays.fill(pixels, argb);
    }

    /** Asettaa yksittäisen pikselin. */
    public void setPixel(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= size || y >= size) return;
        pixels[y * size + x] = argb;
    }

    /**
     * Täyttää suorakaiteen. Koordinaatit ovat pikseliavaruudessa.
     * Käytetään suoraan puskuriin, ei vertex-dataa.
     */
    public void fillRect(int x1, int y1, int x2, int y2, int argb) {
        if (x1 < 0) x1 = 0;
        if (y1 < 0) y1 = 0;
        if (x2 > size) x2 = size;
        if (y2 > size) y2 = size;
        if (x1 >= x2 || y1 >= y2) return;

        for (int y = y1; y < y2; y++) {
            int row = y * size;
            for (int x = x1; x < x2; x++) {
                pixels[row + x] = argb;
            }
        }
    }

    /**
     * Lataa pikselipuskurin GPU-tekstuuriin.
     * YKSI writeToTexture-kutsu per frame — ei draw calleja.
     */
    public void uploadToGpu() {
        staging.clear();

        // KRIITTINEN: kirjoita rivit käänteisesti — alarivi ensin.
        // OpenGL:n tekstuuri (0,0) on vasen alakulma, mutta meidän
        // pixels[0] on minimapin ylärivi. Kääntämällä rivit saadaan
        // tekstuuri oikeinpäin.
        for (int y = size - 1; y >= 0; y--) {
            int row = y * size;
            for (int x = 0; x < size; x++) {
                int argb = pixels[row + x];
                int abgr = (argb & 0xFF000000)
                        | ((argb & 0x00FF0000) >>> 16)
                        | (argb & 0x0000FF00)
                        | ((argb & 0x000000FF) << 16);
                staging.putInt(abgr);
            }
        }

        staging.flip();

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        enc.writeToTexture(this.texture, staging, 0, 0, 0, 0, size, size);
        enc.submit();
    }

    /**
     * Piirtää täytetyn ympyrän pikselipuskuriin.
     * Kutsutaan mobien piirtoon — ei vertex-dataa, ei draw calleja.
     */
    public static void fillCirclePixels(int[] pixels, int size,
                                        int cx, int cy, int radius, int argb) {
        if (radius <= 0) {
            if (cx >= 0 && cy >= 0 && cx < size && cy < size) {
                pixels[cy * size + cx] = argb;
            }
            return;
        }

        int r2 = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int y = cy + dy;
            if (y < 0 || y >= size) continue;
            int row = y * size;
            int dxMax = (int) Math.sqrt(r2 - dy * dy);
            int xStart = Math.max(0, cx - dxMax);
            int xEnd = Math.min(size - 1, cx + dxMax);
            for (int x = xStart; x <= xEnd; x++) {
                pixels[row + x] = argb;
            }
        }
    }

    @Override
    public void close() {
        super.close();
    }
}