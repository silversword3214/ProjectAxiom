package silversword.axiom.client.render.rendersystem.axiomrenderer.core;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.*;
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
                                    .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
                                    .build()
                    )
                    .buildSnippet();

    // 26.3: kaikki pipelinet ovat CompiledRenderPipeline
    public static CompiledRenderPipeline WORLD_COLORED;
    public static CompiledRenderPipeline WORLD_COLORED_LINES;
    public static CompiledRenderPipeline WORLD_COLORED_DEPTH;
    public static CompiledRenderPipeline WORLD_COLORED_LINES_DEPTH;
    public static CompiledRenderPipeline UI_COLORED;
    public static CompiledRenderPipeline UI_COLORED_LINES;
    public static CompiledRenderPipeline UI_TEXTURED;
    public static CompiledRenderPipeline UI_TEXT;

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

        // 5. UI colored
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/ui_colored.vert"))
                .withFragmentShader(id("shaders/ui_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 6. UI colored lines
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS)
                .withLocation(id("pipeline/ui_colored_lines"))
                .withVertexFormat(AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.DEBUG_LINES)
                .withVertexShader(id("shaders/ui_colored.vert"))
                .withFragmentShader(id("shaders/ui_colored.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 7. UI textured
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS, UI_TEXTURE_BINDINGS)
                .withLocation(id("pipeline/ui_textured"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/ui_textured.vert"))
                .withFragmentShader(id("shaders/ui_textured.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // 8. UI text
        BUILDERS.add(new PipelineBuilder(DYNAMIC_TRANSFORMS, UI_TEXTURE_BINDINGS)
                .withLocation(id("pipeline/ui_text"))
                .withVertexFormat(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES)
                .withVertexShader(id("shaders/text.vert"))
                .withFragmentShader(id("shaders/text.frag"))
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("projectaxiom", path);
    }

    private static final class ResourceShaderSource implements ShaderSource {
        private final ResourceManager resources;

        ResourceShaderSource(ResourceManager resources) {
            this.resources = resources;
        }

        @Override
        public String getShader(Identifier shaderId, ShaderType type) {
            String cached = SHADER_SOURCE_CACHE.get(shaderId);
            if (cached != null) return cached;

            var optional = resources.getResource(shaderId);
            if (optional.isEmpty()) {
                LOGGER.error("Shader not found: {}", shaderId);
                throw new RuntimeException("Missing shader: " + shaderId);
            }
            try (InputStream in = optional.get().open()) {
                String src = IOUtils.toString(in, StandardCharsets.UTF_8);
                SHADER_SOURCE_CACHE.put(shaderId, src);
                return src;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load shader: " + shaderId, e);
            }
        }

        @Override
        public ShaderSource.CachedIncludeSource getInclude(Identifier includeId) {
            return null;
        }

        @Override
        public void close() {
        }
    }

    public static void rebuildAll() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ShaderSource source = new ResourceShaderSource(resources);

        int index = 0;
        for (PipelineBuilder builder : BUILDERS) {
            RenderPipeline pipeline = builder.build();

            CompiledRenderPipeline compiled;
            try {
                CompiledRenderPipeline.Pending pending = device
                        .compilePipeline(pipeline, source, Runnable::run)
                        .join();
                compiled = pending.finishCompile();
            } catch (Exception e) {
                LOGGER.error("Pipeline compile failed: {}", pipeline.getLocation(), e);
                compiled = null;
            }

            if (compiled == null) {
                LOGGER.error("Pipeline compile returned null: {}", pipeline.getLocation());
                index++;
                continue;
            }

            switch (index) {
                case 0 -> WORLD_COLORED             = compiled;
                case 1 -> WORLD_COLORED_LINES       = compiled;
                case 2 -> WORLD_COLORED_DEPTH       = compiled;
                case 3 -> WORLD_COLORED_LINES_DEPTH = compiled;
                case 4 -> UI_COLORED                = compiled;
                case 5 -> UI_COLORED_LINES          = compiled;
                case 6 -> UI_TEXTURED               = compiled;
                case 7 -> UI_TEXT                   = compiled;
            }
            index++;
            LOGGER.info("Compiled pipeline: {}", pipeline.getLocation());
        }
    }
}