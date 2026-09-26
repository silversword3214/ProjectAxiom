package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.render.FullbrightState;

@Mixin(Lightmap.class)
public class FullbrightLightmapMixin {

    @Shadow @Final
    private GpuTexture texture;

    private static final Vector4f WHITE = new Vector4f(1f, 1f, 1f, 1f);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void axiom$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
        if (!FullbrightState.enabled) return;

        RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorTexture(this.texture, WHITE);
        ci.cancel();
    }
}