package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public abstract class AxiomVertexElements {
    // 2D position (x,y)
    public static final VertexFormatElement POS2 = register("POS2", 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 2);
    // 3D position (x,y,z)
    public static final VertexFormatElement POS3 = register("POS3", 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);

    private AxiomVertexElements() {}

    private static VertexFormatElement register(String name, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        int id = idFinder();
        return VertexFormatElement.register(id, index, type, usage, count);
    }

    private static int idFinder() {
        int id = 0;
        while (VertexFormatElement.byId(id) != null) {
            id++;
            if (id >= 32) {
                throw new RuntimeException("Too many vertex formats registered");
            }
        }
        return id;
    }
}