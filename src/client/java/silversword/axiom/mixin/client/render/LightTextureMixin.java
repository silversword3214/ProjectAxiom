package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.render.FullbrightState;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow @Final private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void onUpdateHead(float tickProgress, CallbackInfo ci) {
        if (FullbrightState.enabled) {
            // Tyhjennetään lightmap-tekstuuri valkoiseksi
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorTexture(texture, ARGB.color(255, 255, 255, 255));
            ci.cancel(); // Estetään normaali päivitys
        }
    }
}