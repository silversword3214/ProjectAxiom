package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoHurtCam;
import silversword.axiom.client.modules.render.NoOverlay;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Close rendering.
     */
    @Inject(method = "close", at = @At("RETURN"))
    private void axiom$onGameRendererClose(CallbackInfo ci) {
        RenderAPI.getInstance().close();
    }

    /**
     * View bobbing.
     *
     * 26.2:
     * bobView(CameraRenderState, PoseStack)
     */
    @Inject(
            method = "bobView",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$cancelViewBobbing(
            CameraRenderState cameraState,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        NoViewBobbingTilt module =
                ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);

        if (module != null && module.isEnabled()) {
            ci.cancel();
        }
    }

    /**
     * Hurt camera.
     *
     * 26.2:
     * bobHurt(CameraRenderState, PoseStack)
     */
    @Inject(
            method = "bobHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$cancelHurtTilt(
            CameraRenderState cameraState,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        NoViewBobbingTilt noViewBobbing =
                ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);

        if (noViewBobbing != null && noViewBobbing.isEnabled()) {
            ci.cancel();
            return;
        }

        NoHurtCam noHurtCam =
                ModuleManager.getInstance().getModule(NoHurtCam.class);

        if (noHurtCam != null && noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }

    /**
     * Totem overlay.
     */
    @Inject(
            method = "displayItemActivation",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$noOverlayTotem(
            ItemStack stack,
            CallbackInfo ci
    ) {
        NoOverlay module =
                ModuleManager.getInstance().getModule(NoOverlay.class);

        if (module == null || !module.isEnabled() || !module.noTotem.get()) {
            return;
        }

        if (stack.is(Items.TOTEM_OF_UNDYING)) {
            ci.cancel();
        }
    }
}

