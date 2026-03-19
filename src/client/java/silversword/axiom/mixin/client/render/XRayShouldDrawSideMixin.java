package silversword.axiom.mixin.client.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.XRay;

@Mixin(Block.class)
public abstract class XRayShouldDrawSideMixin {

    @Inject(
            method = "shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
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

        // Piirretään sivu jos tämä block on "näytettävä" (ore) ja naapuri on XRay-hidden.
        // Tämä tekee naapurista "ilmaa cullauksen silmissä" -> vein näkyy kokonaan.
        if (XRay.shouldRender(state) && XRay.isXrayHidden(neighbor)) {
            cir.setReturnValue(true);
        }
    }
}
