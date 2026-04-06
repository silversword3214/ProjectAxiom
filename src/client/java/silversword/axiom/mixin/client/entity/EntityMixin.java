package silversword.axiom.mixin.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.event.entity.PreEntityMoveEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.Phase;
import silversword.axiom.client.modules.render.NoOverlay;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isColliding", at = @At("HEAD"), cancellable = true)
    private void onIsColliding(CallbackInfoReturnable<Boolean> cir) {
        if (Phase.isModuleEnabled()) {
            cir.setReturnValue(false);
        }
    }

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

    // Pre entity move
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && self instanceof Player && self == AxiomInitialize.mc.player) {
            PreEntityMoveEvent event = new PreEntityMoveEvent(self, moverType, movement);
            AxiomInitialize.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ci.cancel();
            } else if (!event.getMovement().equals(movement)) {
                ci.cancel();
                self.move(moverType, event.getMovement());
            }
        }
    }
}