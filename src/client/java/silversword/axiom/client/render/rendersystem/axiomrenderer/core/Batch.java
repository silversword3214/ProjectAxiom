// Batch.java
package silversword.axiom.client.render.rendersystem.axiomrenderer.core;


import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Batch {
    private final VertexFormat format;
    private final PrimitiveTopology mode;
    private final List<float[]> vertices = new ArrayList<>();
    private Identifier texture;

    public Batch(VertexFormat format, PrimitiveTopology mode) {
        this.format = format;
        this.mode = mode;
    }

    public VertexFormat getFormat() { return format; }
    public PrimitiveTopology getMode() { return mode; }
    public int vertexCount() { return vertices.size(); }
    public List<float[]> getVertices() { return vertices; }
    public Identifier getTexture() { return texture; }
    public void setTexture(Identifier texture) { this.texture = texture; }

    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        vertices.add(new float[]{x, y, z, r, g, b, a});
    }

    public void vertex2D(float x, float y, float r, float g, float b, float a) {
        vertex(x, y, 0f, r, g, b, a);
    }

    public void vertexUV(float x, float y, float u, float v, float r, float g, float b, float a) {
        if (format != AxiomVertexFormats.POS2_UV_COLOR) {
            throw new IllegalStateException("Wrong vertex format, expected POS2_UV_COLOR");
        }
        vertices.add(new float[]{x, y, 0f, u, v, r, g, b, a});
    }

    public void clear() {
        vertices.clear();
        texture = null;
    }
}