package silversword.axiom.mixin.client.render;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class XRaySideCullingMixin {

    @Inject(
            method = "isSideInvisible",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void axiom$xray_noSideCulling(BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;
        cir.setReturnValue(false);
    }
}
