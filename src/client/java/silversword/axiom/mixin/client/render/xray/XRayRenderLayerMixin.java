package silversword.axiom.mixin.client.render.xray;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(ItemBlockRenderTypes.class)
public class XRayRenderLayerMixin {

    @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
    private static void axiom$forceTranslucentLayer(BlockState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
        if (XRay.isXRayEnabled() && XRay.isXrayHidden(state)) {
            cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
        }
    }
}