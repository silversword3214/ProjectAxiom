package silversword.axiom.mixin.client.overlays;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && self == mc.player;
    }

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true)
    private void axiom$noPumpkinOverlay(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!enabled()) return;
        if (!isSelfPlayer(this)) return;
        if (slot != EquipmentSlot.HEAD) return;

        ItemStack stack = cir.getReturnValue();
        if (stack != null && stack.is(Items.CARVED_PUMPKIN)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
