package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoHurtCam;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera.RearCameraRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "close", at = @At("RETURN"))
    private void axiom$onGameRendererClose(CallbackInfo ci) {
        silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp.ShaderEspRenderer.shutdown();
        RenderAPI.getInstance().close();
    }




    @Inject(
            method = "bobView",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$cancelViewBobbing(
            CameraRenderState cameraState,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        NoViewBobbingTilt module =
                ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);

        if (module != null && module.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "bobHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$cancelHurtTilt(
            CameraRenderState cameraState,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        NoViewBobbingTilt noViewBobbing =
                ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);

        if (noViewBobbing != null && noViewBobbing.isEnabled()) {
            ci.cancel();
            return;
        }

        NoHurtCam noHurtCam =
                ModuleManager.getInstance().getModule(NoHurtCam.class);

        if (noHurtCam != null && noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void axiom$overrideMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget override = RearCameraRenderer.getRenderTargetOverride();
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}

