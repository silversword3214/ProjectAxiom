package silversword.axiom.mixin.client.render;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XRayAbstractBlockStateMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void axiomt$xray_getRenderType(CallbackInfoReturnable<RenderShape> cir) {
        if (!XRay.isXRayEnabled()) return;

        BlockState self = (BlockState) (Object) this;

        if (!XRay.shouldRender(self)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
