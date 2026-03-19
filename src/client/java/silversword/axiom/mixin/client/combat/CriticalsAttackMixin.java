package silversword.axiom.mixin.client.combat;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.combat.Criticals;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class CriticalsAttackMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void axiom$critBeforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        Criticals crits = ModuleManager.getInstance().getModule(Criticals.class);
        if (crits != null) crits.tryDoPacketCrit(target);
    }
}
