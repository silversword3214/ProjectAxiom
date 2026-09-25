package silversword.axiom.client.render.rendersystem.axiomrenderer.minimap;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class MinimapTexture extends AbstractTexture {

    public MinimapTexture(RenderTarget rt) {
        this.texture     = rt.getColorTexture();
        this.textureView = rt.getColorTextureView();
        this.sampler     = RenderSystem.getSamplerCache()
                .getClampToEdge(FilterMode.LINEAR);
    }

    @Override
    public void close() {
        // RT omistaa tekstuurin — älä sulje
        this.texture = null;
        this.textureView = null;
        this.sampler = null;
    }
}