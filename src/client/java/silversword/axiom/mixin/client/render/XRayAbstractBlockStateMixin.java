package silversword.axiom.mixin.client.render;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class XRayAbstractBlockStateMixin {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void axiomt$xray_getRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        if (!XRay.isXRayEnabled()) return;

        BlockState self = (BlockState) (Object) this;

        if (!XRay.shouldRender(self)) {
            cir.setReturnValue(BlockRenderType.INVISIBLE);
        }
    }
}
