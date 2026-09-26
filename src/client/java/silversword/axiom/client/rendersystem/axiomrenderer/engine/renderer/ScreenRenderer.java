package silversword.axiom.client.rendersystem.axiomrenderer.engine.renderer;


import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.AxiomVertexFormats;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.Batch;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.RenderCore;

import java.util.ArrayList;
import java.util.List;

public final class ScreenRenderer {

    private static final float AA_WIDTH = 0.5f;
    private static final float AA_HALF  = AA_WIDTH * 0.5f;

    private final RenderCore core;

    public ScreenRenderer(RenderCore core) {
        this.core = core;
    }

    public void rect(float x, float y, float width, float height, int color) {
        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        emitQuad(batch, x, y, width, height, c[0], c[1], c[2], c[3]);
    }

    public void rectOutline(float x, float y, float width, float height,
                            float thickness, int color) {
        if (thickness <= 0) return;

        CompiledRenderPipeline pipeline = RenderCore.uiColoredLines();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.DEBUG_LINES);
        float[] c = unpack(color);
        float r = c[0], g = c[1], b = c[2], a = c[3];
        float x2 = x + width;
        float y2 = y + height;

        batch.vertex2D(x,  y,  r, g, b, a);
        batch.vertex2D(x2, y,  r, g, b, a);
        batch.vertex2D(x,  y2, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
        batch.vertex2D(x,  y,  r, g, b, a);
        batch.vertex2D(x,  y2, r, g, b, a);
        batch.vertex2D(x2, y,  r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
    }


    public void line(float x1, float y1, float x2, float y2,
                     float thickness, int color) {
        if (thickness <= 0) return;

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        float r = c[0], g = c[1], b = c[2], a = c[3];

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;

        float nx = dy / len;
        float ny = -dx / len;

        float halfT     = thickness * 0.5f;
        float solidHalf = Math.max(0f, halfT - AA_HALF);
        float outerHalf = halfT + AA_HALF;

        float p1aL = x1 - nx * solidHalf, p1aLy = y1 - ny * solidHalf;
        float p1aR = x1 + nx * solidHalf, p1aRy = y1 + ny * solidHalf;
        float p2aL = x2 - nx * solidHalf, p2aLy = y2 - ny * solidHalf;
        float p2aR = x2 + nx * solidHalf, p2aRy = y2 + ny * solidHalf;

        batch.vertex2D(p1aL, p1aLy, r, g, b, a);
        batch.vertex2D(p1aR, p1aRy, r, g, b, a);
        batch.vertex2D(p2aR, p2aRy, r, g, b, a);
        batch.vertex2D(p1aL, p1aLy, r, g, b, a);
        batch.vertex2D(p2aR, p2aRy, r, g, b, a);
        batch.vertex2D(p2aL, p2aLy, r, g, b, a);

        float p1c = x1 + nx * outerHalf, p1cy = y1 + ny * outerHalf;
        float p2c = x2 + nx * outerHalf, p2cy = y2 + ny * outerHalf;
        batch.vertex2D(p1aR, p1aRy, r, g, b, a);
        batch.vertex2D(p1c,  p1cy,  r, g, b, 0f);
        batch.vertex2D(p2c,  p2cy,  r, g, b, 0f);
        batch.vertex2D(p1aR, p1aRy, r, g, b, a);
        batch.vertex2D(p2c,  p2cy,  r, g, b, 0f);
        batch.vertex2D(p2aR, p2aRy, r, g, b, a);

        float p1d = x1 - nx * outerHalf, p1dy = y1 - ny * outerHalf;
        float p2d = x2 - nx * outerHalf, p2dy = y2 - ny * outerHalf;
        batch.vertex2D(p1d,  p1dy,  r, g, b, 0f);
        batch.vertex2D(p1aL, p1aLy, r, g, b, a);
        batch.vertex2D(p2aL, p2aLy, r, g, b, a);
        batch.vertex2D(p1d,  p1dy,  r, g, b, 0f);
        batch.vertex2D(p2aL, p2aLy, r, g, b, a);
        batch.vertex2D(p2d,  p2dy,  r, g, b, 0f);
    }

    public void circle(float cx, float cy, float radius, int color) {
        if (radius <= 0) return;

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        int segs = segmentsForRadius(radius);
        List<float[]> boundary = buildCircleBoundary(cx, cy, radius, 1f, segs);
        emitFilled(batch, boundary, cx, cy, c[0], c[1], c[2], c[3]);
    }

    public void circleOutline(float cx, float cy, float radius,
                              float thickness, int color) {
        if (radius <= 0 || thickness <= 0) return;

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        float innerR = Math.max(0, radius - thickness);
        int segs = segmentsForRadius(radius);

        List<float[]> outer = buildCircleBoundary(cx, cy, radius, 1f, segs);
        List<float[]> inner = buildCircleBoundary(cx, cy, innerR, -1f, segs);
        emitRing(batch, outer, inner, c[0], c[1], c[2], c[3]);
    }

    public void roundedRect(float x, float y, float w, float h,
                            float radius, int color) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0.5f) { rect(x, y, w, h, color); return; }

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        radius = clamp(radius, 0f, Math.min(w, h) * 0.5f);
        int arcSegs = arcSegmentsFor(radius);

        List<float[]> boundary = buildRoundedRectBoundary(
                x, y, w, h, radius, radius, radius, radius, 1f, arcSegs);
        emitFilled(batch, boundary, x + w * 0.5f, y + h * 0.5f, c[0], c[1], c[2], c[3]);
    }

    public void roundedRectCustom(float x, float y, float w, float h,
                                  float radius, int color,
                                  boolean topLeft, boolean topRight,
                                  boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0) return;
        if ((!topLeft && !topRight && !bottomRight && !bottomLeft)
                || radius <= 0.5f) {
            rect(x, y, w, h, color);
            return;
        }

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);

        float rTL = topLeft     ? radius : 0f;
        float rTR = topRight    ? radius : 0f;
        float rBR = bottomRight ? radius : 0f;
        float rBL = bottomLeft  ? radius : 0f;

        int arcSegs = arcSegmentsFor(radius);
        List<float[]> boundary = buildRoundedRectBoundary(
                x, y, w, h, rTL, rTR, rBR, rBL, 1f, arcSegs);
        emitFilled(batch, boundary, x + w * 0.5f, y + h * 0.5f, c[0], c[1], c[2], c[3]);
    }

    public void roundedRectOutline(float x, float y, float w, float h,
                                   float radius, float thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        thickness = Math.min(thickness, Math.min(w, h) * 0.5f);

        CompiledRenderPipeline pipeline = RenderCore.uiColored();
        if (pipeline == null) return;

        Batch batch = core.batchFor(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float[] c = unpack(color);
        radius = clamp(radius, 0f, Math.min(w, h) * 0.5f);
        float innerR = Math.max(0, radius - thickness);
        int arcSegs = arcSegmentsFor(Math.max(radius, innerR));

        List<float[]> outer = buildRoundedRectBoundary(
                x, y, w, h, radius, radius, radius, radius, 1f, arcSegs);
        List<float[]> inner = buildRoundedRectBoundary(
                x + thickness, y + thickness,
                w - 2f * thickness, h - 2f * thickness,
                innerR, innerR, innerR, innerR, -1f, arcSegs);

        emitRing(batch, outer, inner, c[0], c[1], c[2], c[3]);
    }

    private static float[] unpack(int color) {
        return new float[] {
                ((color >> 16) & 0xFF) / 255f,
                ((color >>  8) & 0xFF) / 255f,
                ( color        & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f,
        };
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static int segmentsForRadius(float radius) {
        if (radius <= 0.5f) return 8;
        int segs = (int) Math.ceil(Math.PI * radius);
        return Math.max(16, Math.min(segs, 256));
    }

    private static int arcSegmentsFor(float radius) {
        if (radius <= 0.5f) return 2;
        return Math.max(4, Math.min(32, (int) Math.ceil(radius)));
    }

    private static void addArcPoints(List<float[]> pts, float cx, float cy,
                                     float radius, float startAng, float endAng,
                                     float sign, int segs) {
        if (radius <= 0.001f) {
            float mid = (startAng + endAng) * 0.5f;
            float nx = (float) Math.cos(mid), ny = (float) Math.sin(mid);
            pts.add(new float[]{cx, cy, nx * sign, ny * sign});
            return;
        }
        for (int i = 1; i <= segs; i++) {
            float ang = startAng + (endAng - startAng) * i / segs;
            float nx = (float) Math.cos(ang), ny = (float) Math.sin(ang);
            pts.add(new float[]{
                    cx + nx * radius, cy + ny * radius, nx * sign, ny * sign});
        }
    }

    private static List<float[]> buildRoundedRectBoundary(
            float x, float y, float w, float h,
            float rTL, float rTR, float rBR, float rBL,
            float sign, int arcSegs) {

        List<float[]> pts = new ArrayList<>();
        float x2 = x + w, y2 = y + h;
        float maxR = Math.min(w, h) * 0.5f;
        rTL = clamp(rTL, 0, maxR);
        rTR = clamp(rTR, 0, maxR);
        rBR = clamp(rBR, 0, maxR);
        rBL = clamp(rBL, 0, maxR);

        pts.add(new float[]{x + rTL, y, 0f, -sign});
        pts.add(new float[]{x2 - rTR, y, 0f, -sign});
        addArcPoints(pts, x2 - rTR, y + rTR, rTR,
                (float) (-Math.PI / 2), 0f, sign, arcSegs);
        pts.add(new float[]{x2, y + rTR, sign, 0f});
        pts.add(new float[]{x2, y2 - rBR, sign, 0f});
        addArcPoints(pts, x2 - rBR, y2 - rBR, rBR,
                0f, (float) (Math.PI / 2), sign, arcSegs);
        pts.add(new float[]{x2 - rBR, y2, 0f, sign});
        pts.add(new float[]{x + rBL, y2, 0f, sign});
        addArcPoints(pts, x + rBL, y2 - rBL, rBL,
                (float) (Math.PI / 2), (float) Math.PI, sign, arcSegs);
        pts.add(new float[]{x, y2 - rBL, -sign, 0f});
        pts.add(new float[]{x, y + rTL, -sign, 0f});
        addArcPoints(pts, x + rTL, y + rTL, rTL,
                (float) Math.PI, (float) (3 * Math.PI / 2), sign, arcSegs);

        return pts;
    }

    private static List<float[]> buildCircleBoundary(float cx, float cy,
                                                     float radius, float sign,
                                                     int segs) {
        List<float[]> pts = new ArrayList<>(segs);
        for (int i = 0; i < segs; i++) {
            float ang = (float) (2 * Math.PI * i / segs);
            float nx = (float) Math.cos(ang), ny = (float) Math.sin(ang);
            pts.add(new float[]{
                    cx + nx * radius, cy + ny * radius, nx * sign, ny * sign});
        }
        return pts;
    }

    private static void emitFilled(Batch batch, List<float[]> boundary,
                                   float cx, float cy,
                                   float r, float g, float b, float a) {
        int n = boundary.size();
        if (n < 3) return;

        for (int i = 0; i < n; i++) {
            float[] p1 = boundary.get(i), p2 = boundary.get((i + 1) % n);
            float in1x = p1[0] - p1[2] * AA_HALF, in1y = p1[1] - p1[3] * AA_HALF;
            float in2x = p2[0] - p2[2] * AA_HALF, in2y = p2[1] - p2[3] * AA_HALF;
            batch.vertex2D(cx, cy, r, g, b, a);
            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(in2x, in2y, r, g, b, a);
        }

        for (int i = 0; i < n; i++) {
            float[] p1 = boundary.get(i), p2 = boundary.get((i + 1) % n);
            float in1x = p1[0] - p1[2] * AA_HALF, in1y = p1[1] - p1[3] * AA_HALF;
            float in2x = p2[0] - p2[2] * AA_HALF, in2y = p2[1] - p2[3] * AA_HALF;
            float out1x = p1[0] + p1[2] * AA_HALF, out1y = p1[1] + p1[3] * AA_HALF;
            float out2x = p2[0] + p2[2] * AA_HALF, out2y = p2[1] + p2[3] * AA_HALF;

            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(in2x, in2y, r, g, b, a);

            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(out2x, out2y, r, g, b, 0f);
        }
    }

    private static void emitRing(Batch batch,
                                 List<float[]> outer, List<float[]> inner,
                                 float r, float g, float b, float a) {
        int n = outer.size();
        if (n < 3 || inner.size() != n) return;

        for (int i = 0; i < n; i++) {
            float[] o1 = outer.get(i), o2 = outer.get((i + 1) % n);
            float[] i1 = inner.get(i), i2 = inner.get((i + 1) % n);

            float o1x = o1[0] - o1[2] * AA_HALF, o1y = o1[1] - o1[3] * AA_HALF;
            float o2x = o2[0] - o2[2] * AA_HALF, o2y = o2[1] - o2[3] * AA_HALF;
            float i1x = i1[0] - i1[2] * AA_HALF, i1y = i1[1] - i1[3] * AA_HALF;
            float i2x = i2[0] - i2[2] * AA_HALF, i2y = i2[1] - i2[3] * AA_HALF;

            batch.vertex2D(o1x, o1y, r, g, b, a);
            batch.vertex2D(o2x, o2y, r, g, b, a);
            batch.vertex2D(i2x, i2y, r, g, b, a);
            batch.vertex2D(o1x, o1y, r, g, b, a);
            batch.vertex2D(i2x, i2y, r, g, b, a);
            batch.vertex2D(i1x, i1y, r, g, b, a);
        }

        for (int i = 0; i < n; i++) {
            float[] o1 = outer.get(i), o2 = outer.get((i + 1) % n);
            float in1x = o1[0] - o1[2] * AA_HALF, in1y = o1[1] - o1[3] * AA_HALF;
            float in2x = o2[0] - o2[2] * AA_HALF, in2y = o2[1] - o2[3] * AA_HALF;
            float out1x = o1[0] + o1[2] * AA_HALF, out1y = o1[1] + o1[3] * AA_HALF;
            float out2x = o2[0] + o2[2] * AA_HALF, out2y = o2[1] + o2[3] * AA_HALF;

            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(out2x, out2y, r, g, b, 0f);
        }

        for (int i = 0; i < n; i++) {
            float[] i1 = inner.get(i), i2 = inner.get((i + 1) % n);
            float in1x = i1[0] - i1[2] * AA_HALF, in1y = i1[1] - i1[3] * AA_HALF;
            float in2x = i2[0] - i2[2] * AA_HALF, in2y = i2[1] - i2[3] * AA_HALF;
            float out1x = i1[0] + i1[2] * AA_HALF, out1y = i1[1] + i1[3] * AA_HALF;
            float out2x = i2[0] + i2[2] * AA_HALF, out2y = i2[1] + i2[3] * AA_HALF;

            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(out2x, out2y, r, g, b, 0f);
        }
    }

    private static void emitQuad(Batch batch, float x, float y, float w, float h,
                                 float r, float g, float b, float a) {
        float x2 = x + w;
        float y2 = y + h;
        batch.vertex2D(x,  y,  r, g, b, a);
        batch.vertex2D(x2, y,  r, g, b, a);
        batch.vertex2D(x,  y2, r, g, b, a);
        batch.vertex2D(x,  y2, r, g, b, a);
        batch.vertex2D(x2, y,  r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
    }
}