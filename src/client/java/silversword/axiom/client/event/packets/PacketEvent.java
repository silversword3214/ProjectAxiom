package silversword.axiom.client.event.packets;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public abstract class PacketEvent {
    private final Packet<?> packet;
    private final Connection connection;
    private boolean cancelled = false;

    public PacketEvent(Packet<?> packet, Connection connection) {
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static class Send extends PacketEvent {
        public Send(Packet<?> packet, Connection connection) {
            super(packet, connection);
        }
    }

    public static class Received extends PacketEvent {
        public Received(Packet<?> packet, Connection connection) {
            super(packet, connection);
        }
    }
}