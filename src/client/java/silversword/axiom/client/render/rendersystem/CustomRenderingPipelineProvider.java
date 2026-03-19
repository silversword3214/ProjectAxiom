package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import silversword.axiom.ProjectAxiom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CustomRenderingPipelineProvider {
    // Cache for shader sources to avoid repeated I/O
    public static final Map<Identifier, String> SHADER_SOURCE_CACHE = new HashMap<>();

    private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
            .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    // Pipeline fields – now non‑final, will be reassigned on rebuild
    public static RenderPipeline WORLD_COLORED;
    public static RenderPipeline WORLD_COLORED_LINES;
    public static RenderPipeline WORLD_COLORED_DEPTH;
    public static RenderPipeline WORLD_COLORED_LINES_DEPTH;
    public static RenderPipeline UI_COLORED;
    public static RenderPipeline UI_COLORED_LINES;
    public static RenderPipeline UI_TEXTURED;
    public static RenderPipeline UI_TEXT;
    public static RenderPipeline POST_OUTLINE;
    public static RenderPipeline POST_CHAMS;

    // List of builders (each corresponds to one pipeline)
    private static final List<PipelineBuilder> BUILDERS = new ArrayList<>();

    static {
        // Initialize builders in the same order as the fields above
        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/world_colored"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLineSmooth()
                .withLocation(ProjectAxiom.identifier("pipeline/world_colored_lines"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/world_colored_depth"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLineSmooth()
                .withLocation(ProjectAxiom.identifier("pipeline/world_colored_lines_depth"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/ui_colored"))
                .withVertexFormat(CustomVertexFormats.POS2_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color_2d.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/ui_colored_lines"))
                .withVertexFormat(CustomVertexFormats.POS2_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_color.frag"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/ui_textured"))
                .withVertexFormat(CustomVertexFormats.POS2_UV_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/pos_tex_color.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/pos_tex_color.frag"))
                .withSampler("u_Texture")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder(MESH_UNIFORMS)
                .withLocation(ProjectAxiom.identifier("pipeline/ui_text"))
                .withVertexFormat(CustomVertexFormats.POS2_UV_COLOR, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/text.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/text.frag"))
                .withSampler("u_Texture")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder()
                .withLocation(ProjectAxiom.identifier("pipeline/post/outline"))
                .withVertexFormat(CustomVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/post-process/base.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/post-process/outline.frag"))
                .withSampler("u_Texture")
                .withUniform("PostData", UniformType.UNIFORM_BUFFER)
                .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        BUILDERS.add(new PipelineBuilder()
                .withLocation(ProjectAxiom.identifier("pipeline/post/chams"))
                .withVertexFormat(CustomVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES)
                .withVertexShader(ProjectAxiom.identifier("shaders/post-process/base.vert"))
                .withFragmentShader(ProjectAxiom.identifier("shaders/post-process/image.frag"))
                .withSampler("u_Texture")
                .withSampler("u_TextureI")
                .withUniform("PostData", UniformType.UNIFORM_BUFFER)
                .withUniform("ImageData", UniformType.UNIFORM_BUFFER) // <-- image.frag odottaa ImageDataa
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false));

        // Initial build
        rebuildAll();
    }

    public static void rebuildAll() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();

        // Build each pipeline and assign to the corresponding field
        int index = 0;
        for (PipelineBuilder builder : BUILDERS) {
            RenderPipeline pipeline = builder.build();
            // Precompile with caching
            device.precompilePipeline(pipeline, (identifier, shaderType) -> {
                // Try cache first
                String cached = SHADER_SOURCE_CACHE.get(identifier);
                if (cached != null) return cached;

                // Load from resources
                var optional = resources.getResource(identifier);

                if (optional.isEmpty()) {
                    throw new RuntimeException("Missing shader: " + identifier);
                }
                var resource = optional.get();

                try (InputStream in = resource.getInputStream()) {
                    String source = IOUtils.toString(in, StandardCharsets.UTF_8);
                    SHADER_SOURCE_CACHE.put(identifier, source);
                    return source;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load shader: " + identifier, e);
                }
            });

            // Assign to the correct field based on index
            switch (index) {
                case 0 -> WORLD_COLORED = pipeline;
                case 1 -> WORLD_COLORED_LINES = pipeline;
                case 2 -> WORLD_COLORED_DEPTH = pipeline;
                case 3 -> WORLD_COLORED_LINES_DEPTH = pipeline;
                case 4 -> UI_COLORED = pipeline;
                case 5 -> UI_COLORED_LINES = pipeline;
                case 6 -> UI_TEXTURED = pipeline;
                case 7 -> UI_TEXT = pipeline;
                case 8 -> POST_OUTLINE = pipeline;
                case 9 -> POST_CHAMS = pipeline;

            }
            index++;
            System.out.println("Rebuilt: " + pipeline.getLocation());
        }
    }

    // Optional: keep a precompile method that just forces compilation of current pipelines
    public static void precompile() {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();

        RenderPipeline[] pipelines = {
                WORLD_COLORED, WORLD_COLORED_LINES, WORLD_COLORED_DEPTH, WORLD_COLORED_LINES_DEPTH,
                UI_COLORED, UI_COLORED_LINES, UI_TEXTURED, UI_TEXT, POST_OUTLINE, POST_CHAMS
        };

        for (RenderPipeline pipeline : pipelines) {
            if (pipeline == null) continue;
            device.precompilePipeline(pipeline, (identifier, shaderType) -> {
                String cached = SHADER_SOURCE_CACHE.get(identifier);
                if (cached != null) return cached;
                var resource = resources.getResource(identifier).get();
                try (InputStream in = resource.getInputStream()) {
                    String source = IOUtils.toString(in, StandardCharsets.UTF_8);
                    SHADER_SOURCE_CACHE.put(identifier, source);
                    return source;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("Precompiling: " + pipeline.getLocation());
        }
    }
}