package silversword.axiom.mixin.client.player;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.player.GhostHand;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    public void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        GhostHand ghostHand = GhostHand.INSTANCE;
        if (ghostHand == null || !ghostHand.isEnabled()) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);


        // 1. Suoraan screenHandlerFactory (chestit, uunit, työpöydät...)
        NamedScreenHandlerFactory factory = state.createScreenHandlerFactory(world, pos);
        if (factory != null) {
            player.openHandledScreen(factory);
            cir.setReturnValue(ActionResult.CONSUME);
            return;
        } else {
            System.out.println("[GhostHand] No Factor");
        }

        // 2. Kokeillaan onUseWithItem (blokin oma metodi esineen kanssa)
        ActionResult result = state.onUseWithItem(stack, world, player, hand, hitResult);
        if (result.isAccepted()) {
            cir.setReturnValue(result);
            return;
        }

        // 3. Jos se palauttaa PASS_TO_DEFAULT_BLOCK_ACTION, kokeillaan onUse (ilman esinettä)
        if (result instanceof ActionResult.PassToDefaultBlockAction && hand == Hand.MAIN_HAND) {
            result = state.onUse(world, player, hitResult);
            if (result.isAccepted()) {
                cir.setReturnValue(result);
                return;
            }
        }

        // 4. Yritetään käyttää itse esinettä blokkiin (stack.useOnBlock)
        if (!stack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(stack)) {
            ItemUsageContext context = new ItemUsageContext(player, hand, hitResult);
            result = stack.useOnBlock(context);
            if (result.isAccepted()) {
                cir.setReturnValue(result);
                return;
            }
        }




        System.out.println("[GhostHand] All attempts failed");
    }


}