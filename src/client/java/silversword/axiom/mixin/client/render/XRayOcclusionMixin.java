package silversword.axiom.mixin.client.render;

import net.minecraft.block.AbstractBlock;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class XRayOcclusionMixin {

    @Inject(method = "isOpaqueFullCube()Z", at = @At("HEAD"), cancellable = true)
    private void axiom$xray_notOpaqueFullCube(CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;

        // Tee XRayn näkyvistä blokeista “ei-opaque-full-cube”
        // -> vähentää outoa occlusion/culling -käyttäytymistä.
        if (XRay.isXrayHidden((net.minecraft.block.BlockState)(Object)this)) {
            cir.setReturnValue(false);
        }

    }

    @Inject(method = "isSolidBlock(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private void axiom$xray_notSolidBlock(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isXRayEnabled()) return;

        if (XRay.isXrayHidden((net.minecraft.block.BlockState)(Object)this)) {
            cir.setReturnValue(false);
        }

    }

    @Inject(method = "getAmbientOcclusionLightLevel(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F",
            at = @At("HEAD"), cancellable = true)
    private void axiom$xray_fullBrightAO(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!XRay.isXRayEnabled()) return;

        if (XRay.shouldRender((net.minecraft.block.BlockState)(Object)this)) {
            cir.setReturnValue(1.0f);
        }
    }


}
