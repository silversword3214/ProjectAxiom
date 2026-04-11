package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
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

    private static final RenderPipeline.Snippet DYNAMIC_TRANSFORMS = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
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
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 2. World lines (no depth)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLineSmooth()
                .withLocation(id("pipeline/world_colored_lines"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.DEBUG_LINES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 3. World quads with depth
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/world_colored_depth"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 4. World lines with depth
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/world_colored_lines_depth"))
                .withVertexFormat(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.DEBUG_LINES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 5. UI quads (2D)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/world_colored.vert")) // make sure this shader exists
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 6. UI lines (2D)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored_lines"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.DEBUG_LINES)
                .withVertexShader(id("shaders/world_colored.vert"))
                .withFragmentShader(id("shaders/world_colored.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 7. UI textured quads
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_textured"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/ui_textured.vert"))
                .withFragmentShader(id("shaders/ui_textured.frag"))
                .withSampler("u_Texture")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 8. UI text (SDF)
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_text"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/text.vert"))
                .withFragmentShader(id("shaders/text.frag"))
                .withSampler("u_Texture")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 9. Noop entity mask
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/entity_mask"))
                .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
                .withVertexShader(id("shaders/entity/noop_mask.vert"))
                .withFragmentShader(id("shaders/entity/noop_mask.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 10. Shader outline
        BUILDERS.add(new PipelineBuilder()
                .withLocation(id("pipeline/shader_outline"))
                .withVertexFormat(AxiomVertexFormats.EMPTY, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/post/outline.vert"))
                .withFragmentShader(id("shaders/post/outline.frag"))
                .withSampler("u_Scene")
                .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 11. Composite pass
        BUILDERS.add(new PipelineBuilder()
                .withLocation(id("pipeline/shader_composite"))
                .withVertexFormat(AxiomVertexFormats.EMPTY, VertexFormat.Mode.TRIANGLES)
                .withVertexShader(id("shaders/post/outline.vert"))
                .withFragmentShader(id("shaders/post/composite.frag"))
                .withSampler("u_Scene")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
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
