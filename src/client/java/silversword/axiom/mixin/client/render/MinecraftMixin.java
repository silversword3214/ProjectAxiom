package silversword.axiom.mixin.client.render;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera.RearCameraRenderer;


@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void axiom$renderRearCamera(boolean advanceGameTime, CallbackInfo ci) {
        RearCameraRenderer.renderRearView();
    }
}