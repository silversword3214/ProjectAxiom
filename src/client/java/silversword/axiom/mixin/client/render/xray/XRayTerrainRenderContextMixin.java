package silversword.axiom.mixin.client.render.xray;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.render.XRay;

@Mixin(TerrainRenderContext.class)
public class XRayTerrainRenderContextMixin {

    @Inject(method = "bufferModel", at = @At("HEAD"))
    private void onBufferModel(BlockStateModel model, BlockState blockState, BlockPos blockPos, CallbackInfo ci) {
        boolean hidden = XRay.isXRayEnabled() && XRay.isXrayHidden(blockState);
        XRay.setCurrentBlockHidden(hidden);
    }

    @Inject(method = "bufferModel", at = @At("RETURN"))
    private void afterBufferModel(BlockStateModel model, BlockState blockState, BlockPos blockPos, CallbackInfo ci) {
        XRay.setCurrentBlockHidden(false);
    }
}