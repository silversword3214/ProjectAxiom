package silversword.axiom.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.CameraClip;
import silversword.axiom.client.modules.render.CameraDistance;
import silversword.axiom.client.modules.render.Freecam;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Unique
    private float axiom$requestedCameraDistance;

    // Freecam

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

    // Camera Distance
    @ModifyVariable(
            method = "getMaxZoom",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float axiom$cameraDistance(float original) {
        CameraDistance mod = ModuleManager.getInstance().getModule(CameraDistance.class);
        if (mod != null && mod.isEnabled()) {
            float d = (float) mod.getDistance();
            axiom$requestedCameraDistance = d;
            return d;
        }
        axiom$requestedCameraDistance = original;
        return original;
    }

    // Camera Clip
    @ModifyReturnValue(
            method = "getMaxZoom",
            at = @At("RETURN")
    )
    private float axiom$cameraClip(float original) {
        CameraClip mod = ModuleManager.getInstance().getModule(CameraClip.class);
        if (mod != null && mod.isEnabled()) {
            return axiom$requestedCameraDistance;
        }
        return original;
    }
}