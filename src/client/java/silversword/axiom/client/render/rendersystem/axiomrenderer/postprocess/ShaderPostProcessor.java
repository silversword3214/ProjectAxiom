package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import silversword.axiom.client.modules.render.ShaderESP;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderPipelines;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class ShaderPostProcessor implements AutoCloseable {
    private static final int OUTLINE_UNIFORM_SIZE = 48;

    private GpuBuffer outlineUniformBuffer;
    private GpuSampler linearSampler;

    public void run(RenderTarget maskTarget, RenderTarget postTarget, ShaderESP module) {
        ensureResources();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(postTarget.getColorTexture(), 0x00000000);
        uploadOutlineUniforms(encoder, postTarget.width, postTarget.height, module);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "shader_esp_outline",
                postTarget.getColorTextureView(),
                OptionalInt.empty()
        )) {
            renderPass.setPipeline(RenderPipelines.SHADER_OUTLINE);
            renderPass.bindTexture("u_Scene", maskTarget.getColorTextureView(), linearSampler);
            renderPass.setUniform("OutlineData", outlineUniformBuffer.slice());
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.draw(0, 3);
        }
    }

    public void composite(RenderTarget source, RenderTarget destination) {
        ensureResources();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "shader_esp_composite",
                destination.getColorTextureView(),
                OptionalInt.empty(),
                destination.getDepthTextureView(),
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(RenderPipelines.SHADER_COMPOSITE);
            renderPass.bindTexture("u_Scene", source.getColorTextureView(), linearSampler);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.draw(0, 3);
        }
    }

    private void ensureResources() {
        if (outlineUniformBuffer == null || outlineUniformBuffer.isClosed()) {
            outlineUniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "shader_esp_outline_uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    OUTLINE_UNIFORM_SIZE
            );
        }

        if (linearSampler == null) {
            linearSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR,
                    FilterMode.LINEAR,
                    1,
                    OptionalDouble.empty()
            );
        }
    }

    private void uploadOutlineUniforms(CommandEncoder encoder, int width, int height, ShaderESP module) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(OUTLINE_UNIFORM_SIZE).order(ByteOrder.nativeOrder());

        var outline = module.getOutlineColor().getCurrentColor();
        var fill = module.getFillColor().getCurrentColor();

        putColor(buffer, outline);
        putColor(buffer, fill);
        buffer.putFloat(width);
        buffer.putFloat(height);
        buffer.putFloat((float) module.getThickness().getValue());
        buffer.putFloat(module.isFillEnabled() ? 1.0f : 0.0f);
        buffer.flip();

        GpuBufferSlice slice = outlineUniformBuffer.slice();
        encoder.writeToBuffer(slice, buffer);
    }

    private static void putColor(ByteBuffer buffer, silversword.axiom.client.render.rendersystem.utils.color.Color color) {
        buffer.putFloat(color.r / 255.0f);
        buffer.putFloat(color.g / 255.0f);
        buffer.putFloat(color.b / 255.0f);
        buffer.putFloat(color.a / 255.0f);
    }

    @Override
    public void close() {
        if (outlineUniformBuffer != null && !outlineUniformBuffer.isClosed()) {
            outlineUniformBuffer.close();
            outlineUniformBuffer = null;
        }

        linearSampler = null;
    }
}
