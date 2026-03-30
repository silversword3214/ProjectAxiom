package silversword.axiom.mixin.client.net;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.misc.PingSpoof;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin {

    @Inject(method = "getLatency", at = @At("RETURN"), cancellable = true)
    private void spoofPing(CallbackInfoReturnable<Integer> cir) {
        PingSpoof module = PingSpoof.INSTANCE;
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(module.getFakePing());
        }
    }
}