package silversword.axiom.client.rendersystem.axiomrenderer.api;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.RenderCore;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.RenderPipelines;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.renderer.ScreenRenderer;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.renderer.TextureRenderer;
public class Renderer2D {

    private final GuiGraphicsExtractor g;
    public final RenderCore core;
    private final Matrix4f projection;

    private final ScreenRenderer  screen;
    private final TextureRenderer textures;

    public Renderer2D(GuiGraphicsExtractor graphics, RenderCore core, Matrix4f projection) {
        this.g = graphics;
        this.core = core;
        this.projection = projection;
        this.screen   = new ScreenRenderer(core);
        this.textures = new TextureRenderer(core);

        this.core.beginFrame(this.projection, new Matrix4f().identity());
    }

    public void flush() {
        core.flush();
    }

    public GuiGraphicsExtractor gui() {
        return g;
    }

    private static int A(int color) { return (color >>> 24) & 0xFF; }

    // ============================================================
    //  Muodot
    // ============================================================

    public void drawRect(float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0 || A(color) == 0) return;
        screen.rect(x, y, width, height, color);
    }

    public void drawRectOutline(float x, float y, float width, float height,
                                float thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0 || A(color) == 0) return;
        screen.rectOutline(x, y, width, height, thickness, color);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0 || A(color) == 0) return;
        screen.line(x1, y1, x2, y2, thickness, color);
    }

    public void drawCircle(double cx, double cy, double radius, int color) {
        if (radius <= 0 || A(color) == 0) return;
        screen.circle((float) cx, (float) cy, (float) radius, color);
    }

    public void drawCircleOutline(double cx, double cy, double radius,
                                  int color, double thickness) {
        if (radius <= 0 || thickness <= 0 || A(color) == 0) return;
        screen.circleOutline((float) cx, (float) cy, (float) radius,
                (float) thickness, color);
    }

    public void drawRoundedRect(double x, double y, double w, double h,
                                double radius, int color) {
        if (w <= 0 || h <= 0 || A(color) == 0) return;
        screen.roundedRect((float) x, (float) y, (float) w, (float) h,
                (float) radius, color);
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h,
                                      double radius, int color,
                                      boolean topLeft, boolean topRight,
                                      boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0 || A(color) == 0) return;
        screen.roundedRectCustom((float) x, (float) y, (float) w, (float) h,
                (float) radius, color,
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void drawRoundedRectOutline(double x, double y, double w, double h,
                                       double radius, int color, double thickness) {
        if (w <= 0 || h <= 0 || thickness <= 0 || A(color) == 0) return;
        screen.roundedRectOutline((float) x, (float) y, (float) w, (float) h,
                (float) radius, (float) thickness, color);
    }

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
        textures.texturePart(texture, x, y, width, height, u1, v1, u2, v2, color);
    }

    public void drawRotatedTexture(Identifier texture, float x, float y,
                                   float width, float height,
                                   float angleDeg, int color) {
        if (width <= 0 || height <= 0) return;
        textures.rotatedTexture(texture, x, y, width, height, angleDeg, color);
    }

    public void drawTextureFlippedY(Identifier texture, float x, float y,
                                    float width, float height, int color) {
        if (width <= 0 || height <= 0) return;
        textures.textureFlippedY(texture, x, y, width, height, color);
    }

    public void drawGpuTexture(GpuTextureView view, GpuSampler sampler,
                               float x, float y, float w, float h, int color) {
        textures.gpuTexture(view, sampler, x, y, w, h,
                0f, 0f, 1f, 1f, color);
    }

    public void drawEntityChams(Identifier textureId, float x, float y,
                                float w, float h, int tint) {
        textures.textureFlippedY(textureId, x, y, w, h, tint);
    }

    public void drawEntityChamsWithPipeline(Identifier textureId, float x, float y,
                                            float w, float h, int tint) {
        textures.textureWithPipeline(textureId, RenderPipelines.UI_ENTITY_FILL,
                x, y, w, h, 0f, 1f, 1f, 0f, tint);
    }

    public void drawEntityFill(Identifier textureId, float x, float y,
                               float w, float h, int fillColor) {
        textures.textureWithPipeline(textureId, RenderPipelines.UI_ENTITY_FILL,
                x, y, w, h, 0f, 1f, 1f, 0f, fillColor);
    }

    public void drawEntityEdge(Identifier textureId, float x, float y,
                               float w, float h,
                               int outlineColor, float thickness) {
        textures.textureWithPipeline(
                textureId,
                RenderPipelines.UI_ENTITY_EDGE,
                x, y, w, h,
                0f, 1f, 1f, 0f,
                thickness,
                outlineColor);
    }
}