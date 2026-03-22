package silversword.axiom.client.event;

import net.minecraft.network.protocol.Packet;

public class PacketEvent {
    public static class Receive {
        private static final Receive INSTANCE = new Receive();
        public Packet<?> packet;

        public static Receive get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }
}