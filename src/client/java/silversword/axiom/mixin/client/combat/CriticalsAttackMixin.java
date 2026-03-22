package silversword.axiom.mixin.client.combat;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.combat.Criticals;

@Mixin(MultiPlayerGameMode.class)
public abstract class CriticalsAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void axiom$critBeforeAttack(Player player, Entity target, CallbackInfo ci) {
        Criticals crits = ModuleManager.getInstance().getModule(Criticals.class);
        if (crits != null) crits.tryDoPacketCrit(target);
    }
}
