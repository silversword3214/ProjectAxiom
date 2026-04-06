package silversword.axiom.mixin.client.net;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.packets.PacketReceiveEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.movement.Phase;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    private boolean handlingMotion = false; // ← rekursiokatkaisin

    @Inject(
            method = "handleSetEntityMotion",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onMotionPacket(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (handlingMotion) return; // ← jos jo käsitellään, ohita

        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        AxiomInitialize.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.getPacket() != packet) {
            handlingMotion = true;
            try {
                ((ClientPacketListener) (Object) this)
                        .handleSetEntityMotion((ClientboundSetEntityMotionPacket) event.getPacket());
            } finally {
                handlingMotion = false;
            }
            ci.cancel();
        }
    }
}