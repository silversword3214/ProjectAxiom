package silversword.axiom.mixin.client.player;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.player.GhostHand;

@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void onInteractBlock(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        GhostHand ghostHand = GhostHand.INSTANCE;
        if (ghostHand == null || !ghostHand.isEnabled()) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);


        MenuProvider factory = state.getMenuProvider(world, pos);
        if (factory != null) {
            player.openMenu(factory);
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        } else {

        }

        InteractionResult result = state.useItemOn(stack, world, player, hand, hitResult);
        if (result.consumesAction()) {
            cir.setReturnValue(result);
            return;
        }

        if (result instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
            result = state.useWithoutItem(world, player, hitResult);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
                return;
            }
        }


        if (!stack.isEmpty() && !player.getCooldowns().isOnCooldown(stack)) {
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            result = stack.useOn(context);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
                return;
            }
        }

    }
}