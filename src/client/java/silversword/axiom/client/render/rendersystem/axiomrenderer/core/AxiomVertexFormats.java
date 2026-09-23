package silversword.axiom.client.render.rendersystem.axiomrenderer.core;


import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;

public abstract class AxiomVertexFormats {
    // 2D position (x,y,0)
    public static final VertexFormat POS2_COLOR = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .build();

    // 3D position (x,y,z)
    public static final VertexFormat POS3_COLOR = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .build();

    // For textured UI (2D position + UV + color)
    public static final VertexFormat POS2_UV_COLOR = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)  // Vaihda "UV" -> "UV0"
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .build();

    public static final VertexFormat EMPTY = VertexFormat.builder(0).build();

    private AxiomVertexFormats() {}
}