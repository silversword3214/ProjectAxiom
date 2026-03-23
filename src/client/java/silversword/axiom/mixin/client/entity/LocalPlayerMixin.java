package silversword.axiom.mixin.client.entity;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.RotationHandler;
import silversword.axiom.client.modules.movement.NoSlow;


@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onPreSendMovementPackets(CallbackInfo ci) {
        RotationHandler.onPreSendMovementPackets();
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onPostSendMovementPackets(CallbackInfo ci) {
        RotationHandler.onPostSendMovementPackets();
    }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void onGetActiveItemSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noItemUseSlow.get()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "isMovingSlowly", at = @At("RETURN"), cancellable = true)
    private void onShouldSlowDown(CallbackInfoReturnable<Boolean> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSneakingSlow.get()) {
            cir.setReturnValue(false);
        }
    }
}