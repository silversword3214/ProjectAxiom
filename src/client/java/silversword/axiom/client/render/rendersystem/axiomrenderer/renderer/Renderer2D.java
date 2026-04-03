package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;

public class Renderer2D {
    private final GuiGraphics graphics;
    public final RenderCore core;
    private final Matrix4f projection;


    public Renderer2D(GuiGraphics graphics, RenderCore core, Matrix4f projection) {
        this.graphics = graphics;
        this.core = core;
        this.projection = projection;
        this.core.beginFrame(this.projection, new Matrix4f().identity());
    }

    // --- Basic shapes ---

    public void drawRect(float x, float y, float width, float height, int color) {
        core.addRect2D(x, y, width, height, color);
    }

    public void drawRectOutline(float x, float y, float width, float height, float thickness, int color) {
        core.addRectOutline2D(x, y, width, height, thickness, color);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        core.addLine2D(x1, y1, x2, y2, thickness, color);
    }

    // --- Advanced shapes ---

    public void drawCircle(double cx, double cy, double radius, int color) {
        core.addCircle((float) cx, (float) cy, (float) radius, color);
    }

    public void drawCircleOutline(double cx, double cy, double radius, int color, double thickness) {
        core.addCircleOutline((float) cx, (float) cy, (float) radius, (float) thickness, color);
    }

    public void drawRoundedRect(double x, double y, double w, double h, double radius, int color) {
        core.addRoundedRect((float) x, (float) y, (float) w, (float) h, (float) radius, color);
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h, double radius, int color,
                                      boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        core.addRoundedRectCustom((float) x, (float) y, (float) w, (float) h, (float) radius, color,
                topLeft, topRight, bottomRight, bottomLeft); //
    }

    public void drawRoundedRectOutline(double x, double y, double w, double h, double radius, int color, double thickness) {
        core.addRoundedRectOutline((float) x, (float) y, (float) w, (float) h, (float) radius, (float) thickness, color);
    }

    // Texture drawing
    public void drawTexture(Identifier texture, float x, float y, float width, float height) {
        drawTexture(texture, x, y, width, height, 0xFFFFFFFF);
    }

    public void drawTexture(Identifier texture, float x, float y, float width, float height, int color) {
        core.addTexture(texture, x, y, width, height, color);
    }

    public void drawTexturePart(Identifier texture, float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2) {
        drawTexturePart(texture, x, y, width, height, u1, v1, u2, v2, 0xFFFFFFFF);
    }

    public void drawTexturePart(Identifier texture, float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2, int color) {
        core.addTexturePart(texture, x, y, width, height, u1, v1, u2, v2, color);
    }
}