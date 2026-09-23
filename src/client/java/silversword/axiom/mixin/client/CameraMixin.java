package silversword.axiom.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.CameraClip;
import silversword.axiom.client.modules.render.CameraDistance;
import silversword.axiom.client.modules.render.Freecam;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"
            )
    )
    private void axiom$modifyCameraPosition(
            Args args,
            @Local(argsOnly = true) float partialTicks
    ) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);

        if (freecam != null && freecam.isEnabled()) {
            args.set(0, freecam.getX(partialTicks));
            args.set(1, freecam.getY(partialTicks));
            args.set(2, freecam.getZ(partialTicks));
        }
    }

    @ModifyArgs(
            method = "alignWithEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FF)V"
            )
    )
    private void axiom$modifyCameraRotation(
            Args args,
            @Local(argsOnly = true) float partialTicks
    ) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);

        if (freecam != null && freecam.isEnabled()) {
            args.set(0, (float) freecam.getYaw(partialTicks));
            args.set(1, (float) freecam.getPitch(partialTicks));
        }
    }
}