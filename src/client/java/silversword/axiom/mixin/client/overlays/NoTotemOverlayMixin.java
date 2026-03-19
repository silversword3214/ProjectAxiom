package silversword.axiom.mixin.client.overlays;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoTotemOverlay;

@Mixin(GameRenderer.class)
public abstract class NoTotemOverlayMixin {

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void axiom$noTotemOverlay(ItemStack stack, CallbackInfo ci) {
        NoTotemOverlay mod = ModuleManager.getInstance().getModule(NoTotemOverlay.class);
        if (mod == null || !mod.isEnabled()) return;

        if (stack != null && stack.isOf(Items.TOTEM_OF_UNDYING)) {
            ci.cancel();
        }
    }
}
