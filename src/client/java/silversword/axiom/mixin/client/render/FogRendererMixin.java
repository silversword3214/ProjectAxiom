package silversword.axiom.mixin.client.render;

import net.minecraft.world.level.material.FogType;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoFog;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    // TODO(Ravel): target method applyFog is ambiguous
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private void onApplyFog(Camera camera,
                            int viewDistance,
                            DeltaTracker tickCounter,
                            float tickDelta,
                            ClientLevel world,
                            CallbackInfoReturnable<Vector4f> cir) {

        NoFog noFog = ModuleManager.getInstance().getModule(NoFog.class);
        if (noFog == null || !noFog.isEnabled()) return;

        FogType type = camera.getFluidInCamera();

        boolean cancel = false;


        // Atmospheric (esim. nether/end tyyppinen)
        if (noFog.disableAtmosphericFog.get() && type == FogType.ATMOSPHERIC) {
            cancel = true;
        }

        // Water
        if (noFog.disableWaterFog.get() && type == FogType.WATER) {
            cancel = true;
        }

        // Lava
        if (noFog.disableLavaFog.get() && type == FogType.LAVA) {
            cancel = true;
        }

        // Powder Snow
        if (noFog.disablePowderSnowFog.get() && type == FogType.POWDER_SNOW) {
            cancel = true;
        }

        if (cancel) {
            // Palautetaan tyhjä fog-arvo -> estää sumun
            cir.setReturnValue(new Vector4f(0f, 0f, 0f, 0f));
            cir.cancel();
        }
    }
}