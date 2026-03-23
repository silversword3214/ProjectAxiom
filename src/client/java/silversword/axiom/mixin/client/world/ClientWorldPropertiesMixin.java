package silversword.axiom.mixin.client.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.world.TimeChanger;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$ClientLevelData")
public class ClientWorldPropertiesMixin {

    @Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        TimeChanger mod = ModuleManager.getInstance().getModule(TimeChanger.class);
        if (mod != null && mod.isEnabled()) {
            long forcedTime = mod.getForcedTime();
            if (forcedTime >= 0) {
                cir.setReturnValue(forcedTime);
            }
        }
    }
}