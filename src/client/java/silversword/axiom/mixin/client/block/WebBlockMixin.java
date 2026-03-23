package silversword.axiom.mixin.client.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.NoSlow;

@Mixin(WebBlock.class)
public class WebBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(BlockState state, Level world, BlockPos pos, Entity entity,
                                   InsideBlockEffectApplier handler, boolean bl, CallbackInfo ci) {
        NoSlow noSlow = ModuleManager.getInstance().getModule(NoSlow.class);
        if (noSlow != null && noSlow.isEnabled() && noSlow.noCobwebSlow.get()) {
            ci.cancel();
        }
    }
}