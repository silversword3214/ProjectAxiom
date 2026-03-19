package silversword.axiom.client.event.packets;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;

public abstract class PacketEvent {
    private final Packet<?> packet;
    private final ClientConnection connection;
    private boolean cancelled = false;

    public PacketEvent(Packet<?> packet, ClientConnection connection) {
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static class Send extends PacketEvent {
        public Send(Packet<?> packet, ClientConnection connection) {
            super(packet, connection);
        }
    }

    public static class Received extends PacketEvent {
        public Received(Packet<?> packet, ClientConnection connection) {
            super(packet, connection);
        }
    }
}