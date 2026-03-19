package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public abstract class CustomVertexElements {
    public static final VertexFormatElement POS2 = register("POS2", 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 2);

    private CustomVertexElements() {}

    private static VertexFormatElement register(String name, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        int id = findFreeId();
        return VertexFormatElement.register(id, index, type, usage, count);
    }

    private static int findFreeId() {
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