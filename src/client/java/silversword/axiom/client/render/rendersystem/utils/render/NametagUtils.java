package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

public final class NametagUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Converts a 3D world position to 2D screen coordinates.
     * @param renderer the active 3D renderer (provides projection & view matrices)
     * @param worldPos world position
     * @return a new Vec3 containing (screenX, screenY, depth) if in front, otherwise null
     */
    public static Vec3 worldToScreen(Renderer3D renderer, Vec3 worldPos) {
        Matrix4f proj = renderer.getProjectionMatrix();
        Matrix4f view = renderer.getViewMatrix();

        Vector4f clip = new Vector4f((float) worldPos.x, (float) worldPos.y, (float) worldPos.z, 1.0f);
        clip.mul(view).mul(proj);

        if (clip.w <= 0.0f) return null; // behind camera

        // Perspective division
        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;

        // Convert to screen coordinates
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        float screenX = (ndcX * 0.5f + 0.5f) * width;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * height;
        float depth = clip.z * invW;

        return new Vec3(screenX, screenY, depth);
    }

    /**
     * Draws a text nametag at the given screen position (pixel coordinates).
     * @param ctx HUD context for drawing
     * @param screenX screen X coordinate
     * @param screenY screen Y coordinate
     * @param text the text to draw
     * @param textColor text color
     * @param bgColor background color (may be transparent)
     * @param scale text scale (1.0 = normal size)
     */
    public static void drawNametag(HudContext ctx, double screenX, double screenY,
                                   String text, Color textColor, Color bgColor, float scale) {
        float padding = 4 * scale;
        float radius = 3 * scale;

        // Text dimensions
        float textWidth = ctx.textWidth(text) * scale;
        float textHeight = ctx.fontHeight() * scale;

        // Background rectangle
        float bgX = (float) (screenX - textWidth / 2 - padding);
        float bgY = (float) (screenY - textHeight - padding * 2);
        float bgW = textWidth + padding * 2;
        float bgH = textHeight + padding * 2;

        // Draw background
        if (bgColor.getAlpha() > 0) {
            ctx.fillRounded((int) bgX, (int) bgY, (int) bgW, (int) bgH, (int) radius, bgColor.getARGB());
        }

        // Draw text
        float textX = bgX + padding;
        float textY = bgY + padding;
        ctx.drawScaledText(text, (int) textX, (int) textY, textColor.getARGB(), true, scale);
    }

    /**
     * Convenience method: converts world position to screen and draws nametag.
     * @param renderer active 3D renderer
     * @param ctx HUD context
     * @param worldPos world position
     * @param text text to display
     * @param textColor text color
     * @param bgColor background color
     * @param scale text scale
     * @return true if drawn, false if behind camera
     */
    public static boolean renderNametag(Renderer3D renderer, HudContext ctx,
                                        Vec3 worldPos, String text,
                                        Color textColor, Color bgColor, float scale) {
        Vec3 screenPos = worldToScreen(renderer, worldPos);
        if (screenPos == null) return false;

        drawNametag(ctx, screenPos.x, screenPos.y, text, textColor, bgColor, scale);
        return true;
    }
}