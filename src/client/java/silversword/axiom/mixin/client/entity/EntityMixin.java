package silversword.axiom.mixin.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoOverlay;

@Mixin(Entity.class)
public abstract class EntityMixin {

    // Freeze overlay
    private static boolean shouldRemove() {
        NoOverlay mod = ModuleManager.getInstance().getModule(NoOverlay.class);
        return mod != null && mod.isEnabled() && mod.noFreezing.get();
    }

    private static boolean isSelf(Object self) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && self == mc.player;
    }

    @Inject(method = "getPercentFrozen()F", at = @At("HEAD"), cancellable = true)
    private void axiom$noOverlay_freezingPercent(CallbackInfoReturnable<Float> cir) {
        if (shouldRemove() && isSelf(this)) cir.setReturnValue(0.0f);
    }

    @Inject(method = "getTicksFrozen()I", at = @At("HEAD"), cancellable = true)
    private void axiom$noOverlay_freezingTicks(CallbackInfoReturnable<Integer> cir) {
        if (shouldRemove() && isSelf(this)) cir.setReturnValue(0);
    }

    // Fire overlay
    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void axiom$noOverlay_fire(CallbackInfoReturnable<Boolean> cir) {
        NoOverlay mod = ModuleManager.getInstance().getModule(NoOverlay.class);
        if (mod == null || !mod.isEnabled() || !mod.noFire.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (Object) this == mc.player) {
            cir.setReturnValue(false);
        }
    }
}