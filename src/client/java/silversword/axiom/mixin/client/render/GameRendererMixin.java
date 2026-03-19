package silversword.axiom.mixin.client.render;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.event.GetFovEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {




    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void axiom$cancelViewBobbing(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoViewBobbingTilt m = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        if (m != null && m.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void axiom$cancelHurtTilt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoViewBobbingTilt m = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        if (m != null && m.isEnabled() && m.disableHurtTilt.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValue();
        GetFovEvent event = GetFovEvent.get(fov);
        AxiomInitialize.EVENT_BUS.post(event);
        cir.setReturnValue(event.fov);
    }
}