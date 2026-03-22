package silversword.axiom.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoHurtCam;

@Mixin(GameRenderer.class)
public abstract class NoDamageShakeMixin {

    private static boolean enabled() {
        NoHurtCam mod = ModuleManager.getInstance().getModule(NoHurtCam.class);
        return mod != null && mod.isEnabled();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void axiom$noDamageShake(PoseStack matrices, float tickProgress, CallbackInfo ci) {
        if (enabled()) ci.cancel();
    }
}
