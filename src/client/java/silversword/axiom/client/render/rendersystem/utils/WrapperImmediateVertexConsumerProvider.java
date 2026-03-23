package silversword.axiom.client.render.rendersystem.utils;

import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import java.util.function.Supplier;

public class WrapperImmediateVertexConsumerProvider extends MultiBufferSource.BufferSource {
    private final Supplier<MultiBufferSource> supplier;

    public WrapperImmediateVertexConsumerProvider(Supplier<MultiBufferSource> supplier) {
        super(null, null); // Nyt tämä on sallittua mixinin ansiosta
        this.supplier = supplier;
    }

    @Override
    public VertexConsumer getBuffer(RenderType layer) {
        return supplier.get().getBuffer(layer);
    }

    @Override
    public void endBatch() {
        // Ei tehdä mitään – piirto hoidetaan muualla
    }

    @Override
    public void endBatch(RenderType layer) {
        // Ei tehdä mitään
    }
}