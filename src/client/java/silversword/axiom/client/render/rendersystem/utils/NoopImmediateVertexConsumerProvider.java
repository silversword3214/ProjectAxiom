package silversword.axiom.client.render.rendersystem.utils;

import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

public class NoopImmediateVertexConsumerProvider extends MultiBufferSource.BufferSource {
    public static final NoopImmediateVertexConsumerProvider INSTANCE = new NoopImmediateVertexConsumerProvider();

    private NoopImmediateVertexConsumerProvider() {
        super(null, null); // Nyt toimii
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        return NoopVertexConsumer.INSTANCE;
    }

    @Override
    public void endBatch() {}

    @Override
    public void endBatch(RenderType layer) {}
}