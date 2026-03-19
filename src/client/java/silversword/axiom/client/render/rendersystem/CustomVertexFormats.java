package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public abstract class CustomVertexFormats {
    public static final VertexFormat POS2 = VertexFormat.builder()
            .add("Position", CustomVertexElements.POS2)
            .build();

    public static final VertexFormat POS2_COLOR = VertexFormat.builder()
            .add("Position", CustomVertexElements.POS2)
            .add("Color", VertexFormatElement.COLOR)
            .build();

    public static final VertexFormat POS2_UV_COLOR = VertexFormat.builder()
            .add("Position", CustomVertexElements.POS2)
            .add("UV", VertexFormatElement.UV)
            .add("Color", VertexFormatElement.COLOR)
            .build();

    private CustomVertexFormats() {}
}