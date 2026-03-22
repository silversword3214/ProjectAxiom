package silversword.axiom.mixin.client.render;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XRaySideCullingMixin {

    @Inject(
            method = "skipRendering",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void axiom$xray_noSideCulling(BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;
        cir.setReturnValue(false);
    }
}
