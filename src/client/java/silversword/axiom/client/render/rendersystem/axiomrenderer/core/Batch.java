// Batch.java
package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Batch {
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final List<float[]> vertices = new ArrayList<>();
    private Identifier texture; // vain jos format on POS2_UV_COLOR

    public Batch(VertexFormat format, VertexFormat.Mode mode) {
        this.format = format;
        this.mode = mode;
    }

    public VertexFormat getFormat() { return format; }
    public VertexFormat.Mode getMode() { return mode; }
    public int vertexCount() { return vertices.size(); }
    public List<float[]> getVertices() { return vertices; }
    public Identifier getTexture() { return texture; }
    public void setTexture(Identifier texture) { this.texture = texture; }

    // 3D / 2D värimuoto (POS3_COLOR, POS2_COLOR)
    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        vertices.add(new float[]{x, y, z, r, g, b, a});
    }

    // 2D värimuoto (POS2_COLOR) – käytetään myös ympyröiden jne. apuna
    public void vertex2D(float x, float y, float r, float g, float b, float a) {
        vertex(x, y, 0f, r, g, b, a);
    }

    // Tekstuurimuoto (POS2_UV_COLOR)
    public void vertexUV(float x, float y, float u, float v, float r, float g, float b, float a) {
        if (format != AxiomVertexFormats.POS2_UV_COLOR) {
            throw new IllegalStateException("Wrong vertex format, expected POS2_UV_COLOR");
        }
        vertices.add(new float[]{x, y, 0f, u, v, r, g, b, a});
    }

    /** Tyhjentää tämän erän, jotta se voidaan käyttää uudelleen seuraavalla framella. */
    public void clear() {
        vertices.clear();
        texture = null;
    }
}