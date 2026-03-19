package silversword.axiom.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.CameraClip;
import silversword.axiom.client.modules.render.CameraDistance;
import silversword.axiom.client.modules.render.Freecam;

@Mixin(Camera.class)
public abstract class CameraMixin {



    // Override position
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
    private void onSetPos(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            args.set(0, freecam.getX(tickDelta));
            args.set(1, freecam.getY(tickDelta));
            args.set(2, freecam.getZ(tickDelta));
        }
    }

    // Override rotation
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void onSetRotation(Args args, @Local(argsOnly = true) float tickDelta) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            args.set(0, (float) freecam.getYaw(tickDelta));
            args.set(1, (float) freecam.getPitch(tickDelta));
        }
    }

    // Camera distance
    @ModifyArg(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"
            ),
            index = 0
    )
    private float modifyCameraDistance(float originalDistance) {
        CameraDistance module = ModuleManager.getInstance().getModule(CameraDistance.class);
        if (module != null && module.isEnabled()) {
            // Palautetaan moduulin asettama etäisyys (floatiksi muunnettuna)
            return (float) module.getDistance();
        }
        return originalDistance;
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"
            )
    )
    private float handleClipToSpace(Camera camera, float desiredDistance) {
        CameraDistance distanceModule = ModuleManager.getInstance().getModule(CameraDistance.class);
        CameraClip clipModule = ModuleManager.getInstance().getModule(CameraClip.class);

        // 1. Jos CameraDistance on päällä, käytetään sen asettamaa etäisyyttä
        if (distanceModule != null && distanceModule.isEnabled()) {
            desiredDistance = (float) distanceModule.getDistance();
        }

        // 2. Jos CameraClip on päällä, ohitetaan seinätarkistus kokonaan
        if (clipModule != null && clipModule.isEnabled()) {
            return desiredDistance;
        }

        // 3. Muuten kutsutaan alkuperäistä clipToSpace-metodia (normaali seinätarkistus)
        return camera.clipToSpace(desiredDistance);
    }

}