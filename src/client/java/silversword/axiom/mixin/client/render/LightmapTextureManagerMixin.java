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
import silversword.axiom.client.modules.render.FullbrightState;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Shadow @Final private GpuTexture glTexture;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onUpdateHead(float tickProgress, CallbackInfo ci) {
        if (FullbrightState.enabled) {
            // Tyhjennetään lightmap-tekstuuri valkoiseksi
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorTexture(glTexture, ColorHelper.getArgb(255, 255, 255, 255));
            ci.cancel(); // Estetään normaali päivitys
        }
    }
}