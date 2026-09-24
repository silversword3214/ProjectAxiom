package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudEventGuard {
    private static GuiGraphicsExtractor lastGraphics = null;

    private HudEventGuard() {}

    public static boolean shouldPostEvent(GuiGraphicsExtractor graphics) {
        if (graphics == lastGraphics) return false;
        lastGraphics = graphics;
        return true;
    }
}