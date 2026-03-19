package silversword.axiom.mixin.client.overlays;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoPumpkinOverlay;

@Mixin(LivingEntity.class)
public abstract class NoPumpkinOverlayMixin {

    private static boolean enabled() {
        NoPumpkinOverlay mod = ModuleManager.getInstance().getModule(NoPumpkinOverlay.class);
        return mod != null && mod.isEnabled();
    }

    private static boolean isSelfPlayer(Object self) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && self == mc.player;
    }

    @Inject(method = "getEquippedStack", at = @At("RETURN"), cancellable = true)
    private void axiom$noPumpkinOverlay(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!enabled()) return;
        if (!isSelfPlayer(this)) return;
        if (slot != EquipmentSlot.HEAD) return;

        ItemStack stack = cir.getReturnValue();
        if (stack != null && stack.isOf(Items.CARVED_PUMPKIN)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
