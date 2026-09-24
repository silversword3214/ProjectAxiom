package silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

public final class EntityMaskRenderTarget extends RenderTarget {

    private static final Vector4f CLEAR = new Vector4f(0f, 0f, 0f, 0f);  // KIRKAS PUNAINEN, läpinäkymätön

    private GpuSampler sampler;
    private int trackedW = -1, trackedH = -1;

    public EntityMaskRenderTarget() {
        super("ShaderEspMask", GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
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

    public void clearMask() {
        if (colorTexture == null || depthTexture == null) return;
        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        // Reverse-Z: 0.0 = kaukana, 1.0 = lähellä. Clear "kaukana" että kaikki piirtyy.
        enc.clearColorAndDepthTextures(colorTexture, CLEAR, depthTexture, 0.0);
        enc.submit();
    }

    public GpuSampler sampler() { return sampler; }

    @Override
    public String toString() {
        return "ShaderEspMask[" + width + "x" + height + "]";
    }
}