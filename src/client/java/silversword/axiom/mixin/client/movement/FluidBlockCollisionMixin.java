package silversword.axiom.mixin.client.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.EntityShapeContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.Jesus;

@Mixin(FluidBlock.class)
public abstract class FluidBlockCollisionMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void axiom$jesusCollision(BlockState state, BlockView world, BlockPos pos, ShapeContext context,
                                         CallbackInfoReturnable<VoxelShape> cir) {

        // Vain entity-kontekstissa (muuten turha)
        if (!(context instanceof EntityShapeContext esc)) return;

        Entity e = esc.getEntity();
        if (e == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

        // Vain local player (ettei tee outoa muille client-puolella)
        if (e != mc.player) return;

        Jesus jesus = ModuleManager.getInstance().getModule(Jesus.class);
        if (jesus == null || !jesus.isEnabled()) return;

        // Sneak = uppoa
        if (jesus.sneakToSink.get() && mc.player.isSneaking()) return;

        FluidState fs = state.getFluidState();
        boolean isWater = fs.isOf(Fluids.WATER) || fs.isOf(Fluids.FLOWING_WATER);
        boolean isLava  = fs.isOf(Fluids.LAVA)  || fs.isOf(Fluids.FLOWING_LAVA);

        if (!(isWater || (jesus.includeLava.get() && isLava))) return;

        // TÄRKEIN: tee solidiksi vain jos pelaaja on pinnan YLÄPUOLELLA.
        // Tämä estää "jumi veden sisään" -ilmiön.
        double minY = e.getBoundingBox().minY;
        double surfaceY = pos.getY() + 0.99; // aivan pinnan yläpuolella
        if (minY < surfaceY) return;

        cir.setReturnValue(VoxelShapes.fullCube());
    }
}
