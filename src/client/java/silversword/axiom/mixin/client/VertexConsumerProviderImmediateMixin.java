package silversword.axiom.mixin.client;

import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SequencedMap;


@Mixin(MultiBufferSource.BufferSource.class)
public class VertexConsumerProviderImmediateMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ByteBufferBuilder allocator, SequencedMap layerBuffers, CallbackInfo ci) {
    }
}