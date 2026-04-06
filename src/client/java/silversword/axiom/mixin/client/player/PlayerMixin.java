package silversword.axiom.mixin.client.player;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.SafeWalk;


@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void onIsStayingOnGroundSurface(CallbackInfoReturnable<Boolean> cir) {
        SafeWalk safeWalk = ModuleManager.getInstance().getModule(SafeWalk.class);
        if (safeWalk != null && safeWalk.isEnabled() && SafeWalk.shouldPreventFall) {
            cir.setReturnValue(true);
        }
    }

}