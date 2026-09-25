package silversword.axiom.client.render.rendersystem.axiomrenderer.blockchams;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;

/** Kääre RT:n väritekstuurille. Sama idea kuin ShaderESP:n MaskTexture. */
public final class BlockMaskTexture extends AbstractTexture {

    public BlockMaskTexture(RenderTarget rt) {
        this.texture     = rt.getColorTexture();
        this.textureView = rt.getColorTextureView();
        this.sampler     = RenderSystem.getSamplerCache()
                .getClampToEdge(FilterMode.NEAREST);
    }

    @Override
    public void close() {
        // RT omistaa tekstuurin — älä sulje
        this.texture = null;
        this.textureView = null;
        this.sampler = null;
    }
}