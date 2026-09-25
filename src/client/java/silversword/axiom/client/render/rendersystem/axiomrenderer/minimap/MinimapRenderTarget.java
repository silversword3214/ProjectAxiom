package silversword.axiom.client.render.rendersystem.axiomrenderer.minimap;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import org.joml.Vector4f;

public final class MinimapRenderTarget extends RenderTarget {

    /** Läpinäkyvä clear — minimap näyttää "tyhjältä" chunkkien ulkopuolella. */
    private static final Vector4f CLEAR_COLOR = new Vector4f(0f, 0f, 0f, 0f);
    /** Reverse-Z: 0.0 = kaukana, 1.0 = lähellä. */
    private static final double CLEAR_DEPTH = 0.0;

    private GpuSampler sampler;
    private int trackedW = -1;
    private int trackedH = -1;
    private int size = 256;

    public MinimapRenderTarget() {
        super("Minimap", GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    }

    /** Asettaa minimapin resoluution (esim. 128, 256, 512). */
    public void setSize(int newSize) {
        this.size = Math.max(32, Math.min(1024, newSize));
    }

    public void syncToSize() {
        RenderSystem.assertOnRenderThread();
        if (colorTexture == null || depthTexture == null
                || trackedW != size || trackedH != size) {
            super.resize(size, size);
            trackedW = size;
            trackedH = size;
        }

        if (sampler == null) {
            // LINEAR = pehmeä, NEAREST = pikselimäinen. Minimapille LINEAR näyttää paremmalta.
            sampler = RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, false);
        }
    }

    public void clear() {
        if (colorTexture == null || depthTexture == null) return;
        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        enc.clearColorAndDepthTextures(colorTexture, CLEAR_COLOR, depthTexture, CLEAR_DEPTH);
        enc.submit();
    }

    public GpuSampler sampler() { return sampler; }

    @Override
    public String toString() {
        return "MinimapRT[" + width + "x" + height + "]";
    }
}