package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.render.XRay;

@Mixin(LightmapTextureManager.class)
public class XRayLightmapMixin {

    @Shadow @Final
    private GpuTexture glTexture;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onUpdate(float tickDelta, CallbackInfo ci) {
        if (XRay.isXRayEnabled()) {
            // Täysi valkoinen valaistus (255,255,255,255)
            RenderSystem.getDevice().createCommandEncoder()
                    .clearColorTexture(glTexture, ColorHelper.getArgb(255, 255, 255, 255));
            ci.cancel();
        }
    }
}