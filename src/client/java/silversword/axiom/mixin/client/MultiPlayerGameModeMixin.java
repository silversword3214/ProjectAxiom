package silversword.axiom.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.event.player.UseBlockEvent;
import silversword.axiom.client.hud.util.ComboCounter;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.moduleutils.InteractItemEvent;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onInteractItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Luodaan eventti ja postataan se
        InteractItemEvent event = InteractItemEvent.get(hand);
        AxiomInitialize.EVENT_BUS.post(event);

        // Jos eventissä on asetettu palautusarvo, perutaan alkuperäinen toiminto
        if (event.toReturn != null) {
            cir.setReturnValue(event.toReturn);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        UseBlockEvent event = new UseBlockEvent(hitResult);
        AxiomInitialize.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Get the SearchBlocks module and call onBlockBreak if enabled
        silversword.axiom.client.modules.render.SearchBlocks search =
                silversword.axiom.client.managers.ModuleManager.getInstance().getModule(silversword.axiom.client.modules.render.SearchBlocks.class);
        if (search != null && search.isEnabled()) {
            search.onBlockBreak(pos);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (player != null && player.isLocalPlayer() && target instanceof LivingEntity) {
            ComboCounter.onHit();
        }
    }


}