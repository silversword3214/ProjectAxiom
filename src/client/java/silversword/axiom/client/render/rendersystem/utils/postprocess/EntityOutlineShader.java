package silversword.axiom.client.render.rendersystem.utils.postprocess;

import net.minecraft.entity.Entity;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.ShaderESP;

import silversword.axiom.client.render.rendersystem.BufferRenderer;
import silversword.axiom.client.render.rendersystem.CustomRenderingPipelineProvider;

public class EntityOutlineShader extends EntityShader {
    private static ShaderESP esp;

    public EntityOutlineShader() {
        super(CustomRenderingPipelineProvider.POST_OUTLINE);

    }

    @Override
    protected boolean shouldDraw() {
        if (esp == null) esp = ModuleManager.getInstance().getModule(ShaderESP.class);
        boolean result = esp != null && esp.isShader();
        return result;
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        return shouldDraw() && !esp.shouldSkip(entity);
    }

    @Override
    protected void setupPass(BufferRenderer renderer) {
        // Käytetään suoraan ShaderESP:n tarjoamaa indeksimetodia
        int shapeIndex = esp.getShapeModeIndex();


        // Lähetetään uniformit
        renderer.uniform("OutlineData", OutlineUniforms.write(
                esp.getOutlineWidth(),
                esp.getFillOpacity(),
                shapeIndex,
                esp.getGlowMultiplier()
        ));
    }
}