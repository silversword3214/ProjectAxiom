package silversword.axiom.client.render.rendersystem.utils.postprocess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.world.entity.Entity;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.Chams;
import silversword.axiom.client.render.rendersystem.BufferRenderer;
import silversword.axiom.client.render.rendersystem.CustomRenderingPipelineProvider;


public class ChamsShader extends EntityShader {

    private static Chams chams;

    public ChamsShader() {
        super(CustomRenderingPipelineProvider.POST_CHAMS);
    }

    @Override
    protected boolean shouldDraw() {
        if (chams == null) chams = ModuleManager.getInstance().getModule(Chams.class);
        return chams != null && chams.isEnabled();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        return shouldDraw() && chams.shouldDraw(entity);
    }

    @Override
    protected void setupPass(BufferRenderer renderer) {
        var framebuffer = getFramebuffer();
        var textureView = framebuffer.getColorTextureView();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST); // NEAREST on nopea post-processille

        renderer.sampler("u_Texture", textureView, sampler);
        renderer.sampler("u_TextureI", textureView, sampler);

        // Lähetetään ImageData-uniform (image.frag odottaa sitä)
        renderer.uniform("ImageData", ImageUniforms.write(1.0f, 1.0f, 1.0f, 1.0f));
    }
}