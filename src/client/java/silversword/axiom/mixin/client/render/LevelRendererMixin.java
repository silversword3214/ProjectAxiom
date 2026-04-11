package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess.ShaderRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderLevelTail(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float tickDelta = RenderUtils.getTickDelta();
        Camera camera = mc.gameRenderer.getMainCamera();

        Matrix4f projection = RenderUtils.getProjectionMatrix(tickDelta);
        Matrix4f view = RenderUtils.getViewMatrix(camera);

        RenderAPI api = RenderAPI.getInstance();
        Renderer3D renderer = new Renderer3D(api.getCore(), projection, view, tickDelta);

        Render3DEvent event = new Render3DEvent(
                renderer,
                tickDelta,
                camera.position(),
                projection,
                view
        );

        AxiomInitialize.EVENT_BUS.post(event);
        api.end();

        ShaderRenderer.getInstance().render(camera, tickDelta);
    }
}