package silversword.axiom.client.render.rendersystem.axiomrenderer.blockchams;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

/** Block chams -maskin render target. Oma versio ShaderESP:n vastaavasta. */
public final class BlockMaskRenderTarget extends RenderTarget {

    private static final Vector4f CLEAR = new Vector4f(0f, 0f, 0f, 0f);

    private GpuSampler sampler;
    private int trackedW = -1, trackedH = -1;

    public BlockMaskRenderTarget(String label) {
        super(label, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    }

    public void syncToWindow(int downscale) {
        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth()  / Math.max(1, downscale);
        int h = mc.getWindow().getHeight() / Math.max(1, downscale);
        if (w <= 0 || h <= 0) return;

        if (colorTexture == null || depthTexture == null
                || trackedW != w || trackedH != h) {
            super.resize(w, h);
            trackedW = w;
            trackedH = h;
        }

        if (sampler == null) {
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }
    }

    /**
     * Reverse-Z: depth 0.0 = kaukana, 1.0 = lähellä.
     * Clear 0.0 → kaikki piirretään päälle (koska kaikki on "kauempana" kuin 0.0?).
     * Clear 1.0 → kaikki mitä piirretään on "kauempana" kuin 1.0 → piirtyy.
     * Käytetään 0.0:aa kuten ShaderESP:ssä.
     */
    public void clearMask() {
        if (colorTexture == null || depthTexture == null) return;
        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        enc.clearColorAndDepthTextures(colorTexture, CLEAR, depthTexture, 0.0);
        enc.submit();
    }

    public GpuSampler sampler() { return sampler; }

    @Override
    public String toString() {
        return "BlockMask[" + width + "x" + height + "]";
    }
}