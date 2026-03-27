package silversword.axiom.mixin.client.boat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.mixininterface.BoatPhaseMixinAccessor;
import silversword.axiom.client.modules.movement.BoatPhase;

@Mixin(Entity.class)
public abstract class BoatPhaseDismountMixin {

    @Inject(method = "removePassenger", at = @At("HEAD"))
    private void axiom$onRemovePassenger(Entity passenger, CallbackInfo ci) {
        if (!(passenger instanceof Player)) return;
        if (!((Object)this instanceof AbstractBoat boat)) return;

        Minecraft mc = Minecraft.getInstance();
        if (passenger != mc.player) return;

        BoatPhase mod = ModuleManager.getInstance().getModule(BoatPhase.class);
        if (mod == null || !mod.isEnabled()) return;

        // Make the boat visible again
        boat.setInvisible(false);

        // Force the player to be considered on ground locally
        mc.player.setOnGround(true);

        // Send position packet twice to ensure server accepts it
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            // First packet (position only)
            ServerboundMovePlayerPacket.Pos posPacket = new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    true, false
            );
            connection.send(posPacket);

            // Second packet (position + rotation) to confirm
            ServerboundMovePlayerPacket.PosRot rotPacket = new ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYRot(), mc.player.getXRot(),
                    true, false
            );
            connection.send(rotPacket);
        }

        if (boat instanceof BoatPhaseMixinAccessor) {
            ((BoatPhaseMixinAccessor) boat).resetBoatPhaseTarget();
        }

        // Schedule module disable after one tick
        mod.scheduleDisableAfterOneTick();
    }
}