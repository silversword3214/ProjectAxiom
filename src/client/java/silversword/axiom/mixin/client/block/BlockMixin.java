package silversword.axiom.mixin.client.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.movement.IceSpeed;
import silversword.axiom.client.modules.movement.Slippy;

@Mixin(Block.class)
public class BlockMixin {


    @Redirect(method = "getFriction", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Block;friction:F"))
    private float redirectFriction(Block block) {
        if (Slippy.isModuleEnabled()) {
            return Slippy.getCurrentSlipperiness();
        }
        return block.friction;
    }

    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true)
    private void onGetFriction(CallbackInfoReturnable<Float> cir) {
        if (!IceSpeed.isModuleEnabled()) return;

        Block block = (Block)(Object)this;

        if (block == Blocks.ICE || block == Blocks.PACKED_ICE ||
                block == Blocks.BLUE_ICE || block == Blocks.FROSTED_ICE) {
            cir.setReturnValue(IceSpeed.getCustomFriction());
        }
    }
}