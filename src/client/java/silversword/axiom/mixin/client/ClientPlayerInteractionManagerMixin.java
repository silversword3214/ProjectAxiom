package silversword.axiom.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.hud.util.ComboCounter;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.moduleutils.InteractItemEvent;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Luodaan eventti ja postataan se
        InteractItemEvent event = InteractItemEvent.get(hand);
        AxiomInitialize.EVENT_BUS.post(event);

        // Jos eventissä on asetettu palautusarvo, perutaan alkuperäinen toiminto
        if (event.toReturn != null) {
            cir.setReturnValue(event.toReturn);
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        UseBlockEvent event = new UseBlockEvent(hitResult);
        AxiomInitialize.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Get the SearchBlocks module and call onBlockBreak if enabled
        silversword.axiom.client.modules.render.SearchBlocks search =
                silversword.axiom.client.managers.ModuleManager.getInstance().getModule(silversword.axiom.client.modules.render.SearchBlocks.class);
        if (search != null && search.isEnabled()) {
            search.onBlockBreak(pos);
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player != null && player.isMainPlayer() && target instanceof LivingEntity) {
            ComboCounter.onHit();
        }
    }


}