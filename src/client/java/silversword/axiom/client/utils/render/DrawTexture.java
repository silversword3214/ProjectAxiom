package silversword.axiom.client.utils.render;

import net.minecraft.resources.Identifier;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.List;

public final class DrawTexture {
    private static final List<TextureCommand> commands = new ArrayList<>();

    public static void add(Identifier textureId, double x, double y, double width, double height, Color color) {
        add(textureId, x, y, width, height, 0, color);
    }

    public static void add(Identifier textureId, double x, double y, double width, double height, double rotation, Color color) {
        commands.add(new TextureCommand(textureId, (float) x, (float) y, (float) width, (float) height, (float) rotation, color.getARGB()));
    }

    public static void renderAll(Renderer2D renderer) {
        for (TextureCommand cmd : commands) {
            if (cmd.rotation == 0) {
                renderer.drawTexture(cmd.textureId, cmd.x, cmd.y, cmd.w, cmd.h, cmd.color);
            } else {
                renderer.drawRotatedTexture(cmd.textureId, cmd.x, cmd.y, cmd.w, cmd.h, cmd.rotation, cmd.color);
            }
        }
        commands.clear();
    }

    private static class TextureCommand {
        Identifier textureId;
        float x, y, w, h, rotation;
        int color;
        TextureCommand(Identifier id, float x, float y, float w, float h, float rot, int col) {
            this.textureId = id;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.rotation = rot; this.color = col;
        }
    }
}
