package silversword.axiom.mixin.client.net;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.config.ResourcePackBlockerConfig;

@Mixin(ResourcePackSendS2CPacket.class)
public class ResourcePackSendS2CPacketMixin {

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void onApply(ClientCommonPacketListener listener, CallbackInfo ci) {
        if (!ResourcePackBlockerConfig.isEnabled()) return;

        // Cancel the packet – it will never reach the handler
        ci.cancel();

        // Send fake status responses to satisfy the server
        ResourcePackSendS2CPacket self = (ResourcePackSendS2CPacket) (Object) this;

        // The listener is always ClientPlayNetworkHandler on the client
        if (listener instanceof ClientPlayNetworkHandler handler) {
            handler.sendPacket(new ResourcePackStatusC2SPacket(self.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            handler.sendPacket(new ResourcePackStatusC2SPacket(self.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
        }
    }
}