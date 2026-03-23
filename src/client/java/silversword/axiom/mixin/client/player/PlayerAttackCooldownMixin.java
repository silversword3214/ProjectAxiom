package silversword.axiom.mixin.client.player;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.combat.NoHitDelay;

@Mixin(Player.class)
public abstract class PlayerAttackCooldownMixin {

    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void overrideCooldown(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (NoHitDelay.enabled) {
            cir.setReturnValue(1.0f);
        }
    }
}