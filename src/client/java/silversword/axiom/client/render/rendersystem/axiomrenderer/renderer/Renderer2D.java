package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;

public class Renderer2D {
    private final GuiGraphicsExtractor graphics;
    public final RenderCore core;
    private final Matrix4f projection;


    public Renderer2D(GuiGraphicsExtractor graphics, RenderCore core, Matrix4f projection) {
        this.graphics = graphics;
        this.core = core;
        this.projection = projection;
        this.core.beginFrame(this.projection, new Matrix4f().identity());
    }
    // --- Basic shapes ---

    public void drawRect(float x, float y, float width, float height, int color) {
        fill(x, y, width, height, color);
    }

    public void drawRectOutline(float x, float y, float width, float height, float thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) return;
        float edge = Math.min(thickness, Math.min(width, height) / 2.0f);
        fill(x, y, width, edge, color);
        fill(x, y + height - edge, width, edge, color);
        fill(x, y + edge, edge, height - 2.0f * edge, color);
        fill(x + width - edge, y + edge, edge, height - 2.0f * edge, color);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        for (int i = 0; i <= steps; i++) {
            float progress = (float) i / steps;
            fill(x1 + dx * progress - thickness / 2.0f, y1 + dy * progress - thickness / 2.0f,
                    thickness, thickness, color);
        }
    }

    // --- Advanced shapes ---

    public void drawCircle(double cx, double cy, double radius, int color) {
        if (radius <= 0) return;
        int r = Math.max(1, Math.round((float) radius));
        for (int dy = -r; dy <= r; dy++) {
            int halfWidth = (int) Math.sqrt(r * r - dy * dy);
            fill((float) cx - halfWidth, (float) cy + dy, halfWidth * 2 + 1, 1, color);
        }
    }

    public void drawCircleOutline(double cx, double cy, double radius, int color, double thickness) {
        if (radius <= 0 || thickness <= 0) return;
        int outer = Math.max(1, Math.round((float) radius));
        int inner = Math.max(0, Math.round((float) radius - (float) thickness));
        for (int dy = -outer; dy <= outer; dy++) {
            int outerWidth = (int) Math.sqrt(outer * outer - dy * dy);
            int innerWidth = Math.abs(dy) <= inner ? (int) Math.sqrt(inner * inner - dy * dy) : -1;
            fill((float) cx - outerWidth, (float) cy + dy, outerWidth - innerWidth, 1, color);
            fill((float) cx + innerWidth + 1, (float) cy + dy, outerWidth - innerWidth, 1, color);
        }
    }

    public void drawRoundedRect(double x, double y, double w, double h, double radius, int color) {
        drawRoundedRectCustom(x, y, w, h, radius, color, true, true, true, true);
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h, double radius, int color,
                                      boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0) return;
        int left = Math.round((float) x);
        int top = Math.round((float) y);
        int right = Math.round((float) (x + w));
        int bottom = Math.round((float) (y + h));
        int r = Math.min(Math.max(0, Math.round((float) radius)), Math.min((right - left) / 2, (bottom - top) / 2));
        if (r == 0) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        for (int row = 0; row < bottom - top; row++) {
            int topDistance = r - 1 - row;
            int bottomDistance = row - ((bottom - top) - r);
            int leftInset = 0;
            int rightInset = 0;
            if (row < r) {
                int inset = r - (int) Math.sqrt(r * r - topDistance * topDistance);
                if (topLeft) leftInset = inset;
                if (topRight) rightInset = inset;
            } else if (row >= (bottom - top) - r) {
                int inset = r - (int) Math.sqrt(r * r - bottomDistance * bottomDistance);
                if (bottomLeft) leftInset = inset;
                if (bottomRight) rightInset = inset;
            }
            graphics.fill(left + leftInset, top + row, right - rightInset, top + row + 1, color);
        }
    }

    public void drawRoundedRectOutline(double x, double y, double w, double h, double radius, int color, double thickness) {
        drawRectOutline((float) x, (float) y, (float) w, (float) h, (float) thickness, color);
    }

    // Texture drawing
    public void drawTexture(Identifier texture, float x, float y, float width, float height) {
        drawTexture(texture, x, y, width, height, 0xFFFFFFFF);
    }

    public void drawTexture(Identifier texture, float x, float y, float width, float height, int color) {
        drawTexturePart(texture, x, y, width, height, 0, 0, 1, 1, color);
    }

    public void drawTexturePart(Identifier texture, float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2) {
        drawTexturePart(texture, x, y, width, height, u1, v1, u2, v2, 0xFFFFFFFF);
    }

    public void drawTexturePart(Identifier texture, float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(texture, Math.round(x), Math.round(y), Math.round(width), Math.round(height), u1, v1, u2, v2);
    }

    public void drawRotatedTexture(Identifier texture, float x, float y, float width, float height, float angleDeg, int color) {
        // GuiGraphicsExtractor owns the render pass in 26.2. Keep textures in that pass;
        // rotation can be restored with a dedicated GUI render state without bypassing it.
        drawTexture(texture, x, y, width, height, color);
    }

    private void fill(float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);
        if (right > left && bottom > top) {
            graphics.fill(left, top, right, bottom, color);
        }
    }
}
