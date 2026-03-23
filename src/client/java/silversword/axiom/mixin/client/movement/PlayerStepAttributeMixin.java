package silversword.axiom.mixin.client.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.movement.Step;

@Mixin(Player.class)
public abstract class PlayerStepAttributeMixin {

    // Identifier-based modifier id (1.21+)
    private static final Identifier AXIOM_STEP_MOD = Identifier.fromNamespaceAndPath("projectaxiom", "step_height");

    @Inject(method = "tick", at = @At("TAIL"))
    private void axiom$applyStep(CallbackInfo ci) {
        // vain local player
        if ((Object) this != Minecraft.getInstance().player) return;

        Player p = (Player) (Object) this;

        AttributeInstance inst = p.getAttribute(Attributes.STEP_HEIGHT);
        if (inst == null) return;

        // poista vanha aina
        inst.removeModifier(AXIOM_STEP_MOD);

        if (!Step.isEnabledGlobal()) return;

        double target = Step.getStepHeight();   // esim 1.5 / 2.0 / 2.5
        double add = Math.max(0.0, target - 1.0); // vanilla 1.0

        if (add > 0.0) {
            inst.addTransientModifier(new AttributeModifier(
                    AXIOM_STEP_MOD,
                    add,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
}
