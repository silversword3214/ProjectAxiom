package silversword.axiom.mixin.client.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.movement.Step;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityStepAttributeMixin {

    // Identifier-based modifier id (1.21+)
    private static final Identifier AXIOM_STEP_MOD = Identifier.of("projectaxiom", "step_height");

    @Inject(method = "tick", at = @At("TAIL"))
    private void axiom$applyStep(CallbackInfo ci) {
        // vain local player
        if ((Object) this != MinecraftClient.getInstance().player) return;

        PlayerEntity p = (PlayerEntity) (Object) this;

        EntityAttributeInstance inst = p.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (inst == null) return;

        // poista vanha aina
        inst.removeModifier(AXIOM_STEP_MOD);

        if (!Step.isEnabledGlobal()) return;

        double target = Step.getStepHeight();   // esim 1.5 / 2.0 / 2.5
        double add = Math.max(0.0, target - 1.0); // vanilla 1.0

        if (add > 0.0) {
            inst.addTemporaryModifier(new EntityAttributeModifier(
                    AXIOM_STEP_MOD,
                    add,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
}
