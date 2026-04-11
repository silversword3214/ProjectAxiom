package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.LinkedHashMap;

public final class ShaderMaskBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {

    private static final VertexConsumer DUMMY = new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int i) {return null;}
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float f) {return null;}
    };

    public ShaderMaskBufferSource() {
        super(new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE), new LinkedHashMap<>());
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        String name = renderType.toString();

        if (name.contains("glint") || name.contains("entity_glint")) {
            return DUMMY;
        }

        return super.getBuffer(ShaderRenderTypes.maskEntity());
    }

    @Override
    public void close() {
        sharedBuffer.close();
    }
}