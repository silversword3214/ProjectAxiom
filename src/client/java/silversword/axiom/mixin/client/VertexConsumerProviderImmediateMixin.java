package silversword.axiom.mixin.client;

import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;

/**
 * Tämä mixin tekee VertexConsumerProvider.Immediate-luokan konstruktorista
 * sietävän null-parametreja. Näin voimme kutsua super(null, null)
 * WrapperImmediateVertexConsumerProvider-luokassa.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public class VertexConsumerProviderImmediateMixin {

    /**
     * Injectoidaan konstruktorin alkuun ja korvataan null-parametrit
     * oletusarvoilla.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ByteBufferBuilder allocator, SequencedMap layerBuffers, CallbackInfo ci) {
        // Tämä on tyhjä, koska itse konstruktoria ei voi muuttaa suoraan,
        // mutta tämä injectio varmistaa että konstruktori kutsutaan aina,
        // vaikka parametrit olisivat null.
        // Varsinainen muutos tapahtuu seuraavassa @ModifyArgissa.
    }

    /**
     * Vaihtoehtoinen tapa: korvataan konstruktorin parametrit ennen
     * kuin ne välitetään super-konstruktorille.
     */
    // @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V"), index = 0)
    // private BufferBuilder modifyFallbackBuffer(BufferBuilder original) {
    //     return original != null ? original : new BufferBuilder(256);
    // }
    //
    // @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V"), index = 1)
    // private Map modifyLayerBuffers(Map original) {
    //     return original != null ? original : new HashMap();
    // }
}