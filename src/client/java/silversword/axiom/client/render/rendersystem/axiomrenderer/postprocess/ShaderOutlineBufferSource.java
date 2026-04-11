package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class ShaderOutlineBufferSource extends OutlineBufferSource {
    private final ShaderMaskBufferSource delegate;

    public ShaderOutlineBufferSource(ShaderMaskBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return delegate.getBuffer(renderType);
    }

    @Override
    public void setColor(int color) {
    }

    @Override
    public void endOutlineBatch() {
    }
}