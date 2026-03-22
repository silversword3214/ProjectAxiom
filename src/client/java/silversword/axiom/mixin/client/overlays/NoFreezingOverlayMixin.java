package silversword.axiom.mixin.client.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoFreezingOverlay;

@Mixin(Entity.class)
public abstract class NoFreezingOverlayMixin {

    private static boolean enabled() {
        NoFreezingOverlay mod = ModuleManager.getInstance().getModule(NoFreezingOverlay.class);
        return mod != null && mod.isEnabled();
    }

    private static boolean isSelfPlayer(Object self) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && self == mc.player;
    }

    @Inject(method = "getPercentFrozen()F", at = @At("HEAD"), cancellable = true, require = 0)
    private void axiom$noFreeze_scale(CallbackInfoReturnable<Float> cir) {
        if (enabled() && isSelfPlayer(this)) cir.setReturnValue(0.0f);
    }

    @Inject(method = "getTicksFrozen()I", at = @At("HEAD"), cancellable = true, require = 0)
    private void axiom$noFreeze_ticks(CallbackInfoReturnable<Integer> cir) {
        if (enabled() && isSelfPlayer(this)) cir.setReturnValue(0);
    }

}
