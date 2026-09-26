package silversword.axiom.client.rendersystem.axiomrenderer.engine.renderer;


import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.AxiomVertexFormats;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.Batch;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.RenderCore;

public final class WorldRenderer {

    private final RenderCore core;

    public WorldRenderer(RenderCore core) {
        this.core = core;
    }

    public void line(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     float thickness, int color) {
        CompiledRenderPipeline pipeline = RenderCore.worldColoredLines();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.DEBUG_LINES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
    }

    public void quad(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     double x3, double y3, double z3,
                     double x4, double y4, double z4,
                     int color) {
        CompiledRenderPipeline pipeline = RenderCore.worldColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
        batch.vertex((float) x4, (float) y4, (float) z4, r, g, b, a);
    }

    public void triangle(double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double x3, double y3, double z3,
                         int color) {
        CompiledRenderPipeline pipeline = RenderCore.worldColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
    }
}