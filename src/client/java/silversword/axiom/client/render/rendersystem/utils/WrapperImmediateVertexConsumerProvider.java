package silversword.axiom.client.render.rendersystem.utils;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import java.util.function.Supplier;

public class WrapperImmediateVertexConsumerProvider extends VertexConsumerProvider.Immediate {
    private final Supplier<VertexConsumerProvider> supplier;

    public WrapperImmediateVertexConsumerProvider(Supplier<VertexConsumerProvider> supplier) {
        super(null, null); // Nyt tämä on sallittua mixinin ansiosta
        this.supplier = supplier;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return supplier.get().getBuffer(layer);
    }

    @Override
    public void draw() {
        // Ei tehdä mitään – piirto hoidetaan muualla
    }

    @Override
    public void draw(RenderLayer layer) {
        // Ei tehdä mitään
    }
}