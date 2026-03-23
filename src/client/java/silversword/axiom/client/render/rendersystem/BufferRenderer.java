package silversword.axiom.client.render.rendersystem;


import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.util.Tuple;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import java.util.HashMap;
import java.util.Map;


public class BufferRenderer {
    private static final BufferRenderer INSTANCE = new BufferRenderer();

    private static boolean taken;

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private RenderPipeline pipeline;
    private VertexBufferBuilder mesh;
    private Matrix4f matrix;
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private boolean rendering3D;
    private final HashMap<String, Tuple<GpuTextureView, GpuSampler>> samplers = new HashMap<>();
    private final Map<String, GpuBufferSlice> uniforms = new HashMap<>();

    private BufferRenderer() {}

    public static BufferRenderer begin() {
        if (taken) throw new IllegalStateException("MeshRenderer: Edellistä sessiota ei päätetty!");
        taken = true;
        return INSTANCE;
    }

    // MeshRenderer.java
    // Lisää tämä metodi muiden joukkoon.
    // Se ottaa vastaan Matrix4f-oliot ja muuttaa ne GpuBufferSliceksi.
    // MeshRenderer.java sisälle

    public BufferRenderer attachments(GpuTextureView color, GpuTextureView depth) {
        colorAttachment = color;
        depthAttachment = depth;
        return this;
    }

    public BufferRenderer attachments(RenderTarget framebuffer) {
        colorAttachment = framebuffer.getColorTextureView();
        depthAttachment = framebuffer.getDepthTextureView();
        return this;
    }

    public BufferRenderer mesh(VertexBufferBuilder mesh, com.mojang.blaze3d.vertex.PoseStack matrices) {
        this.mesh = mesh;
        this.matrix = matrices.last().pose();
        return this;
    }

    public BufferRenderer mesh(GpuBuffer vertices, GpuBuffer indices) {
        this.vertexBuffer = vertices;
        this.indexBuffer = indices;
        return this;
    }

    public BufferRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public BufferRenderer mesh(VertexBufferBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public BufferRenderer transform(Matrix4f matrix) {
        this.matrix = matrix;
        return this;
    }

    public BufferRenderer set3D(boolean rendering3D) {
        this.rendering3D = rendering3D;
        return this;
    }

    public BufferRenderer sampler(String name, GpuTextureView view, GpuSampler sampler) {
        if (name != null && view != null && sampler != null) {
            samplers.put(name, new Tuple<>(view, sampler));
        }
        return this;
    }



    public BufferRenderer uniform(String name, GpuBufferSlice data) {
        if (name != null && data != null) {
            uniforms.put(name, data);
        }
        return this;
    }




    public void end() {
        if (mesh != null && mesh.isBuilding()) mesh.end();
        int indexCount = mesh != null ? mesh.getIndicesCount()
                : (int) (indexBuffer != null ? indexBuffer.size() / Integer.BYTES : -1);

        if (indexCount > 0 && pipeline != null) {
            GpuBuffer vertexBuffer = mesh != null ? mesh.getVertexBuffer() : this.vertexBuffer;
            GpuBuffer indexBuffer = mesh != null ? mesh.getIndexBuffer() : this.indexBuffer;

            if (matrix != null) {
                RenderSystem.getModelViewStack().pushMatrix();
                RenderSystem.getModelViewStack().mul(matrix);
            }

            var meshData = BufferUniforms.write(
                    RenderUtils.projection,
                    RenderSystem.getModelViewStack()
            );

            var device = RenderSystem.getDevice();
            var encoder = device.createCommandEncoder();
            var fb = Minecraft.getInstance().getMainRenderTarget();

            var pass = encoder.createRenderPass(
                    () -> "Obsidian Mesh Render",
                    fb.getColorTextureView(), java.util.OptionalInt.empty(),
                    fb.getDepthTextureView(), java.util.OptionalDouble.empty()
            );
            pass.setPipeline(pipeline);
            pass.setUniform("MeshData", meshData);

            for (var entry : uniforms.entrySet()) {
                pass.setUniform(entry.getKey(), entry.getValue());
            }

            for (var entry : samplers.entrySet()) {
                var pair = entry.getValue();
                pass.bindTexture(entry.getKey(), pair.getA(), pair.getB());
            }

            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, com.mojang.blaze3d.vertex.VertexFormat.IndexType.INT);

            pass.drawIndexed(0, 0, indexCount, 1);
            pass.close();

            if (matrix != null) {
                RenderSystem.getModelViewStack().popMatrix();
            }

            if (indexCount > 0 && pipeline != null) {
                // ... piirtokoodi ...
            } else {
                System.out.println("[MeshRenderer] Indexes: 0 or Pipeline: null");
            }

        }

        colorAttachment = null;
        depthAttachment = null;
        pipeline = null;
        mesh = null;
        vertexBuffer = null;
        indexBuffer = null;
        matrix = null;
        samplers.clear();
        uniforms.clear();
        taken = false;
    }
}