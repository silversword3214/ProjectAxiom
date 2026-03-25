package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class RenderCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderCore.class);
    private final Map<RenderPipeline, Batch> batches = new HashMap<>();
    private final Map<Texture, Batch> textBatches = new HashMap<>();
    private final Map<RenderPipeline, VertexBufferManager> bufferManagers = new HashMap<>();
    private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private boolean pipelinesInitialized = false;

    private Matrix4f currentProjectionMatrix;
    private Matrix4f currentModelViewMatrix;

    public RenderCore() {

    }

    public void beginFrame(Matrix4f projection, Matrix4f modelView) {

        batches.clear();
        textBatches.clear();
        allocator.clear();
        this.currentProjectionMatrix = projection;
        this.currentModelViewMatrix = modelView;
    }

    public void flush() {
        for (Map.Entry<RenderPipeline, Batch> entry : batches.entrySet()) {
            drawBatch(entry.getKey(), entry.getValue());
            entry.getValue().clear();
        }
        batches.clear();

        for (Map.Entry<Texture, Batch> entry : textBatches.entrySet()) {
            drawTextBatch(entry.getKey(), entry.getValue());
            entry.getValue().clear();
        }
        textBatches.clear();
    }

    private VertexBufferManager getBufferManager(RenderPipeline pipeline) {
        return bufferManagers.computeIfAbsent(pipeline, k -> new VertexBufferManager());
    }

    // ----- Normal batch drawing (colored, textured) -----
    private void drawBatch(RenderPipeline pipeline, Batch batch) {
        if (pipeline == null) {
            LOGGER.error("Pipeline is null");
            return;
        }

        MeshData mesh = buildMeshFromBatch(batch);
        if (mesh == null) {
            LOGGER.warn("Mesh is null for batch (vertexCount={})", batch.vertexCount());
            return;
        }

        MeshData.DrawState drawParams = mesh.drawState();
        VertexFormat format = drawParams.format();
        int vertexBufferSize = drawParams.vertexCount() * format.getVertexSize();

        VertexBufferManager vbm = getBufferManager(pipeline);
        vbm.ensureCapacity(vertexBufferSize);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        vbm.upload(mesh.vertexBuffer(), vertexBufferSize, encoder);
        GpuBuffer vertices = vbm.getCurrentBuffer();

        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            mesh.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer());
            indexType = mesh.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = indexBuffer.getBuffer(drawParams.indexCount());
            indexType = indexBuffer.type();
        }

        Matrix4f mvp = new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                mvp,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Matrix4f()
        );

        GpuTextureView textureView = null;
        GpuSampler sampler = null;
        if (batch.getTexture() != null) {
            var textureManager = Minecraft.getInstance().getTextureManager();
            var abstractTexture = textureManager.getTexture(batch.getTexture());
            if (abstractTexture != null) {
                textureView = abstractTexture.getTextureView();
                sampler = abstractTexture.getSampler();
            } else {
                LOGGER.warn("Texture not found: {}", batch.getTexture());
            }
        }

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "axiomrenderapi_draw",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {

            if (textureView != null) {
                renderPass.bindTexture("u_Texture", textureView, sampler);
            }

            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, drawParams.indexCount(), 1);
        } catch (Exception e) {
            LOGGER.error("Error during render pass", e);
        }
        
        mesh.close();
        GpuFence fence = encoder.createFence();
        vbm.setFence(fence);
        vbm.rotate();
    }

    // ----- Text batch drawing -----
    private void drawTextBatch(Texture texture, Batch batch) {
        RenderPipeline pipeline = RenderPipelines.UI_TEXT;
        if (pipeline == null) {
            LOGGER.error("Text pipeline is null");
            return;
        }
        if (batch.vertexCount() == 0) return;

        MeshData mesh = buildMeshFromBatch(batch);
        if (mesh == null) return;

        MeshData.DrawState drawParams = mesh.drawState();
        VertexFormat format = drawParams.format();
        int vertexBufferSize = drawParams.vertexCount() * format.getVertexSize();

        VertexBufferManager vbm = getBufferManager(pipeline);
        vbm.ensureCapacity(vertexBufferSize);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        vbm.upload(mesh.vertexBuffer(), vertexBufferSize, encoder);
        GpuBuffer vertices = vbm.getCurrentBuffer();

        RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
        GpuBuffer indices = indexBuffer.getBuffer(drawParams.indexCount());
        VertexFormat.IndexType indexType = indexBuffer.type();

        Matrix4f mvp = new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                mvp,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Matrix4f()
        );

        GpuTextureView textureView = texture.textureView();
        GpuSampler sampler = texture.sampler();

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "axiomrenderapi_text",
                Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {

            if (textureView != null) {
                renderPass.bindTexture("u_Texture", textureView, sampler);
            }

            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, drawParams.indexCount(), 1);
        } catch (Exception e) {
            LOGGER.error("Error during text render pass", e);
        }

        mesh.close();

        GpuFence fence = encoder.createFence();
        vbm.setFence(fence);
        vbm.rotate();
    }

    private MeshData buildMeshFromBatch(Batch batch) {
        if (batch.vertexCount() == 0) return null;
        BufferBuilder builder = new BufferBuilder(allocator, batch.getMode(), batch.getFormat());
        for (float[] v : batch.getVertices()) {
            if (v.length == 9) { // POS2_UV_COLOR
                builder.addVertex(v[0], v[1], v[2])
                        .setUv(v[3], v[4])
                        .setColor(v[5], v[6], v[7], v[8]);
            } else { // POS3_COLOR, POS2_COLOR
                builder.addVertex(v[0], v[1], v[2])
                        .setColor(v[3], v[4], v[5], v[6]);
            }
        }
        return builder.buildOrThrow();
    }

    // --- 3D drawing methods ---
    public void addLine3D(double x1, double y1, double z1, double x2, double y2, double z2, float thickness, int color) {
        RenderPipeline pipeline = RenderPipelines.WORLD_COLORED_LINES;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline, k -> new Batch(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.DEBUG_LINES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex((float)x1, (float)y1, (float)z1, r, g, b, a);
        batch.vertex((float)x2, (float)y2, (float)z2, r, g, b, a);
    }

    public void addQuad(double x1, double y1, double z1,
                        double x2, double y2, double z2,
                        double x3, double y3, double z3,
                        double x4, double y4, double z4,
                        int color) {
        RenderPipeline pipeline = RenderPipelines.WORLD_COLORED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline, k -> new Batch(AxiomVertexFormats.POS3_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex((float)x1, (float)y1, (float)z1, r, g, b, a);
        batch.vertex((float)x2, (float)y2, (float)z2, r, g, b, a);
        batch.vertex((float)x3, (float)y3, (float)z3, r, g, b, a);
        batch.vertex((float)x1, (float)y1, (float)z1, r, g, b, a);
        batch.vertex((float)x3, (float)y3, (float)z3, r, g, b, a);
        batch.vertex((float)x4, (float)y4, (float)z4, r, g, b, a);
    }

    // --- 2D drawing methods ---
    public void addRect2D(float x, float y, float width, float height, int color) {
        RenderPipeline pipeline = RenderPipelines.UI_COLORED;
        if (pipeline == null) {
            LOGGER.error("uiColoredPipeline is null!");
            return;
        }
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        addQuad2D(batch, x, y, width, height, r, g, b, a);
    }

    public void addRectOutline2D(float x, float y, float width, float height, float thickness, int color) {
        if (thickness <= 0) return;
        RenderPipeline pipeline = RenderPipelines.UI_COLORED_LINES;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.DEBUG_LINES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        float x2 = x + width;
        float y2 = y + height;
        batch.vertex2D(x, y, r, g, b, a);
        batch.vertex2D(x2, y, r, g, b, a);
        batch.vertex2D(x, y2, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
        batch.vertex2D(x, y, r, g, b, a);
        batch.vertex2D(x, y2, r, g, b, a);
        batch.vertex2D(x2, y, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
    }

    public void addLine2D(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0) return;
        RenderPipeline pipeline = RenderPipelines.UI_COLORED_LINES;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.DEBUG_LINES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex2D(x1, y1, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
    }

    // --- Circle ---
    public void addCircle(float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        RenderPipeline pipeline = RenderPipelines.UI_COLORED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        double circumference = 2 * Math.PI * radius;
        int segments = Math.max(12, (int) (circumference / 0.5));
        double angleStep = 2 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
        }
    }

    public void addCircleOutline(float cx, float cy, float radius, float thickness, int color) {
        if (radius <= 0 || thickness <= 0) return;
        RenderPipeline pipeline = RenderPipelines.UI_COLORED_LINES;
        if (pipeline == null) return;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        double circumference = 2 * Math.PI * radius;
        int segments = Math.max(24, (int) (circumference / 0.5));
        double angleStep = 2 * Math.PI / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addLineThick((float) x1, (float) y1, (float) x2, (float) y2, thickness, color);
        }
    }

    // --- Rounded Rect ---
    public void addRoundedRect(float x, float y, float w, float h, float radius, int color) {
        if (radius <= 0.2f) {
            addRect2D(x, y, w, h, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        RenderPipeline pipeline = RenderPipelines.UI_COLORED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        // Yläreuna (suorakulmio)
        if (radius * 2 < w) {
            addQuad2D(batch, x + radius, y, w - 2 * radius, radius, r, g, b, a);
        }
        // Alareuna
        if (radius * 2 < w) {
            addQuad2D(batch, x + radius, y + h - radius, w - 2 * radius, radius, r, g, b, a);
        }
        // Keskiosa
        if (radius * 2 < h) {
            addQuad2D(batch, x, y + radius, w, h - 2 * radius, r, g, b, a);
        }

        double arcLength = (Math.PI * radius) / 2;
        int segments = Math.max(6, (int) (arcLength / 0.5));
        double angleStep = Math.PI / 2 / segments;

        // Vasen yläkulma
        float cx = x + radius;
        float cy = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI + i * angleStep;
            double angle2 = Math.PI + (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
        }

        // Oikea yläkulma
        cx = x + w - radius;
        cy = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI * 1.5 + i * angleStep;
            double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
        }

        // Oikea alakulma
        cx = x + w - radius;
        cy = y + h - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
        }

        // Vasen alakulma
        cx = x + radius;
        cy = y + h - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI / 2 + i * angleStep;
            double angle2 = Math.PI / 2 + (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
        }
    }

    public void addRoundedRectCustom(float x, float y, float w, float h, float radius, int color,
                                     boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0.5f || (!topLeft && !topRight && !bottomRight && !bottomLeft)) {
            addRect2D(x, y, w, h, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        RenderPipeline pipeline = RenderPipelines.UI_COLORED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float rTop = (topLeft || topRight) ? radius : 0;
        float rBottom = (bottomLeft || bottomRight) ? radius : 0;
        float rLeft = (topLeft || bottomLeft) ? radius : 0;
        float rRight = (topRight || bottomRight) ? radius : 0;

        float centerX = x + rLeft;
        float centerY = y + rTop;
        float centerW = w - rLeft - rRight;
        float centerH = h - rTop - rBottom;

        if (centerW > 0 && centerH > 0) {
            addQuad2D(batch, centerX, centerY, centerW, centerH, r, g, b, a);
        }

        if (rTop > 0 && centerW > 0) {
            addQuad2D(batch, centerX, y, centerW, rTop, r, g, b, a);
        }
        if (rBottom > 0 && centerW > 0) {
            addQuad2D(batch, centerX, y + h - rBottom, centerW, rBottom, r, g, b, a);
        }
        if (rLeft > 0 && centerH > 0) {
            addQuad2D(batch, x, centerY, rLeft, centerH, r, g, b, a);
        }
        if (rRight > 0 && centerH > 0) {
            addQuad2D(batch, x + w - rRight, centerY, rRight, centerH, r, g, b, a);
        }

        // Suorat kulmat (jos ei pyöristetty)
        if (!topLeft) {
            addQuad2D(batch, x, y, rLeft > 0 ? rLeft : radius, rTop > 0 ? rTop : radius, r, g, b, a);
        }
        if (!topRight) {
            addQuad2D(batch, x + w - (rRight > 0 ? rRight : radius), y, rRight > 0 ? rRight : radius, rTop > 0 ? rTop : radius, r, g, b, a);
        }
        if (!bottomRight) {
            addQuad2D(batch, x + w - (rRight > 0 ? rRight : radius), y + h - (rBottom > 0 ? rBottom : radius), rRight > 0 ? rRight : radius, rBottom > 0 ? rBottom : radius, r, g, b, a);
        }
        if (!bottomLeft) {
            addQuad2D(batch, x, y + h - (rBottom > 0 ? rBottom : radius), rLeft > 0 ? rLeft : radius, rBottom > 0 ? rBottom : radius, r, g, b, a);
        }

        // Kaaret (pyöristetyt kulmat)
        double arcLength = (Math.PI * radius) / 2;
        int segments = Math.max(6, (int) (arcLength / 0.5));
        double angleStep = Math.PI / 2 / segments;

        if (topLeft) {
            float cx = x + radius;
            float cy = y + radius;
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI + i * angleStep;
                double angle2 = Math.PI + (i + 1) * angleStep;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
            }
        }
        if (topRight) {
            float cx = x + w - radius;
            float cy = y + radius;
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI * 1.5 + i * angleStep;
                double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
            }
        }
        if (bottomRight) {
            float cx = x + w - radius;
            float cy = y + h - radius;
            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
            }
        }
        if (bottomLeft) {
            float cx = x + radius;
            float cy = y + h - radius;
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI / 2 + i * angleStep;
                double angle2 = Math.PI / 2 + (i + 1) * angleStep;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                addTriangle2D(batch, cx, cy, (float) x1, (float) y1, (float) x2, (float) y2, r, g, b, a);
            }
        }
    }

    public void addRoundedRectOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        if (radius <= 0.5f || thickness <= 0) {
            addRectOutline2D(x, y, w, h, thickness, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        RenderPipeline pipeline = RenderPipelines.UI_COLORED_LINES;
        if (pipeline == null) return;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        double arcLength = (Math.PI * radius) / 2;
        int segments = Math.max(8, (int) (arcLength / 0.5));
        double angleStep = Math.PI / 2 / segments;

        // Vasen yläkulma
        float cx1 = x + radius;
        float cy1 = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI + i * angleStep;
            double angle2 = Math.PI + (i + 1) * angleStep;
            double x1 = cx1 + radius * Math.cos(angle1);
            double y1 = cy1 + radius * Math.sin(angle1);
            double x2 = cx1 + radius * Math.cos(angle2);
            double y2 = cy1 + radius * Math.sin(angle2);
            addLineThick((float) x1, (float) y1, (float) x2, (float) y2, thickness, color);
        }

        // Oikea yläkulma
        float cx2 = x + w - radius;
        float cy2 = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI * 1.5 + i * angleStep;
            double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
            double x1 = cx2 + radius * Math.cos(angle1);
            double y1 = cy2 + radius * Math.sin(angle1);
            double x2 = cx2 + radius * Math.cos(angle2);
            double y2 = cy2 + radius * Math.sin(angle2);
            addLineThick((float) x1, (float) y1, (float) x2, (float) y2, thickness, color);
        }

        // Oikea alakulma
        float cx3 = x + w - radius;
        float cy3 = y + h - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx3 + radius * Math.cos(angle1);
            double y1 = cy3 + radius * Math.sin(angle1);
            double x2 = cx3 + radius * Math.cos(angle2);
            double y2 = cy3 + radius * Math.sin(angle2);
            addLineThick((float) x1, (float) y1, (float) x2, (float) y2, thickness, color);
        }

        // Vasen alakulma
        float cx4 = x + radius;
        float cy4 = y + h - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI / 2 + i * angleStep;
            double angle2 = Math.PI / 2 + (i + 1) * angleStep;
            double x1 = cx4 + radius * Math.cos(angle1);
            double y1 = cy4 + radius * Math.sin(angle1);
            double x2 = cx4 + radius * Math.cos(angle2);
            double y2 = cy4 + radius * Math.sin(angle2);
            addLineThick((float) x1, (float) y1, (float) x2, (float) y2, thickness, color);
        }

        // Suorat osat
        addLineThick(x + radius, y, x + w - radius, y, thickness, color);
        addLineThick(x + w, y + radius, x + w, y + h - radius, thickness, color);
        addLineThick(x + w - radius, y + h, x + radius, y + h, thickness, color);
        addLineThick(x, y + h - radius, x, y + radius, thickness, color);
    }

    // --- Helper methods for 2D batching ---
    private void addQuad2D(Batch batch, float x, float y, float w, float h, float r, float g, float b, float a) {
        float x2 = x + w;
        float y2 = y + h;
        batch.vertex2D(x, y, r, g, b, a);
        batch.vertex2D(x2, y, r, g, b, a);
        batch.vertex2D(x, y2, r, g, b, a);
        batch.vertex2D(x, y2, r, g, b, a);
        batch.vertex2D(x2, y, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
    }

    private void addTriangle2D(Batch batch, float x1, float y1, float x2, float y2, float x3, float y3,
                               float r, float g, float b, float a) {
        batch.vertex2D(x1, y1, r, g, b, a);
        batch.vertex2D(x2, y2, r, g, b, a);
        batch.vertex2D(x3, y3, r, g, b, a);
    }

    private void addLineThick(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0) return;
        RenderPipeline pipeline = RenderPipelines.UI_COLORED;
        if (pipeline == null) return;

        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_COLOR, VertexFormat.Mode.TRIANGLES));
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6f) return;
        float nx = dy / len;
        float ny = -dx / len;
        float halfThick = thickness / 2f;
        float ox = nx * halfThick;
        float oy = ny * halfThick;

        float v1x = x1 - ox;
        float v1y = y1 - oy;
        float v2x = x1 + ox;
        float v2y = y1 + oy;
        float v3x = x2 + ox;
        float v3y = y2 + oy;
        float v4x = x2 - ox;
        float v4y = y2 - oy;

        addTriangle2D(batch, v1x, v1y, v2x, v2y, v3x, v3y, r, g, b, a);
        addTriangle2D(batch, v1x, v1y, v3x, v3y, v4x, v4y, r, g, b, a);
    }

    // --- Texture drawing ---
    public void addTexture(Identifier texture, float x, float y, float width, float height, int color) {
        addTexturePart(texture, x, y, width, height, 0, 0, 1, 1, color);
    }

    public void addTexturePart(Identifier texture, float x, float y, float width, float height,
                               float u1, float v1, float u2, float v2, int color) {
        RenderPipeline pipeline = RenderPipelines.UI_TEXTURED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_UV_COLOR, VertexFormat.Mode.TRIANGLES));
        batch.setTexture(texture);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float x2 = x + width;
        float y2 = y + height;
        batch.vertexUV(x, y, u1, v1, r, g, b, a);
        batch.vertexUV(x2, y, u2, v1, r, g, b, a);
        batch.vertexUV(x, y2, u1, v2, r, g, b, a);
        batch.vertexUV(x, y2, u1, v2, r, g, b, a);
        batch.vertexUV(x2, y, u2, v1, r, g, b, a);
        batch.vertexUV(x2, y2, u2, v2, r, g, b, a);
    }

    public void addRotatedTexture(Identifier texture, float x, float y, float width, float height, float angleDeg, int color) {
        RenderPipeline pipeline = RenderPipelines.UI_TEXTURED;
        if (pipeline == null) return;
        Batch batch = batches.computeIfAbsent(pipeline,
                k -> new Batch(AxiomVertexFormats.POS2_UV_COLOR, VertexFormat.Mode.TRIANGLES));
        batch.setTexture(texture);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float cx = x + width / 2;
        float cy = y + height / 2;
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float halfW = width / 2;
        float halfH = height / 2;

        // Neljä nurkkaa (vasen ylä, oikea ylä, oikea ala, vasen ala)
        float[] xs = {-halfW, halfW, halfW, -halfW};
        float[] ys = {-halfH, -halfH, halfH, halfH};
        float[] u = {0, 1, 1, 0};
        float[] v = {0, 0, 1, 1};

        // Kierretään pisteet keskipisteen ympäri
        float[] xRot = new float[4];
        float[] yRot = new float[4];
        for (int i = 0; i < 4; i++) {
            xRot[i] = cx + xs[i] * cos - ys[i] * sin;
            yRot[i] = cy + xs[i] * sin + ys[i] * cos;
        }

        // Kaksi kolmiota: (0,1,2) ja (0,2,3)
        batch.vertexUV(xRot[0], yRot[0], u[0], v[0], r, g, b, a);
        batch.vertexUV(xRot[1], yRot[1], u[1], v[1], r, g, b, a);
        batch.vertexUV(xRot[2], yRot[2], u[2], v[2], r, g, b, a);

        batch.vertexUV(xRot[0], yRot[0], u[0], v[0], r, g, b, a);
        batch.vertexUV(xRot[2], yRot[2], u[2], v[2], r, g, b, a);
        batch.vertexUV(xRot[3], yRot[3], u[3], v[3], r, g, b, a);
    }

    public void addTextQuadMesh(Texture texture,
                                float x0, float y0, float x1, float y1,
                                float u0, float v0, float u1, float v1,
                                float r, float g, float b, float a) {
        RenderPipeline pipeline = RenderPipelines.UI_TEXT;
        if (pipeline == null) return;
        Batch batch = textBatches.computeIfAbsent(texture,
                k -> new Batch(AxiomVertexFormats.POS2_UV_COLOR, VertexFormat.Mode.TRIANGLES));

        // Kolmio 1
        batch.vertexUV(x0, y0, u0, v0, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);
        // Kolmio 2
        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x1, y1, u1, v1, r, g, b, a);
    }

    public void close() {
        if (allocator != null) allocator.close();
        for (VertexBufferManager vbm : bufferManagers.values()) {
            vbm.close();
        }
        bufferManagers.clear();
    }
}