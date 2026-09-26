package silversword.axiom.mixin.client.render;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class HideHandMixin {

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void axiom$hideHandWhenScreenOpen(
            CameraRenderState cameraState,
            PlayerRenderState playerState,
            GpuTextureView depthTextureView,
            CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null && mc.gui.screen() != null) {
            ci.cancel();
        }
    }
}