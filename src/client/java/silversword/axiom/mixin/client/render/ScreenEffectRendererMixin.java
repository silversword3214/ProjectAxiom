package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoOverlay;

@Mixin(ScreenEffectRenderer.class) // 1. Vaihda kohdeluokka
public abstract class ScreenEffectRendererMixin {

    @Inject(
            method = "renderItemActivationAnimation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$noOverlayTotem(
            PlayerRenderState playerRenderState,
            PoseStack poseStack,
            float partialTicks,
            SubmitNodeCollector submitNodeCollector,
            CallbackInfo ci
    ) {
        NoOverlay module = ModuleManager.getInstance().getModule(NoOverlay.class);
        if (module == null || !module.isEnabled() || !module.noTotem.get()) {
            return;
        }

        // Check if the item activation is a totem
        PlayerRenderState.ItemActivationRenderState activation = playerRenderState.itemActivation;
        if (activation != null && activation.itemState != null) {
            // Need to check the item from itemState somehow
            // This might require checking activation.itemState.item() == Items.TOTEM_OF_UNDYING
            // or similar
        }
    }
}