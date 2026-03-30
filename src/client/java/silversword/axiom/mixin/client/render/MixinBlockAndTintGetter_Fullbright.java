package silversword.axiom.mixin.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.render.FullbrightState;

@Mixin(BlockAndTintGetter.class)
public interface MixinBlockAndTintGetter_Fullbright {

    @Inject(
            method = "getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    default void axiom_fullbright_getLightLevel(LightLayer type, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (FullbrightState.enabled) {
            cir.setReturnValue(FullbrightState.MINIMUM_LIGHT);
        }
    }

    @Inject(
            method = "getRawBrightness(Lnet/minecraft/core/BlockPos;I)I",
            at = @At("HEAD"),
            cancellable = true
    )
    default void axiom_fullbright_getBaseLightLevel(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
        if (FullbrightState.enabled) {
            cir.setReturnValue(FullbrightState.MINIMUM_LIGHT);
        }
    }
}