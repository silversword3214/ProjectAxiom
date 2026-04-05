package silversword.axiom.mixin.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.combat.KillAura;
import silversword.axiom.client.modules.combat.MaceDmg;
import silversword.axiom.client.modules.movement.NoSlow;
import silversword.axiom.client.modules.render.AntiBlind;
import silversword.axiom.client.modules.render.NoOverlay;

import static silversword.axiom.client.main.AxiomInitialize.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {



    // Pumpkin overlay
    private static boolean shouldRemove() {
        NoOverlay mod = ModuleManager.getInstance().getModule(NoOverlay.class);
        return mod != null && mod.isEnabled() && mod.noPumpkin.get();
    }

    private static boolean isSelf(Object self) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && self == mc.player;
    }

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true)
    private void axiom$noOverlay_pumpkin(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!shouldRemove()) return;
        if (!isSelf(this)) return;
        if (slot != EquipmentSlot.HEAD) return;

        ItemStack stack = cir.getReturnValue();
        if (stack != null && stack.is(Items.CARVED_PUMPKIN)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }



    // AntiBlind
    private boolean shouldRemoveEffect(Holder<MobEffect> effect) {
        if (!((Object) this instanceof LocalPlayer)) return false;
        AntiBlind mod = ModuleManager.getInstance().getModule(AntiBlind.class);
        if (mod == null || !mod.isEnabled() || !mod.noDarkness.get()) return false;
        return effect == MobEffects.BLINDNESS || effect == MobEffects.DARKNESS;
    }

    // Potion effects
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (shouldRemoveEffect(effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void onGetStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (shouldRemoveEffect(effect)) {
            cir.setReturnValue(null);
        }
    }

    // NoSlow block slow
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
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

    // NoSlow water slow
    @Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true)
    private void onGetBaseWaterMovementSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow == null || !noSlow.isEnabled()) return;

        float original = cir.getReturnValue(); // oletus 0.8
        if (original < 1.0f && noSlow.noWaterSlow.get()) {
            cir.setReturnValue(1.0f);
        }
    }

    // NoSlow slowness potion
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffectNoSlow(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect == MobEffects.SLOWNESS) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void onGetStatusEffectNoSlow(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect == MobEffects.SLOWNESS) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddStatusEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlownessPotion.get() && effect.getEffect() == MobEffects.SLOWNESS) {
            cir.setReturnValue(false); // Estetään efektin lisääminen
        }
    }

    // NoSlow lava
    @Inject(method = "travelInLava", at = @At("RETURN"))
    private void onTravelInLava(Vec3 movementInput, double gravity, boolean falling, double y, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow == null || !noSlow.isEnabled() || !noSlow.noLavaSlow.get()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        // Palautetaan nopeus (kerrotaan 2:lla, koska travelInLava kertoi 0.5:llä)
        self.setDeltaMovement(self.getDeltaMovement().scale(2.0));
    }

}