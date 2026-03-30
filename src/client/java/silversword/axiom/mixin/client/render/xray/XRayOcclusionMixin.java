package silversword.axiom.mixin.client.render.xray;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XRayOcclusionMixin {

    @Inject(method = "isSolidRender()Z", at = @At("HEAD"), cancellable = true)
    private void axiom$xray_notOpaqueFullCube(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;
        if (XRay.isXrayHidden((net.minecraft.world.level.block.state.BlockState)(Object)this)) {
            cir.setReturnValue(false);
        }

    }

    @Inject(method = "isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private void axiom$xray_notSolidBlock(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;

        if (XRay.isXrayHidden((net.minecraft.world.level.block.state.BlockState)(Object)this)) {
            cir.setReturnValue(false);
        }

    }

    @Inject(method = "getShadeBrightness(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
            at = @At("HEAD"), cancellable = true)
    private void axiom$xray_fullBrightAO(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!XRay.isXRayEnabled()) return;

        if (XRay.shouldRender((net.minecraft.world.level.block.state.BlockState)(Object)this)) {
            cir.setReturnValue(1.0f);
        }
    }


}
