package silversword.axiom.client.utils.render;

import net.minecraft.resources.Identifier;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

public final class DrawTexture {

    public static void add(Identifier textureId, double x, double y, double width, double height, Color color) {
        add(textureId, x, y, width, height, 0, color);
    }

    public static void add(Identifier textureId, double x, double y, double width, double height, double rotation, Color color) {
        RenderAPI.getInstance().getCore().addTexture(textureId, (float) x, (float) y, (float) width, (float) height, color.getARGB());
    }

    public static void renderAll() {

    }
}