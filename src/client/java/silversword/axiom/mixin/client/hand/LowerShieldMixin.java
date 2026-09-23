package silversword.axiom.mixin.client.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.LowerShield;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class LowerShieldMixin {

    /**
     * 26.3: Vanha ItemInHandRenderer on jaettu kahtia:
     *   - FirstPersonHandsAndItems        → kerää tilan
     *   - FirstPersonHandsAndItemsRenderer → piirtää
     *
     * Injektoidaan submitArmWithItem-metodiin BEFORE-segmentissä kohtaan jossa
     * varsinainen itemStack lähetetään piirtoon (ItemStackRenderState.submit).
     * Siinä kohdassa poseStack on jo valmiiksi transformoitu käsiasentoon,
     * joten translate(0, -offsetY, 0) laskee kilpeä suoraan.
     */
    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void axiom$lowerShield(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
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

        if (itemStack == null || !itemStack.is(Items.SHIELD)) return;

        AvatarRenderState avatar = playerState.avatarRenderState;
        if (avatar == null) return;

        // 26.3: pelaajatieto tulee PlayerRenderState → AvatarRenderState -kautta
        boolean isBlocking = avatar.isUsingItem && avatar.useItemHand == hand;

        float offsetY = mod.getOffsetY(isBlocking);
        if (offsetY <= 0.0f) return;

        poseStack.translate(0.0, -offsetY, 0.0);
    }
}