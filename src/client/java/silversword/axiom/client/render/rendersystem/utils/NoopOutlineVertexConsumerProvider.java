package silversword.axiom.client.render.rendersystem.utils;

import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class NoopOutlineVertexConsumerProvider extends OutlineBufferSource {
    public static final NoopOutlineVertexConsumerProvider INSTANCE = new NoopOutlineVertexConsumerProvider();

    private NoopOutlineVertexConsumerProvider() {
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        return NoopVertexConsumer.INSTANCE;
    }



    @Override
    public void endOutlineBatch() {
        // Ei tehdä mitään
    }
}