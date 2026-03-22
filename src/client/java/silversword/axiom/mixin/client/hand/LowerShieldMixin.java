// TODO(Ravel): Failed to fully resolve file: class com.intellij.psi.impl.source.tree.java.PsiPolyadicExpressionImpl cannot be cast to class com.intellij.psi.PsiLiteralExpression (com.intellij.psi.impl.source.tree.java.PsiPolyadicExpressionImpl and com.intellij.psi.PsiLiteralExpression are in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @7d77e38c)
// TODO(Ravel): Failed to fully resolve file: class com.intellij.psi.impl.source.tree.java.PsiPolyadicExpressionImpl cannot be cast to class com.intellij.psi.PsiLiteralExpression (com.intellij.psi.impl.source.tree.java.PsiPolyadicExpressionImpl and com.intellij.psi.PsiLiteralExpression are in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @7d77e38c)
package silversword.axiom.mixin.client.hand;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.LowerShield;

@Mixin(ItemInHandRenderer.class)
public abstract class LowerShieldMixin {

    @Inject(
            method = "renderArmWithItem",
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
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci
    ) {
        LowerShield mod = ModuleManager.getInstance().getModule(LowerShield.class);
        if (mod == null || !mod.isEnabled()) return;
        if (player == null) return;

        // Vain kilpi
        if (stack == null || !stack.is(Items.SHIELD)) return;

        boolean isBlocking =
                player.isUsingItem() &&
                        player.getUsedItemHand() == hand &&
                        player.getUseItem().is(Items.SHIELD);

        float offsetY = mod.getOffsetY(isBlocking);
        if (offsetY <= 0.0f) return;

        // Y-akseli: miinus = alas
        matrices.translate(0.0, -offsetY, 0.0);
    }
}
