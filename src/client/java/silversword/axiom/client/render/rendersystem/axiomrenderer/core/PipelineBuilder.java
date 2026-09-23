package silversword.axiom.client.render.rendersystem.axiomrenderer.core;


import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.mixininterface.ILineSmoothing;

public final class PipelineBuilder {
    private final RenderPipeline.Builder innerBuilder;
    private CompareOp depthTest = CompareOp.ALWAYS_PASS;
    private boolean depthWrite;
    private BlendFunction blend = BlendFunction.TRANSLUCENT;
    private boolean lineSmooth;

    public PipelineBuilder(RenderPipeline.Snippet... snippets) {
        innerBuilder = RenderPipeline.builder(snippets);
    }

    public PipelineBuilder withLocation(Identifier location) {
        innerBuilder.withLocation(location);
        return this;
    }

    public PipelineBuilder withVertexFormat(VertexFormat format, PrimitiveTopology topology) {
        innerBuilder.withVertexBinding(0, format).withPrimitiveTopology(topology);
        return this;
    }

    public PipelineBuilder withVertexShader(Identifier shader) {
        innerBuilder.withVertexShader(shader);
        return this;
    }

    public PipelineBuilder withFragmentShader(Identifier shader) {
        innerBuilder.withFragmentShader(shader);
        return this;
    }

    public PipelineBuilder withDepthTestFunction(CompareOp function) {
        depthTest = function;
        return this;
    }

    public PipelineBuilder withDepthWrite(boolean write) {
        depthWrite = write;
        return this;
    }

    public PipelineBuilder withBlend(BlendFunction function) {
        blend = function;
        return this;
    }

    public PipelineBuilder withCull(boolean cull) {
        innerBuilder.withCull(cull);
        return this;
    }

    public PipelineBuilder withLineSmooth() {
        lineSmooth = true;
        return this;
    }

    public RenderPipeline build() {
        innerBuilder.withColorTargetState(new ColorTargetState(blend));
        innerBuilder.withDepthStencilState(new DepthStencilState(depthTest, depthWrite));
        RenderPipeline pipeline = innerBuilder.build();
        ((ILineSmoothing) pipeline).axiom_setLineSmooth(lineSmooth);
        return pipeline;
    }
}