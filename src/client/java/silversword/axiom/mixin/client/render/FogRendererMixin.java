package silversword.axiom.mixin.client.render;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoFog;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private void onApplyFog(Camera camera,
                            int viewDistance,
                            RenderTickCounter tickCounter,
                            float tickDelta,
                            ClientWorld world,
                            CallbackInfoReturnable<Vector4f> cir) {

        NoFog noFog = ModuleManager.getInstance().getModule(NoFog.class);
        if (noFog == null || !noFog.isEnabled()) return;

        CameraSubmersionType type = camera.getSubmersionType();

        boolean cancel = false;


        // Atmospheric (esim. nether/end tyyppinen)
        if (noFog.disableAtmosphericFog.get() && type == CameraSubmersionType.ATMOSPHERIC) {
            cancel = true;
        }

        // Water
        if (noFog.disableWaterFog.get() && type == CameraSubmersionType.WATER) {
            cancel = true;
        }

        // Lava
        if (noFog.disableLavaFog.get() && type == CameraSubmersionType.LAVA) {
            cancel = true;
        }

        // Powder Snow
        if (noFog.disablePowderSnowFog.get() && type == CameraSubmersionType.POWDER_SNOW) {
            cancel = true;
        }

        if (cancel) {
            // Palautetaan tyhjä fog-arvo -> estää sumun
            cir.setReturnValue(new Vector4f(0f, 0f, 0f, 0f));
            cir.cancel();
        }
    }
}