package silversword.axiom.client.rendersystem.utils.minimap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class MinimapSurface extends AbstractTexture {

    private static final int USAGE = 1 | 4;

    private final int[] pixels;
    private final ByteBuffer staging;
    private final int size;

    public MinimapSurface(int size) {
        this.size = size;
        this.pixels = new int[size * size];
        this.staging = ByteBuffer.allocateDirect(size * size * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

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
                FilterMode.NEAREST,
                FilterMode.NEAREST,
                false);
    }

    public int size() { return size; }
    public int[] pixels() { return pixels; }

    public void clear(int argb) {
        java.util.Arrays.fill(pixels, argb);
    }


    public void uploadToGpu() {
        staging.clear();

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