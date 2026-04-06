package silversword.axiom.mixin.client.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.player.AntiCactus;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        ModuleManager manager = ModuleManager.getInstance();
        if (manager == null) return;

        AntiCactus antiCactus = (AntiCactus) manager.getModule(AntiCactus.class);

        // Muutetaan kaktus solidiksi vain, jos moduuli on päällä JA moodi on "Solid"
        if (antiCactus != null && antiCactus.isEnabled() && antiCactus.mode.getMode().equals("Solid")) {
            cir.setReturnValue(Shapes.block());
        }
    }
}