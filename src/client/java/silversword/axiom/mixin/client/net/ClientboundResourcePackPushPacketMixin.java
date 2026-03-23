package silversword.axiom.mixin.client.net;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.config.ResourcePackBlockerConfig;

@Mixin(ClientboundResourcePackPushPacket.class)
public class ClientboundResourcePackPushPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void onApply(ClientCommonPacketListener listener, CallbackInfo ci) {
        if (!ResourcePackBlockerConfig.isEnabled()) return;

        // Cancel the packet – it will never reach the handler
        ci.cancel();

        // Send fake status responses to satisfy the server
        ClientboundResourcePackPushPacket self = (ClientboundResourcePackPushPacket) (Object) this;

        // The listener is always ClientPlayNetworkHandler on the client
        if (listener instanceof ClientPacketListener handler) {
            handler.send(new ServerboundResourcePackPacket(self.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
            handler.send(new ServerboundResourcePackPacket(self.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        }
    }
}