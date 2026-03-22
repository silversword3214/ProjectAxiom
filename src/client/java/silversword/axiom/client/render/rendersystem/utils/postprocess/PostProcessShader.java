package silversword.axiom.client.render.rendersystem.utils.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.DynamicUniformStorage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import silversword.axiom.ProjectAxiom;
import silversword.axiom.client.render.rendersystem.BufferRenderer;
import silversword.axiom.client.render.rendersystem.VertexBufferBuilder;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static silversword.axiom.client.main.AxiomInitialize.mc;

public abstract class PostProcessShader {
    protected final RenderPipeline pipeline;
    private RenderTarget framebuffer;

    protected PostProcessShader(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public RenderTarget getFramebuffer() {
        if (framebuffer == null) {
            if (mc.getWindow() == null) {
                throw new IllegalStateException("Window not initialized yet");
            }
            framebuffer = new TextureTarget(
                    ProjectAxiom.MOD_ID + " PostProcessShader " + this.getClass().getSimpleName(),
                    mc.getWindow().getWidth(),
                    mc.getWindow().getHeight(),
                    true
            );
        }
        return framebuffer;
    }

    protected abstract boolean shouldDraw();
    protected void preDraw() {}
    protected void postDraw() {}
    protected abstract void setupPass(BufferRenderer renderer);

    public void clearTexture() {
        if (this.shouldDraw()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(getFramebuffer().getColorTexture(), 0);
        }
    }

    public void submitVertices(Runnable draw) {
        if (!shouldDraw()) return;
        preDraw();
        draw.run();
        postDraw();
    }

    public void render() {
        if (!shouldDraw()) return;

        var renderer = BufferRenderer.begin()
                .attachments(mc.getMainRenderTarget())
                .pipeline(pipeline)
                .uniform("PostData", UNIFORM_STORAGE.writeUniform(new UniformData(
                        (float) mc.getWindow().getWidth(),
                        (float) mc.getWindow().getHeight(),
                        (float) glfwGetTime()
                )))
                .sampler("u_Texture", getFramebuffer().getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

        setupPass(renderer);

        VertexBufferBuilder builder = new VertexBufferBuilder(pipeline);
        builder.begin();
        builder.vec2(-1, -1).next();
        builder.vec2( 1, -1).next();
        builder.vec2( 1,  1).next();
        builder.vec2(-1,  1).next();
        builder.quad(0, 1, 2, 3);
        builder.end();

        renderer.mesh(builder);
        renderer.end();
    }

    // LISÄTTY METODI:
    public void onResized(int width, int height) {
        if (framebuffer != null) {
            framebuffer.resize(width, height);
        }
    }

    // Uniforms
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec2()
            .putFloat()
            .get();

    private static final DynamicUniformStorage<UniformData> UNIFORM_STORAGE = new DynamicUniformStorage<>("Obsidian - Post UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        UNIFORM_STORAGE.endFrame();
    }

    private record UniformData(float sizeX, float sizeY, float time) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putVec2(sizeX, sizeY)
                    .putFloat(time);
        }
    }
}