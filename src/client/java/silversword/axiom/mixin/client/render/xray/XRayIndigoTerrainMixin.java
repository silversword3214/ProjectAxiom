package silversword.axiom.mixin.client.render.xray;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(AltModelBlockRendererImpl.class)
public class XRayIndigoTerrainMixin {

    @Shadow private BlockState blockState;

    @Inject(method = "transform", at = @At("HEAD"), require = 0)
    private void axiom$xray_transform(MutableQuadView quad,
                                      CallbackInfoReturnable<Boolean> cir) {

        if (blockState == null) return;
        if (!XRay.isXRayEnabled()) return;
        if (!XRay.isXrayHidden(blockState)) return;

        float alpha = XRay.getHiddenAlpha();
        if (alpha <= 0.0f || alpha >= 1.0f) return;

        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));

        for (int i = 0; i < 4; i++) {
            int existing = quad.color(i);
            quad.color(i, (a << 24) | (existing & 0x00FFFFFF));
        }

        if (quad.chunkLayer() != ChunkSectionLayer.TRANSLUCENT) {
            quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
        }
    }
}