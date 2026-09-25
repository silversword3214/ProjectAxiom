package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderPipelines;

public class Renderer2D {

    private final GuiGraphicsExtractor g;   // jätetty yhteensopivuuden vuoksi
    public final RenderCore core;
    private final Matrix4f projection;

    public Renderer2D(GuiGraphicsExtractor graphics, RenderCore core, Matrix4f projection) {
        this.g = graphics;
        this.core = core;
        this.projection = projection;

        // Jokainen instanssi päivittää projektion
        this.core.beginFrame(this.projection, new Matrix4f().identity());
    }

    /** Ajaa kaikki batchit GPU:lle. */
    public void flush() {
        core.flush();
    }

    /** Paljastaa g:n niille harvoille, jotka vielä tarvitsevat vanilla-polkua (esim. item-render). */
    public GuiGraphicsExtractor gui() {
        return g;
    }

    // ================================================================
    //  Väriapurit
    // ================================================================

    private static int A(int color) { return (color >>> 24) & 0xFF; }

    // ================================================================
    //  Suorakaide
    // ================================================================

    public void drawRect(float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0 || A(color) == 0) return;
        core.addRect2D(x, y, width, height, color);
    }

    public void drawRectOutline(float x, float y, float width, float height,
                                float thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0 || A(color) == 0) return;
        core.addRectOutline2D(x, y, width, height, thickness, color);
    }

    // ================================================================
    //  Viiva (AA)
    // ================================================================

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0 || A(color) == 0) return;
        core.addLine2D(x1, y1, x2, y2, thickness, color);
    }

    // ================================================================
    //  Ympyrä (AA)
    // ================================================================

    public void drawCircle(double cx, double cy, double radius, int color) {
        if (radius <= 0 || A(color) == 0) return;
        core.addCircle((float) cx, (float) cy, (float) radius, color);
    }

    public void drawCircleOutline(double cx, double cy, double radius,
                                  int color, double thickness) {
        if (radius <= 0 || thickness <= 0 || A(color) == 0) return;
        core.addCircleOutline((float) cx, (float) cy, (float) radius,
                (float) thickness, color);
    }

    // ================================================================
    //  Pyöristetyt suorakaiteet
    // ================================================================

    public void drawRoundedRect(double x, double y, double w, double h,
                                double radius, int color) {
        if (w <= 0 || h <= 0 || A(color) == 0) return;
        core.addRoundedRect((float) x, (float) y, (float) w, (float) h,
                (float) radius, color);
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h,
                                      double radius, int color,
                                      boolean topLeft, boolean topRight,
                                      boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0 || A(color) == 0) return;
        core.addRoundedRectCustom((float) x, (float) y, (float) w, (float) h,
                (float) radius, color,
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void drawRoundedRectOutline(double x, double y, double w, double h,
                                       double radius, int color, double thickness) {
        if (w <= 0 || h <= 0 || thickness <= 0 || A(color) == 0) return;
        core.addRoundedRectOutline((float) x, (float) y, (float) w, (float) h,
                (float) radius, (float) thickness, color);
    }

    // ================================================================
    //  Tekstuurit
    // ================================================================

    public void drawTexture(Identifier texture, float x, float y,
                            float width, float height) {
        drawTexture(texture, x, y, width, height, 0xFFFFFFFF);
    }

    public void drawTexture(Identifier texture, float x, float y,
                            float width, float height, int color) {
        drawTexturePart(texture, x, y, width, height, 0f, 0f, 1f, 1f, color);
    }

    public void drawTexturePart(Identifier texture, float x, float y,
                                float width, float height,
                                float u1, float v1, float u2, float v2,
                                int color) {
        if (width <= 0 || height <= 0) return;
        core.addTexturePart(texture, x, y, width, height, u1, v1, u2, v2, color);
    }

    public void drawRotatedTexture(Identifier texture, float x, float y,
                                   float width, float height,
                                   float angleDeg, int color) {
        if (width <= 0 || height <= 0) return;
        core.addRotatedTexture(texture, x, y, width, height, angleDeg, color);
    }

    /** Piirtää tekstuurin Y-peilattuna (V-koordinaatit vaihdettu). */
    public void drawTextureFlippedY(Identifier texture, float x, float y,
                                    float width, float height, int color) {
        if (width <= 0 || height <= 0) return;
        core.addTexturePart(texture, x, y, width, height,
                0f, 1f, 1f, 0f, color);
    }

    // ================================================================
    //  GPU-tekstuurit (RT:t, maskit jne.)
    // ================================================================

    public void drawGpuTexture(GpuTextureView view, GpuSampler sampler,
                               float x, float y, float w, float h, int color) {
        core.addGpuTextureQuad(view, sampler, x, y, w, h,
                0f, 0f, 1f, 1f, color);
    }

    // ================================================================
    //  Shader ESP -apurit
    // ================================================================

    public void drawEntityChams(Identifier textureId, float x, float y,
                                float w, float h, int tint) {
        core.addTexturePart(textureId, x, y, w, h, 0f, 1f, 1f, 0f, tint);
    }

    public void drawEntityFill(Identifier textureId, float x, float y,
                               float w, float h, int fillColor) {
        core.addTextureWithPipeline(textureId, RenderPipelines.UI_ENTITY_FILL,
                x, y, w, h, 0f, 1f, 1f, 0f, fillColor);
    }

    public void drawEntityEdge(Identifier textureId, float x, float y,
                               float w, float h,
                               int outlineColor, float thickness) {
        core.addTextureWithPipeline(
                textureId,
                RenderPipelines.UI_ENTITY_EDGE,
                x, y, w, h,
                0f, 1f, 1f, 0f,
                thickness,
                outlineColor);
    }
}