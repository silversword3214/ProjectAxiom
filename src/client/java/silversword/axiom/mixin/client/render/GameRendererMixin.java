package silversword.axiom.mixin.client.render;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.event.GetFovEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoHurtCam;
import silversword.axiom.client.modules.render.NoOverlay;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    // Close rendering
    @Inject(method = "close", at = @At("RETURN"))
    private void onGameRendererClose(CallbackInfo ci) {
        RenderAPI.getInstance().close();
    }

    // View bobbing
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void axiom$cancelViewBobbing(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        NoViewBobbingTilt m = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        if (m != null && m.isEnabled()) {
            ci.cancel();
        }
    }

    // Hurt cam
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void axiom$cancelHurtTilt(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        NoViewBobbingTilt m = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        if (m != null && m.isEnabled()) {
            ci.cancel();
        }

        NoHurtCam hurtMod = ModuleManager.getInstance().getModule(NoHurtCam.class);
        if (hurtMod != null && hurtMod.isEnabled()) {
            ci.cancel();
        }

    }

    // FOV
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(CallbackInfoReturnable<Float> cir) {
        float fov = cir.getReturnValue();
        GetFovEvent event = GetFovEvent.get(fov);
        AxiomInitialize.EVENT_BUS.post(event);
        cir.setReturnValue(event.fov);
    }

    // Totem overlay
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void axiom$noOverlay_totem(ItemStack stack, CallbackInfo ci) {
        NoOverlay mod = ModuleManager.getInstance().getModule(NoOverlay.class);
        if (mod == null || !mod.isEnabled() || !mod.noTotem.get()) return;

        if (stack != null && stack.is(Items.TOTEM_OF_UNDYING)) {
            ci.cancel();
        }
    }
}