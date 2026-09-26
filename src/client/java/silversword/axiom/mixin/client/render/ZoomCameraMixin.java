package silversword.axiom.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import silversword.axiom.client.modules.render.ZoomState;

@Mixin(Camera.class)
public class ZoomCameraMixin {

    private long axiom$lastFrameNs = 0L;

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float axiom$applyZoom(float original,
                                  @Local(argsOnly = true) float partialTicks) {

        long now = System.nanoTime();
        double dt = (axiom$lastFrameNs == 0L)
                ? 0.016
                : (now - axiom$lastFrameNs) / 1_000_000_000.0;
        axiom$lastFrameNs = now;
        dt = Math.min(dt, 0.1);

        double target = ZoomState.targetMultiplier;

        if (ZoomState.smoothCamera) {
            double diff = target - ZoomState.currentMultiplier;
            if (Math.abs(diff) < 0.001) {
                ZoomState.currentMultiplier = target;
            } else {
                double factor = Math.min(1.0, ZoomState.smoothSpeed * dt);
                ZoomState.currentMultiplier += diff * factor;
            }
        } else {
            ZoomState.currentMultiplier = target;
        }

        double mult = ZoomState.currentMultiplier;
        if (mult <= 1.0001) return original;

        return (float) (original / mult);
    }
}