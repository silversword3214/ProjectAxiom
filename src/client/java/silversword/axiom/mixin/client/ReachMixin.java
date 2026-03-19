package silversword.axiom.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.combat.Reach;
import silversword.axiom.client.managers.ModuleManager;
import net.minecraft.client.MinecraftClient;

@Mixin(ClientPlayerInteractionManager.class)
public class ReachMixin {

    // Muisti edellisestä reach-arvosta
    private double lastReachValue = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Reach reach = ModuleManager.getInstance().getModule(Reach.class);

        double targetValue = 4.5; // default
        if (reach != null && reach.isEnabled()) {
            targetValue = reach.getReachDistance();
        }

        // Tarkistetaan onko arvo muuttunut
        if (lastReachValue != targetValue) {
            try {
                mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE)
                        .setBaseValue(targetValue);
                mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE)
                        .setBaseValue(targetValue);

                if (reach != null && reach.isEnabled()) {

                } else {

                }

                lastReachValue = targetValue; // päivitä muisti
            } catch (Exception e) {
                System.out.println("[Reach] Error: " + e.getMessage());
            }
        }
    }
}
