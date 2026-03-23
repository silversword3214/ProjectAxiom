package silversword.axiom.mixin.client.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.NoSlow;

@Mixin(SlimeBlock.class)
public class SlimeBlockMixin {

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void onSteppedOn(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlimeBlockSlow.get()) {
            ci.cancel(); // Estetään hidastus
        }
    }

    @Inject(method = "updateEntityMovementAfterFallOn", at = @At("HEAD"), cancellable = true)
    private void onEntityLand(BlockGetter world, Entity entity, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlimeBlockSlow.get()) {
            ci.cancel(); // Estetään pomppiminen
        }
    }

    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void onLandedUpon(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noSlimeBlockSlow.get()) {
            ci.cancel();
        }
    }
}