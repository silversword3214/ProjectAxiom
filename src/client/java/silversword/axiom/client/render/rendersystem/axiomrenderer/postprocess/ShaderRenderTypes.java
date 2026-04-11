package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderPipelines;

public final class ShaderRenderTypes {
    private static final OutputTarget MASK_TARGET = new OutputTarget(
            "projectaxiom_shader_esp_mask",
            () -> ShaderRenderer.getInstance().getFramebufferManager().getMaskTarget()
    );

    private static RenderType maskEntity;

    private ShaderRenderTypes() {}

    public static RenderType maskEntity() {
        if (maskEntity == null) {
            maskEntity = RenderType.create(
                    "projectaxiom_shader_esp_mask",
                    RenderSetup.builder(RenderPipelines.ENTITY_MASK)
                            .setOutputTarget(MASK_TARGET)
                            .setOutline(OutlineProperty.NONE)
                            .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                            .createRenderSetup()
            );
        }
        return maskEntity;
    }
}