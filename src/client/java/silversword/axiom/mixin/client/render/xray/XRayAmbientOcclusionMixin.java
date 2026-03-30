package silversword.axiom.mixin.client.render.xray;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class XRayAmbientOcclusionMixin {

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (XRay.isXRayEnabled()) {
            cir.setReturnValue(1.0f);
        }
    }
}