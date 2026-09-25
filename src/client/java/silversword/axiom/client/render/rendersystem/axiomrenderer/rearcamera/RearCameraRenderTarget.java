package silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

public final class RearCameraRenderTarget extends RenderTarget {

    private static final Vector4f CLEAR_COLOR = new Vector4f(1f, 0f, 1f, 1f); // magenta
    private static final double CLEAR_DEPTH = 1.0;

    private GpuSampler sampler;
    private int trackedWidth  = -1;
    private int trackedHeight = -1;

    public RearCameraRenderTarget() {
        super("RearCamera", GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    }

    public void syncToWindow() {
        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return;

        if (this.colorTexture == null || this.depthTexture == null
                || trackedWidth != w || trackedHeight != h) {
            super.resize(w, h);
            trackedWidth  = w;
            trackedHeight = h;
        }

        if (sampler == null) {
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
        return "RearCameraRT[" + width + "x" + height
                + (colorTextureView != null ? ", ready" : ", not-ready") + "]";
    }
}