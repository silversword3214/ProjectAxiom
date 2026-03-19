package silversword.axiom.mixin.client.overlays;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoFireOverlay;

@Mixin(Entity.class)
public abstract class NoFireOverlayEntityMixin {

    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void axiom$noFireOverlay_isOnFire(CallbackInfoReturnable<Boolean> cir) {
        NoFireOverlay mod = ModuleManager.getInstance().getModule(NoFireOverlay.class);
        if (mod == null || !mod.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // VAIN oma pelaaja -> ei sotketa muiden entityjen fire-renderiä
        if ((Object) this == mc.player) {
            cir.setReturnValue(false);
        }
    }
}
