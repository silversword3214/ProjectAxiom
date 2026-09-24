package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderPipelines;

public class Renderer2D {
    private final GuiGraphicsExtractor g;
    public final RenderCore core;
    private final Matrix4f projection;

    private static GuiGraphicsExtractor lastFrameGraphics = null;

    public Renderer2D(GuiGraphicsExtractor graphics, RenderCore core, Matrix4f projection) {
        this.g = graphics;
        this.core = core;
        this.projection = projection;

        if (graphics != lastFrameGraphics) {
            this.core.beginFrame(this.projection, new Matrix4f().identity());
            lastFrameGraphics = graphics;
        }
    }

    /** Kutsuttavissa, jos joku haluaa käyttää RenderCorea suoraan. */
    public void flush() {
        core.flush();
    }

    // ================================================================
    //  Väriapurit
    // ================================================================

    private static int A(int color) { return (color >>> 24) & 0xFF; }
    private static int RGB(int color) { return color & 0x00FFFFFF; }
    private static int ARGB(int rgb, int a) {
        if (a <= 0) return 0;
        if (a > 255) a = 255;
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    // ================================================================
    //  Peittävyys (coverage) – 2×2 supernäytteistys
    // ================================================================

    private static boolean insideCircle(double px, double py, double cx, double cy, double r) {
        double dx = px - cx, dy = py - cy;
        return dx * dx + dy * dy <= r * r;
    }

    private static boolean insideRoundedRect(
            double px, double py,
            double x, double y, double w, double h,
            double rTL, double rTR, double rBR, double rBL) {
        double x2 = x + w, y2 = y + h;
        if (px < x || px > x2 || py < y || py > y2) return false;
        if (rTL > 0 && px < x + rTL && py < y + rTL) {
            double dx = px - (x + rTL), dy = py - (y + rTL);
            return dx * dx + dy * dy <= rTL * rTL;
        }
        if (rTR > 0 && px > x2 - rTR && py < y + rTR) {
            double dx = px - (x2 - rTR), dy = py - (y + rTR);
            return dx * dx + dy * dy <= rTR * rTR;
        }
        if (rBR > 0 && px > x2 - rBR && py > y2 - rBR) {
            double dx = px - (x2 - rBR), dy = py - (y2 - rBR);
            return dx * dx + dy * dy <= rBR * rBR;
        }
        if (rBL > 0 && px < x + rBL && py > y2 - rBL) {
            double dx = px - (x + rBL), dy = py - (y2 - rBL);
            return dx * dx + dy * dy <= rBL * rBL;
        }
        return true;
    }

    private static float covCircle(int pxi, int pyi, double cx, double cy, double r) {
        int c = 0;
        for (int sy = 0; sy < 2; sy++)
            for (int sx = 0; sx < 2; sx++)
                if (insideCircle(pxi + 0.25 + sx * 0.5, pyi + 0.25 + sy * 0.5, cx, cy, r)) c++;
        return c * 0.25f;
    }

    private static float covRoundedRect(
            int pxi, int pyi,
            double x, double y, double w, double h,
            double rTL, double rTR, double rBR, double rBL) {
        int c = 0;
        for (int sy = 0; sy < 2; sy++)
            for (int sx = 0; sx < 2; sx++)
                if (insideRoundedRect(pxi + 0.25 + sx * 0.5, pyi + 0.25 + sy * 0.5,
                        x, y, w, h, rTL, rTR, rBR, rBL)) c++;
        return c * 0.25f;
    }

    private static float distToSegment(double px, double py,
                                       double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-9) return (float) Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0) t = 0;
        else if (t > 1) t = 1;
        double projX = x1 + t * dx, projY = y1 + t * dy;
        return (float) Math.hypot(px - projX, py - projY);
    }

    // ================================================================
    //  Suorakaide
    // ================================================================

    public void drawRect(float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0 || A(color) == 0) return;
        g.fill(Math.round(x), Math.round(y),
                Math.round(x + width), Math.round(y + height), color);
    }

    public void drawRectOutline(float x, float y, float width, float height, float thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) return;
        float edge = Math.min(thickness, Math.min(width, height) / 2.0f);
        drawRect(x, y, width, edge, color);
        drawRect(x, y + height - edge, width, edge, color);
        drawRect(x, y + edge, edge, height - 2f * edge, color);
        drawRect(x + width - edge, y + edge, edge, height - 2f * edge, color);
    }

    // ================================================================
    //  Viiva (AA, analyyttinen peittävyys)
    // ================================================================

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0 || A(color) == 0) return;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;

        float halfT = thickness * 0.5f;
        int rgbVal = RGB(color);
        int baseA = A(color);

        int minY = (int) Math.floor(Math.min(y1, y2) - halfT - 1);
        int maxY = (int) Math.ceil (Math.max(y1, y2) + halfT + 1);
        int minX = (int) Math.floor(Math.min(x1, x2) - halfT - 1);
        int maxX = (int) Math.ceil (Math.max(x1, x2) + halfT + 1);

        for (int py = minY; py <= maxY; py++) {
            int runStart = -1;
            int runEnd = -1;
            for (int px = minX; px <= maxX; px++) {
                float d = distToSegment(px + 0.5, py + 0.5, x1, y1, x2, y2);
                float cov = halfT + 0.5f - d;      // 1 px pehmeä reuna
                if (cov <= 0f) {
                    if (runStart >= 0) {
                        g.fill(runStart, py, runEnd + 1, py + 1, color);
                        runStart = runEnd = -1;
                    }
                    continue;
                }
                if (cov >= 1f) {
                    if (runStart < 0) runStart = px;
                    runEnd = px;
                } else {
                    if (runStart >= 0) {
                        g.fill(runStart, py, runEnd + 1, py + 1, color);
                        runStart = runEnd = -1;
                    }
                    g.fill(px, py, px + 1, py + 1, ARGB(rgbVal, Math.round(baseA * cov)));
                }
            }
            if (runStart >= 0) g.fill(runStart, py, runEnd + 1, py + 1, color);
        }
    }

    // ================================================================
    //  Ympyrä (AA)
    // ================================================================

    public void drawCircle(double cx, double cy, double radius, int color) {
        if (radius <= 0 || A(color) == 0) return;
        int rgbVal = RGB(color);
        int baseA = A(color);

        int topY = (int) Math.floor(cy - radius) - 1;
        int botY = (int) Math.ceil (cy + radius) + 1;

        for (int py = topY; py <= botY; py++) {
            double dy = py + 0.5 - cy;
            double inside = radius * radius - dy * dy;
            if (inside < 0) continue;
            double halfW = Math.sqrt(inside);
            int xStart = (int) Math.floor(cx - halfW) - 1;
            int xEnd   = (int) Math.ceil (cx + halfW) + 1;

            int pxL = xStart;
            while (pxL <= xEnd) {
                float cov = covCircle(pxL, py, cx, cy, radius);
                if (cov >= 1f) break;
                if (cov > 0f) g.fill(pxL, py, pxL + 1, py + 1, ARGB(rgbVal, Math.round(baseA * cov)));
                pxL++;
            }
            int pxR = xEnd;
            while (pxR > pxL) {
                float cov = covCircle(pxR, py, cx, cy, radius);
                if (cov >= 1f) break;
                if (cov > 0f) g.fill(pxR, py, pxR + 1, py + 1, ARGB(rgbVal, Math.round(baseA * cov)));
                pxR--;
            }
            if (pxR >= pxL) g.fill(pxL, py, pxR + 1, py + 1, color);
        }
    }

    public void drawCircleOutline(double cx, double cy, double radius, int color, double thickness) {
        if (radius <= 0 || thickness <= 0 || A(color) == 0) return;
        int rgbVal = RGB(color);
        int baseA = A(color);

        double innerR = Math.max(0, radius - thickness);
        double innerR2 = innerR * innerR;
        double outerR2 = radius * radius;

        int topY = (int) Math.floor(cy - radius) - 1;
        int botY = (int) Math.ceil (cy + radius) + 1;

        for (int py = topY; py <= botY; py++) {
            double dy = py + 0.5 - cy;
            double dySq = dy * dy;
            if (dySq >= outerR2) continue;
            double outerHalfW = Math.sqrt(outerR2 - dySq);
            double innerHalfW = dySq < innerR2 ? Math.sqrt(innerR2 - dySq) : 0;

            fillBandAA(py, cx - outerHalfW, cx - innerHalfW, cx, cy, radius, innerR,
                    rgbVal, baseA);
            fillBandAA(py, cx + innerHalfW, cx + outerHalfW, cx, cy, radius, innerR,
                    rgbVal, baseA);
        }
    }

    private void fillBandAA(int py, double xL, double xR,
                            double cx, double cy, double rOut, double rIn,
                            int rgbVal, int baseA) {
        if (xR <= xL) return;
        int xStart = (int) Math.floor(xL) - 1;
        int xEnd   = (int) Math.ceil (xR) + 1;
        for (int px = xStart; px <= xEnd; px++) {
            float cov = 0f;
            for (int sy = 0; sy < 2; sy++) {
                for (int sx = 0; sx < 2; sx++) {
                    double sxp = px + 0.25 + sx * 0.5;
                    double syp = py + 0.25 + sy * 0.5;
                    double d2 = (sxp - cx) * (sxp - cx) + (syp - cy) * (syp - cy);
                    if (d2 <= rOut * rOut && d2 >= rIn * rIn) cov += 0.25f;
                }
            }
            if (cov <= 0f) continue;
            int a = Math.round(baseA * cov);
            g.fill(px, py, px + 1, py + 1, ARGB(rgbVal, a));
        }
    }


    public void drawRoundedRect(double x, double y, double w, double h, double radius, int color) {
        drawRoundedRectCustom(x, y, w, h, radius, color, true, true, true, true);
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h, double radius, int color,
                                      boolean topLeft, boolean topRight,
                                      boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0 || A(color) == 0) return;
        int rgbVal = RGB(color);
        int baseA = A(color);

        double maxR = Math.min(w, h) * 0.5;
        double rTL = topLeft     ? Math.min(radius, maxR) : 0;
        double rTR = topRight    ? Math.min(radius, maxR) : 0;
        double rBR = bottomRight ? Math.min(radius, maxR) : 0;
        double rBL = bottomLeft  ? Math.min(radius, maxR) : 0;

        if (rTL <= 0.5 && rTR <= 0.5 && rBR <= 0.5 && rBL <= 0.5) {
            drawRect((float) x, (float) y, (float) w, (float) h, color);
            return;
        }

        int topY = (int) Math.floor(y) - 1;
        int botY = (int) Math.ceil (y + h) + 1;

        for (int py = topY; py <= botY; py++) {
            double scanY = py + 0.5;
            if (scanY < y || scanY > y + h) continue;

            int pxL = (int) Math.floor(x) - 1;
            int pxR = (int) Math.ceil (x + w) + 1;

            int xL = pxL;
            while (xL <= pxR) {
                float cov = covRoundedRect(xL, py, x, y, w, h, rTL, rTR, rBR, rBL);
                if (cov >= 1f) break;
                if (cov > 0f) g.fill(xL, py, xL + 1, py + 1, ARGB(rgbVal, Math.round(baseA * cov)));
                xL++;
            }
            int xR = pxR;
            while (xR > xL) {
                float cov = covRoundedRect(xR, py, x, y, w, h, rTL, rTR, rBR, rBL);
                if (cov >= 1f) break;
                if (cov > 0f) g.fill(xR, py, xR + 1, py + 1, ARGB(rgbVal, Math.round(baseA * cov)));
                xR--;
            }
            if (xR >= xL) g.fill(xL, py, xR + 1, py + 1, color);
        }
    }

    public void drawRoundedRectOutline(double x, double y, double w, double h,
                                       double radius, int color, double thickness) {
        if (w <= 0 || h <= 0 || thickness <= 0 || A(color) == 0) return;
        int rgbVal = RGB(color);
        int baseA = A(color);

        double maxR = Math.min(w, h) * 0.5;
        double r = Math.min(radius, maxR);
        double innerR = Math.max(0, r - thickness);

        if (r <= 0.5 || thickness >= Math.min(w, h) * 0.5) {
            drawRectOutline((float) x, (float) y, (float) w, (float) h, (float) thickness, color);
            return;
        }

        double innerX = x + thickness;
        double innerY = y + thickness;
        double innerW = w - 2 * thickness;
        double innerH = h - 2 * thickness;

        int topY = (int) Math.floor(y) - 1;
        int botY = (int) Math.ceil (y + h) + 1;

        for (int py = topY; py <= botY; py++) {
            double scanY = py + 0.5;
            if (scanY < y || scanY > y + h) continue;

            int pxStart = (int) Math.floor(x) - 1;
            int pxEnd   = (int) Math.ceil (x + w) + 1;

            for (int px = pxStart; px <= pxEnd; px++) {
                float covOuter = covRoundedRect(px, py, x, y, w, h, r, r, r, r);
                float covInner = 0f;
                if (innerW > 0 && innerH > 0) {
                    covInner = covRoundedRect(px, py, innerX, innerY, innerW, innerH,
                            innerR, innerR, innerR, innerR);
                }
                float cov = covOuter - covInner;
                if (cov <= 0.01f) continue;
                int a = Math.round(baseA * cov);
                g.fill(px, py, px + 1, py + 1, ARGB(rgbVal, a));
            }
        }
    }
    public void drawTexture(Identifier texture, float x, float y, float width, float height) {
        drawTexture(texture, x, y, width, height, 0xFFFFFFFF);
    }

    public void drawTexture(Identifier texture, float x, float y, float width, float height, int color) {
        drawTexturePart(texture, x, y, width, height, 0f, 0f, 1f, 1f, color);
    }

    public void drawTexturePart(Identifier texture, float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2, int color) {
        if (width <= 0 || height <= 0) return;

        var abstractTexture = net.minecraft.client.Minecraft.getInstance()
                .getTextureManager().getTexture(texture);
        if (abstractTexture == null) return;

        var gpuTex = abstractTexture.getTexture();
        if (gpuTex == null) return;

        int texW = gpuTex.getWidth(0);
        int texH = gpuTex.getHeight(0);
        if (texW <= 0 || texH <= 0) return;

        // Normalisoidut UV:t (0..1) → pikseli-UV:t
        float uPx = u1 * texW;
        float vPx = v1 * texH;
        int uW = Math.round((u2 - u1) * texW);
        int vH = Math.round((v2 - v1) * texH);

        int px = Math.round(x);
        int py = Math.round(y);
        int pw = Math.round(width);
        int ph = Math.round(height);

        g.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                texture,
                px, py,          // int x, int y
                uPx, vPx,        // float u, float v
                pw, ph,          // int width, int height
                uW, vH,          // int regionWidth, int regionHeight
                texW, texH       // int textureWidth, int textureHeight
        );
    }

    public void drawRotatedTexture(Identifier texture, float x, float y, float width, float height,
                                   float angleDeg, int color) {
        if (width <= 0 || height <= 0) return;

        if (Math.abs(angleDeg) < 0.01f) {
            drawTexture(texture, x, y, width, height, color);
            return;
        }

        // Pyöristys pose-matriisilla
        var pose = g.pose();
        pose.pushMatrix();

        float cx = x + width / 2f;
        float cy = y + height / 2f;
        pose.translate(cx, cy);
        pose.rotate((float) Math.toRadians(angleDeg));
        pose.translate(-width / 2f, -height / 2f);

        drawTexture(texture, 0f, 0f, width, height, color);

        pose.popMatrix();
    }
    public void drawTextureFlippedY(Identifier texture, float x, float y,
                                    float width, float height, int color) {
        if (width <= 0 || height <= 0) return;

        var abstractTexture = net.minecraft.client.Minecraft.getInstance()
                .getTextureManager().getTexture(texture);
        if (abstractTexture == null) return;

        var gpuTex = abstractTexture.getTexture();
        if (gpuTex == null) return;

        int texW = gpuTex.getWidth(0);
        int texH = gpuTex.getHeight(0);
        if (texW <= 0 || texH <= 0) return;

        // Piirretään yksi yksi-pikselin korkuinen kaistale kerrallaan
        // alhaalta ylös, jolloin saadaan aikaan pystypeilaus ilman posen käyttöä.
        for (int row = 0; row < texH; row++) {
            int dstY = Math.round(y + (height * row) / texH);
            int dstH = Math.round(y + (height * (row + 1)) / texH) - dstY;
            if (dstH <= 0) continue;

            int srcV = texH - row - 1;   // peilaa rivi

            g.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    texture,
                    Math.round(x), dstY,
                    0f, (float) srcV,
                    Math.round(width), dstH,
                    texW, 1,
                    texW, texH
            );
        }
    }

    public void drawGpuTexture(
            com.mojang.renderpearl.api.textures.GpuTextureView view,
            com.mojang.renderpearl.api.textures.GpuSampler sampler,
            float x, float y, float w, float h, int color) {
        core.addGpuTextureQuad(view, sampler, x, y, w, h, 0f, 0f, 1f, 1f, color);
    }

    public void drawEntityEdge(GpuTextureView maskView, GpuSampler sampler,
                               float x, float y, float w, float h, int outlineColor) {
        core.addGpuTextureQuadWithPipeline(
                RenderPipelines.UI_ENTITY_EDGE,
                maskView, sampler,
                x, y, w, h,
                0f, 1f, 1f, 0f,    // ← v0 ja v1 vaihdettu (oli 0f, 0f, 1f, 1f)
                outlineColor);
    }

}