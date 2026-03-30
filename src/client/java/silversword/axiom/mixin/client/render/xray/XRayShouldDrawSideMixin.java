package silversword.axiom.mixin.client.render.xray;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(Block.class)
public abstract class XRayShouldDrawSideMixin {

    @Inject(
            method = "shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void axiom$xray_forceDrawSide(
            BlockState state,
            BlockState neighbor,
            Direction side,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!XRay.isXRayEnabled()) return;

        if (XRay.shouldRender(state) && XRay.isXrayHidden(neighbor)) {
            cir.setReturnValue(true);
        }
    }
}
