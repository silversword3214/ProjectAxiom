package silversword.axiom.mixin.client.net;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.modules.combat.KillAura;
import silversword.axiom.client.modules.misc.PingSpoof;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.SilentRotationController;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @ModifyVariable(method = "send", at = @At("HEAD"), argsOnly = true)
    private Packet<?> modifyPacket(Packet<?> packet) {
        KillAura killAura = KillAura.getInstance();
        if (killAura != null && killAura.isSilentModeActive()) {
            SilentRotationController rotation = killAura.getSilentRotationController();
            if (rotation != null && rotation.hasTarget() && rotation.isActive()) {
                if (packet instanceof ServerboundMovePlayerPacket.Rot rotPacket) {
                    // Korvataan pelkkä rotaatiopaketti
                    return new ServerboundMovePlayerPacket.Rot(
                            rotation.getCurrentYaw(),
                            rotation.getCurrentPitch(),
                            rotPacket.isOnGround(),
                            rotPacket.horizontalCollision()
                    );
                } else if (packet instanceof ServerboundMovePlayerPacket.PosRot posRotPacket) {
                    // Korvataan positio+rotaatiopaketti
                    return new ServerboundMovePlayerPacket.PosRot(
                            posRotPacket.getX(0), posRotPacket.getY(0), posRotPacket.getZ(0),
                            rotation.getCurrentYaw(),
                            rotation.getCurrentPitch(),
                            posRotPacket.isOnGround(),
                            posRotPacket.horizontalCollision()
                    );
                }
            }
        }
        return packet;
    }



    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        PacketEvent.Send event = new PacketEvent.Send(packet, (Connection) (Object) this);
        AxiomInitialize.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}