package silversword.axiom.mixin.client.render.xray;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class XRayProbeMixin {

    @Inject(method = "tesselateBlock", at = @At("HEAD"))
    private void probe1(CallbackInfo ci) {
        System.out.println("[XRay] PROBE: tesselateBlock");
    }

    @Inject(method = "putQuadWithTint", at = @At("HEAD"))
    private void probe2(CallbackInfo ci) {
        System.out.println("[XRay] PROBE: putQuadWithTint");
    }

    @Inject(method = "tesselateAmbientOcclusion", at = @At("HEAD"))
    private void probe3(CallbackInfo ci) {
        System.out.println("[XRay] PROBE: tesselateAmbientOcclusion");
    }

    @Inject(method = "tesselateFlat", at = @At("HEAD"))
    private void probe4(CallbackInfo ci) {
        System.out.println("[XRay] PROBE: tesselateFlat");
    }
}