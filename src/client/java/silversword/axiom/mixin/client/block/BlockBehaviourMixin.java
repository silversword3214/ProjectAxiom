package silversword.axiom.mixin.client.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;  // TÄMÄ ON TÄRKEÄ
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.movement.Phase;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                     CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Phase.isModuleEnabled()) return;
        if (!(context instanceof EntityCollisionContext ec)) return;

        Entity entity = ec.getEntity();
        if (!(entity instanceof Player player)) return;

        boolean sneaking = player.isShiftKeyDown();
        if (sneaking) {
            cir.setReturnValue(Shapes.empty());
            return;
        }

        double playerMinY = player.getBoundingBox().minY;
        double blockMaxY = pos.getY() + 1.0;
        if (blockMaxY <= playerMinY) return;

        cir.setReturnValue(Shapes.empty());
    }
}