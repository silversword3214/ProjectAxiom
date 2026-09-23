package silversword.axiom.mixin.client.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.LowerShield;

@Mixin(ItemInHandRenderer.class)
public abstract class LowerShieldMixin {

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void axiom$lowerShield(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        LowerShield mod = ModuleManager.getInstance().getModule(LowerShield.class);
        if (mod == null || !mod.isEnabled()) return;
        if (player == null) return;

        if (itemStack == null || !itemStack.is(Items.SHIELD)) return;

        boolean isBlocking = player.isUsingItem()
                && player.getUsedItemHand() == hand
                && player.getUseItem().is(Items.SHIELD);

        float offsetY = mod.getOffsetY(isBlocking);
        if (offsetY <= 0.0f) return;

        poseStack.translate(0.0, -offsetY, 0.0);
    }
}