package silversword.axiom.mixin.client.render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.FullbrightState;

@Mixin(BlockRenderView.class)
public interface MixinBlockRenderView_Fullbright {

    @Inject(
            method = "getLightLevel(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    default void axiom_fullbright_getLightLevel(LightType type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!FullbrightState.enabled) return;

        // null = molemmat, muuten rajaa
        if (FullbrightState.type != null && FullbrightState.type != type) return;

        cir.setReturnValue(FullbrightState.minimumLight); // 15 = “normi fullbright”
    }

    @Inject(
            method = "getBaseLightLevel(Lnet/minecraft/util/math/BlockPos;I)I",
            at = @At("HEAD"),
            cancellable = true
    )
    default void axiom_fullbright_getBaseLightLevel(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
        if (!FullbrightState.enabled) return;

        // Base light level vaikuttaa myös useaan laskentapolkuun
        cir.setReturnValue(FullbrightState.minimumLight);
    }
}
