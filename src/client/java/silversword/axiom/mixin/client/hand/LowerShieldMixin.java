package silversword.axiom.mixin.client.hand;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.LowerShield;

@Mixin(HeldItemRenderer.class)
public abstract class LowerShieldMixin {

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem" +
                            "(Lnet/minecraft/entity/LivingEntity;" +
                            "Lnet/minecraft/item/ItemStack;" +
                            "Lnet/minecraft/item/ItemDisplayContext;" +
                            "Lnet/minecraft/client/util/math/MatrixStack;" +
                            "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void axiom$lowerShield(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci
    ) {
        LowerShield mod = ModuleManager.getInstance().getModule(LowerShield.class);
        if (mod == null || !mod.isEnabled()) return;
        if (player == null) return;

        // Vain kilpi
        if (stack == null || !stack.isOf(Items.SHIELD)) return;

        boolean isBlocking =
                player.isUsingItem() &&
                        player.getActiveHand() == hand &&
                        player.getActiveItem().isOf(Items.SHIELD);

        float offsetY = mod.getOffsetY(isBlocking);
        if (offsetY <= 0.0f) return;

        // Y-akseli: miinus = alas
        matrices.translate(0.0, -offsetY, 0.0);
    }
}
