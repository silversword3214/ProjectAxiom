package silversword.axiom.mixin.client.movement;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.Jesus;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockCollisionMixin {

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void axiom$jesusCollision(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context,
                                      CallbackInfoReturnable<VoxelShape> cir) {

        // Vain entity-kontekstissa (muuten turha)
        if (!(context instanceof EntityCollisionContext esc)) return;

        Entity e = esc.getEntity();
        if (e == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // Vain local player (ettei tee outoa muille client-puolella)
        if (e != mc.player) return;

        Jesus jesus = ModuleManager.getInstance().getModule(Jesus.class);
        if (jesus == null || !jesus.isEnabled()) return;

        // Sneak = uppoa
        if (jesus.sneakToSink.get() && mc.player.isShiftKeyDown()) return;

        FluidState fs = state.getFluidState();
        boolean isWater = fs.is(Fluids.WATER) || fs.is(Fluids.FLOWING_WATER);
        boolean isLava  = fs.is(Fluids.LAVA)  || fs.is(Fluids.FLOWING_LAVA);

        if (!(isWater || (jesus.includeLava.get() && isLava))) return;

        // TÄRKEIN: tee solidiksi vain jos pelaaja on pinnan YLÄPUOLELLA.
        // Tämä estää "jumi veden sisään" -ilmiön.
        double minY = e.getBoundingBox().minY;
        double surfaceY = pos.getY() + 0.99; // aivan pinnan yläpuolella
        if (minY < surfaceY) return;

        cir.setReturnValue(Shapes.block());
    }
}
