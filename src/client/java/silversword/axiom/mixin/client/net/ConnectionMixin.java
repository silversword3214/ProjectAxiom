package silversword.axiom.mixin.client.net;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.event.packets.PacketEvent;

@Mixin(Connection.class)
public abstract class ConnectionMixin {


    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        if (AxiomInitialize.EVENT_BUS.post(new PacketEvent.Send(packet, (Connection) (Object) this)).isCancelled()) {
            ci.cancel();
        }
    }
}