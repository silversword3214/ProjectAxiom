package silversword.axiom.client.utils.render;

import silversword.axiom.client.rendersystem.axiomrenderer.font.TextRenderer;

public class TextUtils {
    private static final double SCALE_FIX = 1.5 / 2.3;

    public static int getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.round(TextRenderer.get().getWidth(text, false) * SCALE_FIX);
    }

    public static int getHeight() {
        return (int) Math.round(TextRenderer.get().getHeight(false) * SCALE_FIX);
    }
}