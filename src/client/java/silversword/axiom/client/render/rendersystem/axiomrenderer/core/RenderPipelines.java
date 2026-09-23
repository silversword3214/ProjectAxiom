package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderPipelines.class);
    public static final Map<Identifier, String> SHADER_SOURCE_CACHE = new HashMap<>();

    private static final RenderPipeline.Snippet DYNAMIC_TRANSFORMS =
            RenderPipeline.builder()
                    .withBindGroupLayout(
                            BindGroupLayout.builder()
                                    .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                                    .build()
                    )
                    .buildSnippet();

    private static final RenderPipeline.Snippet UI_TEXTURE_BINDINGS =
            RenderPipeline.builder()
                    .withBindGroupLayout(
                            BindGroupLayout.builder()
                                    .withSampler("u_Texture")
                                    .build()
                    )
                    .buildSnippet();

    // Public pipeline fields
    public static RenderPipeline WORLD_COLORED;
    public static RenderPipeline WORLD_COLORED_LINES;
    public static RenderPipeline WORLD_COLORED_DEPTH;
    public static RenderPipeline WORLD_COLORED_LINES_DEPTH;
    public static RenderPipeline UI_COLORED;
    public static RenderPipeline UI_COLORED_LINES;
    public static RenderPipeline UI_TEXTURED;
    public static RenderPipeline UI_TEXT;
    public static RenderPipeline ENTITY_MASK;
    public static RenderPipeline SHADER_OUTLINE;
    public static RenderPipeline SHADER_COMPOSITE;

    private static final List<PipelineBuilder> BUILDERS = new ArrayList<>();

    static {
        // 1. World quads (no depth)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/world_colored"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 2. World lines (no depth)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLineSmooth()
                .withLocation(id("pipeline/world_colored_lines"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.DEBUG_LINES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 3. World quads with depth
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/world_colored_depth"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(CompareOp.LESS_THAN_OR_EQUAL)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 4. World lines with depth
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/world_colored_lines_depth"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.DEBUG_LINES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(CompareOp.LESS_THAN_OR_EQUAL)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        /// UI colored
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/ui_colored.vert"))
                .withFragmentShader(id("shaders/ui_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

// UI colored lines
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored_lines"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.DEBUG_LINES)
                .withVertexShader(id("shaders/ui_colored.vert"))
                .withFragmentShader(id("shaders/ui_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

// UI textured
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS, UI_TEXTURE_BINDINGS)
                .withLocation(id("pipeline/ui_textured"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/ui_textured.vert"))
                .withFragmentShader(id("shaders/ui_textured.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

// UI text
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS, UI_TEXTURE_BINDINGS)
                .withLocation(id("pipeline/ui_text"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/text.vert"))
                .withFragmentShader(id("shaders/text.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 9. Noop entity mask
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/entity_mask"))
                .withVertexFormat(DefaultVertexFormat.ENTITY, PrimitiveTopology.QUADS)
                .withVertexShader(id("shaders/entity/noop_mask.vert"))
                .withFragmentShader(id("shaders/entity/noop_mask.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 10. Shader outline
        BUILDERS.add(new PipelineBuilder()
                .withLocation(id("pipeline/shader_outline"))
                .withVertexFormat(AxiomVertexFormats.EMPTY, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/post/outline.vert"))
                .withFragmentShader(id("shaders/post/outline.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 11. Composite pass
        BUILDERS.add(new PipelineBuilder()
                .withLocation(id("pipeline/shader_composite"))
                .withVertexFormat(AxiomVertexFormats.EMPTY, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/post/outline.vert"))
                .withFragmentShader(id("shaders/post/composite.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("projectaxiom", path);
    }

    public static void rebuildAll() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();

        int index = 0;
        for (PipelineBuilder builder : BUILDERS) {
            RenderPipeline pipeline = builder.build();
            device.precompilePipeline(pipeline, (identifier, shaderType) -> {
                String cached = SHADER_SOURCE_CACHE.get(identifier);
                if (cached != null) return cached;

                var optional = resources.getResource(identifier);
                if (optional.isEmpty()) {
                    LOGGER.error("Shader not found: {}", identifier);
                    throw new RuntimeException("Missing shader: " + identifier); // Keskeytä alustus
                }
                try (InputStream in = optional.get().open()) {
                    String source = IOUtils.toString(in, StandardCharsets.UTF_8);
                    SHADER_SOURCE_CACHE.put(identifier, source);
                    return source;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load shader: " + identifier, e);
                }
            });

            switch (index) {
                case 0 -> WORLD_COLORED = pipeline;
                case 1 -> WORLD_COLORED_LINES = pipeline;
                case 2 -> WORLD_COLORED_DEPTH = pipeline;
                case 3 -> WORLD_COLORED_LINES_DEPTH = pipeline;
                case 4 -> UI_COLORED = pipeline;
                case 5 -> UI_COLORED_LINES = pipeline;
                case 6 -> UI_TEXTURED = pipeline;
                case 7 -> UI_TEXT = pipeline;
                case 8 -> ENTITY_MASK = pipeline;
                case 9 -> SHADER_OUTLINE = pipeline;
                case 10 -> SHADER_COMPOSITE = pipeline;
            }
            index++;
            LOGGER.info("Rebuilt pipeline: {}", pipeline.getLocation());
        }
    }


}
