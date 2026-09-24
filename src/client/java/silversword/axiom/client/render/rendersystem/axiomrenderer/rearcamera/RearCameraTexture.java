package silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class RearCameraTexture extends AbstractTexture {

    public RearCameraTexture(RenderTarget rt) {
        this.texture     = rt.getColorTexture();
        this.textureView = rt.getColorTextureView();
        this.sampler     = RenderSystem.getSamplerCache()
                .getClampToEdge(FilterMode.LINEAR);
    }

    @Override
    public void close() {
        this.texture = null;
        this.textureView = null;
        this.sampler = null;
    }
}