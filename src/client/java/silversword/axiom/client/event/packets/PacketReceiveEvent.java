package silversword.axiom.client.event.packets;

import net.minecraft.network.protocol.Packet;
import silversword.axiom.client.eventbus.ICancellable;

public class PacketReceiveEvent implements ICancellable {

    private Packet<?> packet;
    private boolean cancelled = false; //

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() { return packet; }
    public void setPacket(Packet<?> packet) { this.packet = packet; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public boolean isCancelled() { return cancelled; }
}