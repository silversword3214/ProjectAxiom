package silversword.axiom.client.render.rendersystem.utils.postprocess;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.world.entity.Entity;
import silversword.axiom.client.mixininterface.IWorldRenderer;

import silversword.axiom.client.render.rendersystem.utils.render.CustomOutlineVertexConsumerProvider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public abstract class EntityShader extends PostProcessShader {
    public final CustomOutlineVertexConsumerProvider vertexConsumerProvider;

    protected EntityShader(RenderPipeline pipeline) {
        super(pipeline);
        this.vertexConsumerProvider = new CustomOutlineVertexConsumerProvider();
    }

    public abstract boolean shouldDraw(Entity entity);

    @Override
    protected void preDraw() {
        ((IWorldRenderer) mc.levelRenderer).axiom$pushEntityOutlineFramebuffer(getFramebuffer());
    }

    @Override
    protected void postDraw() {
        ((IWorldRenderer) mc.levelRenderer).axiom$popEntityOutlineFramebuffer();
    }

    public void submitVertices() {
        submitVertices(vertexConsumerProvider::draw);
    }

    public boolean shouldDrawShader() {
        return shouldDraw();
    }
}