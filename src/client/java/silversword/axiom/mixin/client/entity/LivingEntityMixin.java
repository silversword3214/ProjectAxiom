package silversword.axiom.mixin.client.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.combat.AntiKnockback;
import silversword.axiom.client.modules.movement.NoSlow;
import silversword.axiom.client.modules.render.AntiBlind;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // --- AntiKnockback: Knockback removing
    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), argsOnly = true, index = 1)
    private double modifyKnockbackStrength(double strength) {
        AntiKnockback mod = ModuleManager.getInstance().getModule(AntiKnockback.class);
        if (mod != null && mod.isEnabled()) {
            // knockbackPercent on slider, getValue() palauttaa double-arvon (0-100)
            return strength * (mod.knockbackPercent.getValue() / 100.0);
        }
        return strength;
    }

    // --- AntiBlind: blindness & darkness ---
    private boolean shouldRemoveEffect(RegistryEntry<StatusEffect> effect) {
        if (!((Object) this instanceof ClientPlayerEntity)) return false;
        AntiBlind mod = ModuleManager.getInstance().getModule(AntiBlind.class);
        if (mod == null || !mod.isEnabled() || !mod.noDarkness.get()) return false;
        return effect == StatusEffects.BLINDNESS || effect == StatusEffects.DARKNESS;
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (shouldRemoveEffect(effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onGetStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<StatusEffectInstance> cir) {
        if (shouldRemoveEffect(effect)) {
            cir.setReturnValue(null);
        }
    }

    // --- NoSlow: block slow ---
    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow == null || !noSlow.isEnabled()) return;

        float original = cir.getReturnValue();
        if (original < 1.0f) {
            if (noSlow.noSoulSandSlow.get() || noSlow.noSlimeBlockSlow.get() || noSlow.noBerryBushSlow.get()) {
                cir.setReturnValue(1.0f);
            }
        }
    }

    // --- NoSlow: water slow ---
    @Inject(method = "getBaseWaterMovementSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void onGetBaseWaterMovementSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow == null || !noSlow.isEnabled()) return;

        float original = cir.getReturnValue(); // oletus 0.8
        if (original < 1.0f && noSlow.noWaterSlow.get()) {
            cir.setReturnValue(1.0f);
        }
    }

    // --- NoSlow: slowness potion ---
    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffectNoSlow(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect == StatusEffects.SLOWNESS) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onGetStatusEffectNoSlow(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<StatusEffectInstance> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect == StatusEffects.SLOWNESS) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddStatusEffect(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect.getEffectType() == StatusEffects.SLOWNESS) {
            cir.setReturnValue(false); // Estetään efektin lisääminen
        }
    }

    // --- NoSlow: lava ---
    @Inject(method = "travelInLava", at = @At("RETURN"))
    private void onTravelInLava(Vec3d movementInput, double gravity, boolean falling, double y, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow == null || !noSlow.isEnabled() || !noSlow.noLavaSlow.get()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        // Palautetaan nopeus (kerrotaan 2:lla, koska travelInLava kertoi 0.5:llä)
        self.setVelocity(self.getVelocity().multiply(2.0));
    }

}