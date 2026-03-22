package silversword.axiom.client.utils.render;

import net.minecraft.resources.Identifier;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DrawTexture {
    private static final Map<Texture, List<TextureQuad>> QUADS_BY_TEXTURE = new HashMap<>();

    public static void add(Identifier textureId, double x, double y, double width, double height, Color color) {
        add(textureId, x, y, width, height, 0, color);
    }

    public static void add(Identifier textureId, double x, double y, double width, double height, double rotation, Color color) {
        Texture tex = TextureManager.getTexture(textureId);
        if (tex != null) {
            QUADS_BY_TEXTURE.computeIfAbsent(tex, k -> new ArrayList<>()).add(new TextureQuad(tex, x, y, width, height, rotation, color));
        }
    }

    public static void renderAll() {
        if (QUADS_BY_TEXTURE.isEmpty()) return;

        for (Map.Entry<Texture, List<TextureQuad>> entry : QUADS_BY_TEXTURE.entrySet()) {
            Texture tex = entry.getKey();
            List<TextureQuad> quads = entry.getValue();

            Renderer2D.TEXTURE.begin();

            for (TextureQuad quad : quads) {
                if (quad.rotation != 0) {
                    Renderer2D.TEXTURE.texQuad(quad.x, quad.y, quad.width, quad.height, quad.rotation, 0, 0, 1, 1, quad.color);
                } else {
                    Renderer2D.TEXTURE.texQuad(quad.x, quad.y, quad.width, quad.height, quad.color);
                }
            }

            Renderer2D.TEXTURE.render(tex.textureView(), tex.sampler());
        }

        QUADS_BY_TEXTURE.clear();
    }

    private record TextureQuad(Texture tex, double x, double y, double width, double height, double rotation, Color color) {}
}