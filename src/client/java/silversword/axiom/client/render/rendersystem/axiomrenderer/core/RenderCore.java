package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

// ─── RenderPearl API ───────────────────────────────────────────────
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

// ─── Blaze3D — Minecraft-sillat (pakollisia, ei RenderPearl-vastineita) ───
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;

// ─── Minecraft ─────────────────────────────────────────────────────
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

// ─── JOML ──────────────────────────────────────────────────────────
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// ─── SLF4J ─────────────────────────────────────────────────────────
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ─── Project ───────────────────────────────────────────────────────
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;

import java.util.*;

public class RenderCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderCore.class);

    private static final float AA_WIDTH = 0.5f;

    // Jokainen batch tallentaa oman MVP:nsä. Näin useampi beginFrame()
    // samassa framessa (esim. FabricHudHook + AxiomHudBootstrap) ei
    // korvaa aiempien bätsien projektiota.
    private final Map<CompiledRenderPipeline, Batch> batches = new HashMap<>();

    // Tekstuurikohtaiset batchit. Avain = tekstuurin Identifier, jotta
    // useampi eri tekstuuri ei ylikirjoita toistensa sidontaa flushissa.
    private final Map<Identifier, Batch> textureBatches = new HashMap<>();

    // Tekstibatchit (font atlas), avain = Texture-objekti.
    private final Map<Texture, Batch> textBatches = new HashMap<>();

    private final Map<CompiledRenderPipeline, VertexBufferManager> bufferManagers = new HashMap<>();
    private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private Matrix4f currentProjectionMatrix;
    private Matrix4f currentModelViewMatrix;

    private boolean scissorEnabled = false;
    private int scissorX, scissorY, scissorW, scissorH;

    private static RenderTarget targetOverride = null;
    public static void setTargetOverride(RenderTarget rt) { targetOverride = rt; }
    private final Map<GpuTextureView, CompiledRenderPipeline> gpuViewPipelines = new HashMap<>();
    private final Map<GpuTextureView, Batch> gpuViewBatches = new HashMap<>();
    private final Map<GpuTextureView, GpuSampler> gpuViewSamplers = new HashMap<>();

    private final Map<Identifier, CompiledRenderPipeline> texturePipelines = new HashMap<>();

    // Scissor
    private final List<CommittedGroup> committedGroups = new ArrayList<>();

    private static final class CommittedGroup {
        final Map<CompiledRenderPipeline, Batch> batches;
        final Map<Identifier, Batch> textureBatches;
        final Map<Texture, Batch> textBatches;
        final Map<GpuTextureView, Batch> gpuViewBatches;
        final Map<GpuTextureView, GpuSampler> gpuViewSamplers;
        final Map<GpuTextureView, CompiledRenderPipeline> gpuViewPipelines;
        final Map<Identifier, CompiledRenderPipeline> texturePipelines;

        final boolean scissorEnabled;
        final int sx, sy, sw, sh;

        CommittedGroup(Map<CompiledRenderPipeline, Batch> b,
                       Map<Identifier, Batch> tb,
                       Map<Texture, Batch> txb,
                       Map<GpuTextureView, Batch> gvb,
                       Map<GpuTextureView, GpuSampler> gvs,
                       Map<GpuTextureView, CompiledRenderPipeline> gvp,
                       Map<Identifier, CompiledRenderPipeline> tp,
                       boolean se, int sx, int sy, int sw, int sh) {
            this.batches = b;
            this.textureBatches = tb;
            this.textBatches = txb;
            this.gpuViewBatches = gvb;
            this.gpuViewSamplers = gvs;
            this.gpuViewPipelines = gvp;
            this.texturePipelines = tp;
            this.scissorEnabled = se;
            this.sx = sx; this.sy = sy; this.sw = sw; this.sh = sh;
        }
    }


    public RenderCore() {}

    // ================================================================
    //  Pipeline-apurit
    // ================================================================

    private static CompiledRenderPipeline uiColored() {
        CompiledRenderPipeline p = RenderPipelines.UI_COLORED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_COLORED; }
        return p;
    }

    private static CompiledRenderPipeline uiColoredLines() {
        CompiledRenderPipeline p = RenderPipelines.UI_COLORED_LINES;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_COLORED_LINES; }
        return p;
    }

    private static CompiledRenderPipeline uiTextured() {
        CompiledRenderPipeline p = RenderPipelines.UI_TEXTURED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_TEXTURED; }
        return p;
    }

    private static CompiledRenderPipeline uiText() {
        CompiledRenderPipeline p = RenderPipelines.UI_TEXT;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_TEXT; }
        return p;
    }

    private static CompiledRenderPipeline worldColored() {
        CompiledRenderPipeline p = RenderPipelines.WORLD_COLORED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.WORLD_COLORED; }
        return p;
    }

    private static CompiledRenderPipeline worldColoredLines() {
        CompiledRenderPipeline p = RenderPipelines.WORLD_COLORED_LINES;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.WORLD_COLORED_LINES; }
        return p;
    }

    // ================================================================
    //  Batch-hallinta — tallenna MVP luontihetkellä
    // ================================================================

    /**
     * Hakee tai luo batchin ja tallentaa sille tämänhetkisen MVP:n.
     * Kriittinen: batch muistaa projektion, jolla sen verteksit on
     * tuotettu. Myöhempi beginFrame() ei enää vaikuta siihen.
     */
    private Batch getOrCreateBatch(CompiledRenderPipeline pipeline,
                                   VertexFormat format,
                                   PrimitiveTopology mode) {
        return batches.computeIfAbsent(pipeline, k -> {
            Batch b = new Batch(format, mode);
            b.setMvp(currentProjectionMatrix, currentModelViewMatrix);
            return b;
        });
    }

    /**
     * Tekstuurikohtainen batch. Avain = Identifier, jotta eri tekstuurit
     * eivät ylikirjoita toistensa sidontaa flushissa.
     */
    private Batch getOrCreateTextureBatch(Identifier texture) {
        return textureBatches.computeIfAbsent(texture, k -> {
            Batch b = new Batch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
            b.setTexture(k);
            b.setMvp(currentProjectionMatrix, currentModelViewMatrix);
            return b;
        });
    }

    private Batch getOrCreateTextBatch(Texture texture) {
        return textBatches.computeIfAbsent(texture, k -> {
            Batch b = new Batch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
            b.setMvp(currentProjectionMatrix, currentModelViewMatrix);
            return b;
        });
    }

    // ================================================================
    //  Elinkaari
    // ================================================================

    /**
     * EI enää tyhjennä bätsejä. Pelkästään päivittää globaalin projektion.
     * Tyhjennys tapahtuu flushissa, jotta 3D-bätsit eivät häviä kun
     * HUD-layer vaihtaa projektion orthoksi.
     */
    public void beginFrame(Matrix4f projection, Matrix4f modelView) {
        this.currentProjectionMatrix = projection;
        this.currentModelViewMatrix = modelView;
    }

    public void flush() {
        commitCurrentGroup();

        try {
            for (CommittedGroup g : committedGroups) {
                drawCommittedGroup(g);
            }
        } catch (Exception e) {
            throw new RuntimeException("Flush failed", e);
        } finally {
            committedGroups.clear();
            allocator.clear();
        }
    }

    private void drawCommittedGroup(CommittedGroup g) {
        boolean savedSE = scissorEnabled;
        int savedSx = scissorX, savedSy = scissorY;
        int savedSw = scissorW, savedSh = scissorH;

        scissorEnabled = g.scissorEnabled;
        scissorX = g.sx; scissorY = g.sy;
        scissorW = g.sw; scissorH = g.sh;

        try {
            // 1. Väribatchit
            for (Map.Entry<CompiledRenderPipeline, Batch> e : g.batches.entrySet()) {
                drawBatch(e.getKey(), e.getValue());
                e.getValue().clear();
            }

            // 2. Tekstuurikohtaiset batchit
            CompiledRenderPipeline textured = uiTextured();
            for (Map.Entry<Identifier, Batch> e : g.textureBatches.entrySet()) {
                CompiledRenderPipeline pipe = g.texturePipelines.getOrDefault(e.getKey(), textured);
                drawBatch(pipe, e.getValue());
                e.getValue().clear();
            }

            // 3. GPU-tekstuurit
            for (Map.Entry<GpuTextureView, Batch> e : g.gpuViewBatches.entrySet()) {
                GpuTextureView view = e.getKey();
                Batch batch = e.getValue();
                GpuSampler sampler = g.gpuViewSamplers.get(view);
                CompiledRenderPipeline pipeline = g.gpuViewPipelines.getOrDefault(view, textured);
                batch.setSampler(sampler);
                drawBatchWithView(pipeline, view, batch);
                batch.clear();
            }

            // 4. Font atlas
            for (Map.Entry<Texture, Batch> e : g.textBatches.entrySet()) {
                drawTextBatch(e.getKey(), e.getValue());
                e.getValue().clear();
            }
        } finally {
            scissorEnabled = savedSE;
            scissorX = savedSx; scissorY = savedSy;
            scissorW = savedSw; scissorH = savedSh;
        }
    }

    private VertexBufferManager getBufferManager(CompiledRenderPipeline pipeline) {
        return bufferManagers.computeIfAbsent(pipeline, k -> new VertexBufferManager());
    }

    public void enableScissor(int x, int y, int w, int h) {
        if (!scissorEnabled || scissorX != x || scissorY != y
                || scissorW != w || scissorH != h) {
            commitCurrentGroup();
        }
        this.scissorEnabled = true;
        this.scissorX = x;
        this.scissorY = y;
        this.scissorW = w;
        this.scissorH = h;
    }

    public void disableScissor() {
        if (scissorEnabled) {
            commitCurrentGroup();
        }
        this.scissorEnabled = false;
    }

    private void commitCurrentGroup() {
        if (batches.isEmpty() && textureBatches.isEmpty()
                && textBatches.isEmpty() && gpuViewBatches.isEmpty()) {
            return;
        }

        committedGroups.add(new CommittedGroup(
                new HashMap<>(batches),
                new HashMap<>(textureBatches),
                new HashMap<>(textBatches),
                new HashMap<>(gpuViewBatches),
                new HashMap<>(gpuViewSamplers),
                new HashMap<>(gpuViewPipelines),
                new HashMap<>(texturePipelines),
                scissorEnabled, scissorX, scissorY, scissorW, scissorH
        ));

        batches.clear();
        textureBatches.clear();
        textBatches.clear();
        gpuViewBatches.clear();
        gpuViewSamplers.clear();
        gpuViewPipelines.clear();
        texturePipelines.clear();
    }

    private static RenderTarget getRenderTarget() {
        if (targetOverride != null) return targetOverride;
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    // ================================================================
    //  Batch-piirto
    // ================================================================

    private void drawBatch(CompiledRenderPipeline pipeline, Batch batch) {
        if (pipeline == null) return;

        MeshData mesh = buildMeshFromBatch(batch);
        if (mesh == null) return;

        MeshData.DrawState drawParams = mesh.drawState();
        VertexFormat format = drawParams.format();
        int vertexBufferSize = drawParams.vertexCount() * format.getVertexSize();

        VertexBufferManager vbm = getBufferManager(pipeline);
        vbm.ensureCapacity(vertexBufferSize);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        vbm.upload(mesh.vertexBuffer(), vertexBufferSize, encoder);
        GpuBufferSlice vertices = vbm.getCurrentBuffer().slice(0, vertexBufferSize);

        RenderSystem.AutoStorageIndexBuffer indexBuffer =
                RenderSystem.getSequentialBuffer(batch.getMode());
        GpuBuffer indices = indexBuffer.getBuffer(drawParams.indexCount());
        IndexType indexType = indexBuffer.type();

        Matrix4f mvp = batch.getMvp() != null
                ? batch.getMvp()
                : new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                mvp,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Matrix4f()
        );

        GpuTextureView textureView = null;
        GpuSampler sampler = null;
        if (batch.getTexture() != null) {
            var abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(batch.getTexture());
            if (abstractTexture != null) {
                textureView = abstractTexture.getTextureView();
                sampler = abstractTexture.getSampler();
            }
        }

        var rt = getRenderTarget();

        try {
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "axiomrenderapi_draw",
                    rt.getColorTextureView(),
                    Optional.empty(),
                    rt.getDepthTextureView(),
                    OptionalDouble.empty())) {

                renderPass.setPipeline(pipeline);

                if (textureView != null) {
                    renderPass.setUniform("u_Texture", textureView, sampler);
                }
                if (applyScissor(renderPass)) {
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                    renderPass.setVertexBuffer(0, vertices);
                    renderPass.setIndexBuffer(indices, indexType);
                    renderPass.drawIndexed(drawParams.indexCount(), 1, 0, 0, 0);
                }
            } catch (Exception e) {
                LOGGER.error("Error during render pass", e);
            }

            GpuFence fence = encoder.createFence();
            vbm.setFence(fence);
        } finally {
            encoder.submit();
        }

        mesh.close();
        vbm.rotate();
    }

    private boolean applyScissor(RenderPass renderPass) {
        if (!scissorEnabled) {
            renderPass.disableScissor();
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        int targetWidth = renderTarget.width;
        int targetHeight = renderTarget.height;

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        if (targetWidth <= 0 || targetHeight <= 0 || scaledWidth <= 0 || scaledHeight <= 0) {
            renderPass.disableScissor();
            return true;
        }

        float scaleX = (float) targetWidth / scaledWidth;
        float scaleY = (float) targetHeight / scaledHeight;

        float guiMinX = scissorX;
        float guiMaxX = scissorX + scissorW;
        float guiMinY = scissorY;
        float guiMaxY = scissorY + scissorH;

        int fbMinX = Math.round(guiMinX * scaleX);
        int fbMaxX = Math.round(guiMaxX * scaleX);
        int fbMinY = targetHeight - Math.round(guiMaxY * scaleY);
        int fbMaxY = targetHeight - Math.round(guiMinY * scaleY);

        int clampedMinX = Math.max(0, Math.min(fbMinX, targetWidth));
        int clampedMaxX = Math.max(0, Math.min(fbMaxX, targetWidth));
        int clampedMinY = Math.max(0, Math.min(fbMinY, targetHeight));
        int clampedMaxY = Math.max(0, Math.min(fbMaxY, targetHeight));

        int glX = clampedMinX;
        int glY = clampedMinY;
        int glW = clampedMaxX - clampedMinX;
        int glH = clampedMaxY - clampedMinY;

        if (glW <= 0 || glH <= 0) return false;

        renderPass.enableScissor(glX, glY, glW, glH);
        return true;
    }

    // ================================================================
    //  Tekstibatch-piirto
    // ================================================================

    private void drawTextBatch(Texture texture, Batch batch) {
        CompiledRenderPipeline pipeline = uiText();
        if (pipeline == null) {
            LOGGER.error("Text pipeline is null even after rebuild");
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
        GpuBufferSlice vertices = vbm.getCurrentBuffer().slice(0, vertexBufferSize);

        RenderSystem.AutoStorageIndexBuffer indexBuffer =
                RenderSystem.getSequentialBuffer(batch.getMode());
        GpuBuffer indices = indexBuffer.getBuffer(drawParams.indexCount());
        IndexType indexType = indexBuffer.type();

        Matrix4f mvp = batch.getMvp() != null
                ? batch.getMvp()
                : new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                mvp,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Matrix4f()
        );

        GpuTextureView textureView = texture.textureView();
        GpuSampler sampler = texture.sampler();

        var rt = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        try {
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "axiomrenderapi_text",
                    rt.getColorTextureView(),
                    Optional.empty(),
                    rt.getDepthTextureView(),
                    OptionalDouble.empty())) {

                renderPass.setPipeline(pipeline);

                if (textureView != null) {
                    renderPass.setUniform("u_Texture", textureView, sampler);
                }
                if (applyScissor(renderPass)) {
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                    renderPass.setVertexBuffer(0, vertices);
                    renderPass.setIndexBuffer(indices, indexType);
                    renderPass.drawIndexed(drawParams.indexCount(), 1, 0, 0, 0);
                }
            } catch (Exception e) {
                LOGGER.error("Error during text render pass", e);
            }

            GpuFence fence = encoder.createFence();
            vbm.setFence(fence);
        } finally {
            encoder.submit();
        }

        mesh.close();
        vbm.rotate();
    }

    private void drawBatchWithView(CompiledRenderPipeline pipeline,
                                   GpuTextureView view, Batch batch) {
        if (pipeline == null) {
            LOGGER.error("drawBatchWithView: pipeline is null, view={}", view);
            return;
        }
        if (batch.vertexCount() == 0) {
            LOGGER.warn("drawBatchWithView: empty batch");
            return;
        }

        MeshData mesh = buildMeshFromBatch(batch);
        if (mesh == null) return;

        MeshData.DrawState drawParams = mesh.drawState();
        int vertexBufferSize = drawParams.vertexCount()
                * drawParams.format().getVertexSize();

        VertexBufferManager vbm = getBufferManager(pipeline);
        vbm.ensureCapacity(vertexBufferSize);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        vbm.upload(mesh.vertexBuffer(), vertexBufferSize, encoder);
        GpuBufferSlice vertices = vbm.getCurrentBuffer().slice(0, vertexBufferSize);

        RenderSystem.AutoStorageIndexBuffer ib =
                RenderSystem.getSequentialBuffer(batch.getMode());
        GpuBuffer indices = ib.getBuffer(drawParams.indexCount());
        IndexType indexType = ib.type();

        Matrix4f mvp = batch.getMvp() != null
                ? batch.getMvp()
                : new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);

        GpuBufferSlice dynamicTransforms =
                RenderSystem.getDynamicUniforms().writeTransform(
                        mvp,
                        new Vector4f(1, 1, 1, 1),
                        new Vector3f(0, 0, 0),
                        new Matrix4f());

        var rt = getRenderTarget();

        try {
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "axiom_gpu_texture",
                    rt.getColorTextureView(),
                    Optional.empty(),
                    rt.getDepthTextureView(),
                    OptionalDouble.empty())) {

                renderPass.setPipeline(pipeline);

                renderPass.setUniform("u_Texture", view, batch.getSampler());
                if (applyScissor(renderPass)) {
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                    renderPass.setVertexBuffer(0, vertices);
                    renderPass.setIndexBuffer(indices, indexType);
                    renderPass.drawIndexed(drawParams.indexCount(), 1, 0, 0, 0);
                }
            }
            GpuFence fence = encoder.createFence();
            vbm.setFence(fence);
        } catch (Exception e) {
            LOGGER.error("drawBatchWithView failed", e);
        } finally {
            encoder.submit();
        }

        mesh.close();
        vbm.rotate();
    }


    private MeshData buildMeshFromBatch(Batch batch) {
        if (batch.vertexCount() == 0) return null;
        BufferBuilder builder = new BufferBuilder(allocator, batch.getMode(), batch.getFormat());
        for (float[] v : batch.getVertices()) {
            if (v.length == 9) {
                builder.addVertex(v[0], v[1], v[2])
                        .setUv(v[3], v[4])
                        .setColor(v[5], v[6], v[7], v[8]);
            } else {
                builder.addVertex(v[0], v[1], v[2])
                        .setColor(v[3], v[4], v[5], v[6]);
            }
        }
        return builder.buildOrThrow();
    }

    // ================================================================
    //  AA-apufunktiot
    // ================================================================

    private static int segsForRadius(float radius) {
        if (radius <= 0.5f) return 8;
        int segs = (int) Math.ceil(Math.PI * radius);
        return Math.max(16, Math.min(segs, 256));
    }

    private static int arcSegsFor(float radius) {
        if (radius <= 0.5f) return 2;
        return Math.max(4, Math.min(32, (int) Math.ceil(radius)));
    }

    private static void addArcPoints(List<float[]> pts, float cx, float cy, float radius,
                                     float startAng, float endAng, float sign, int segs) {
        if (radius <= 0.001f) {
            float mid = (startAng + endAng) * 0.5f;
            float nx = (float) Math.cos(mid), ny = (float) Math.sin(mid);
            pts.add(new float[]{cx, cy, nx * sign, ny * sign});
            return;
        }
        for (int i = 1; i <= segs; i++) {
            float ang = startAng + (endAng - startAng) * i / segs;
            float nx = (float) Math.cos(ang), ny = (float) Math.sin(ang);
            pts.add(new float[]{cx + nx * radius, cy + ny * radius, nx * sign, ny * sign});
        }
    }

    private static List<float[]> buildRoundedRectBoundary(
            float x, float y, float w, float h,
            float rTL, float rTR, float rBR, float rBL,
            float sign, int arcSegs) {

        List<float[]> pts = new ArrayList<>();
        float x2 = x + w, y2 = y + h;
        float maxR = Math.min(w, h) * 0.5f;
        rTL = Math.max(0, Math.min(rTL, maxR));
        rTR = Math.max(0, Math.min(rTR, maxR));
        rBR = Math.max(0, Math.min(rBR, maxR));
        rBL = Math.max(0, Math.min(rBL, maxR));

        pts.add(new float[]{x + rTL, y, 0f, -sign});
        pts.add(new float[]{x2 - rTR, y, 0f, -sign});
        addArcPoints(pts, x2 - rTR, y + rTR, rTR, (float) (-Math.PI / 2), 0f, sign, arcSegs);
        pts.add(new float[]{x2, y + rTR, sign, 0f});
        pts.add(new float[]{x2, y2 - rBR, sign, 0f});
        addArcPoints(pts, x2 - rBR, y2 - rBR, rBR, 0f, (float) (Math.PI / 2), sign, arcSegs);
        pts.add(new float[]{x2 - rBR, y2, 0f, sign});
        pts.add(new float[]{x + rBL, y2, 0f, sign});
        addArcPoints(pts, x + rBL, y2 - rBL, rBL, (float) (Math.PI / 2), (float) Math.PI, sign, arcSegs);
        pts.add(new float[]{x, y2 - rBL, -sign, 0f});
        pts.add(new float[]{x, y + rTL, -sign, 0f});
        addArcPoints(pts, x + rTL, y + rTL, rTL, (float) Math.PI, (float) (3 * Math.PI / 2), sign, arcSegs);

        return pts;
    }

    private static List<float[]> buildCircleBoundary(float cx, float cy, float radius,
                                                     float sign, int segs) {
        List<float[]> pts = new ArrayList<>(segs);
        for (int i = 0; i < segs; i++) {
            float ang = (float) (2 * Math.PI * i / segs);
            float nx = (float) Math.cos(ang), ny = (float) Math.sin(ang);
            pts.add(new float[]{cx + nx * radius, cy + ny * radius, nx * sign, ny * sign});
        }
        return pts;
    }

    private static void emitFilledShape(Batch batch, List<float[]> b, float cx, float cy,
                                        float r, float g, float bl, float a) {
        int n = b.size();
        if (n < 3) return;
        float half = AA_WIDTH * 0.5f;

        for (int i = 0; i < n; i++) {
            float[] p1 = b.get(i), p2 = b.get((i + 1) % n);
            float in1x = p1[0] - p1[2] * half, in1y = p1[1] - p1[3] * half;
            float in2x = p2[0] - p2[2] * half, in2y = p2[1] - p2[3] * half;
            batch.vertex2D(cx, cy, r, g, bl, a);
            batch.vertex2D(in1x, in1y, r, g, bl, a);
            batch.vertex2D(in2x, in2y, r, g, bl, a);
        }

        for (int i = 0; i < n; i++) {
            float[] p1 = b.get(i), p2 = b.get((i + 1) % n);
            float in1x = p1[0] - p1[2] * half, in1y = p1[1] - p1[3] * half;
            float in2x = p2[0] - p2[2] * half, in2y = p2[1] - p2[3] * half;
            float out1x = p1[0] + p1[2] * half, out1y = p1[1] + p1[3] * half;
            float out2x = p2[0] + p2[2] * half, out2y = p2[1] + p2[3] * half;

            batch.vertex2D(in1x, in1y, r, g, bl, a);
            batch.vertex2D(out1x, out1y, r, g, bl, 0f);
            batch.vertex2D(in2x, in2y, r, g, bl, a);

            batch.vertex2D(in2x, in2y, r, g, bl, a);
            batch.vertex2D(out1x, out1y, r, g, bl, 0f);
            batch.vertex2D(out2x, out2y, r, g, bl, 0f);
        }
    }

    private static void emitRing(Batch batch, List<float[]> outer, List<float[]> inner,
                                 float r, float g, float b, float a) {
        int n = outer.size();
        if (n < 3 || inner.size() != n) return;
        float half = AA_WIDTH * 0.5f;

        for (int i = 0; i < n; i++) {
            float[] o1 = outer.get(i), o2 = outer.get((i + 1) % n);
            float[] i1 = inner.get(i), i2 = inner.get((i + 1) % n);

            float o1x = o1[0] - o1[2] * half, o1y = o1[1] - o1[3] * half;
            float o2x = o2[0] - o2[2] * half, o2y = o2[1] - o2[3] * half;
            float i1x = i1[0] - i1[2] * half, i1y = i1[1] - i1[3] * half;
            float i2x = i2[0] - i2[2] * half, i2y = i2[1] - i2[3] * half;

            batch.vertex2D(o1x, o1y, r, g, b, a);
            batch.vertex2D(o2x, o2y, r, g, b, a);
            batch.vertex2D(i2x, i2y, r, g, b, a);
            batch.vertex2D(o1x, o1y, r, g, b, a);
            batch.vertex2D(i2x, i2y, r, g, b, a);
            batch.vertex2D(i1x, i1y, r, g, b, a);
        }

        for (int i = 0; i < n; i++) {
            float[] o1 = outer.get(i), o2 = outer.get((i + 1) % n);
            float in1x = o1[0] - o1[2] * half, in1y = o1[1] - o1[3] * half;
            float in2x = o2[0] - o2[2] * half, in2y = o2[1] - o2[3] * half;
            float out1x = o1[0] + o1[2] * half, out1y = o1[1] + o1[3] * half;
            float out2x = o2[0] + o2[2] * half, out2y = o2[1] + o2[3] * half;

            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(out2x, out2y, r, g, b, 0f);
        }

        for (int i = 0; i < n; i++) {
            float[] i1 = inner.get(i), i2 = inner.get((i + 1) % n);
            float in1x = i1[0] - i1[2] * half, in1y = i1[1] - i1[3] * half;
            float in2x = i2[0] - i2[2] * half, in2y = i2[1] - i2[3] * half;
            float out1x = i1[0] + i1[2] * half, out1y = i1[1] + i1[3] * half;
            float out2x = i2[0] + i2[2] * half, out2y = i2[1] + i2[3] * half;

            batch.vertex2D(in1x, in1y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(in2x, in2y, r, g, b, a);
            batch.vertex2D(out1x, out1y, r, g, b, 0f);
            batch.vertex2D(out2x, out2y, r, g, b, 0f);
        }
    }

    // ================================================================
    //  3D-piirto
    // ================================================================

    public void addLine3D(double x1, double y1, double z1, double x2, double y2, double z2, float thickness, int color) {
        CompiledRenderPipeline pipeline = worldColoredLines();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.DEBUG_LINES);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
    }

    public void addQuad(double x1, double y1, double z1,
                        double x2, double y2, double z2,
                        double x3, double y3, double z3,
                        double x4, double y4, double z4,
                        int color) {
        CompiledRenderPipeline pipeline = worldColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
        batch.vertex((float) x4, (float) y4, (float) z4, r, g, b, a);
    }

    public void addTriangle(double x1, double y1, double z1,
                            double x2, double y2, double z2,
                            double x3, double y3, double z3,
                            int color) {
        CompiledRenderPipeline pipeline = worldColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS3_COLOR, PrimitiveTopology.TRIANGLES);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        batch.vertex((float) x1, (float) y1, (float) z1, r, g, b, a);
        batch.vertex((float) x2, (float) y2, (float) z2, r, g, b, a);
        batch.vertex((float) x3, (float) y3, (float) z3, r, g, b, a);
    }

    // ================================================================
    //  2D-piirto
    // ================================================================

    public void addRect2D(float x, float y, float width, float height, int color) {
        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        addQuad2D(batch, x, y, width, height, r, g, b, a);
    }

    public void addRectOutline2D(float x, float y, float width, float height, float thickness, int color) {
        if (thickness <= 0) return;
        CompiledRenderPipeline pipeline = uiColoredLines();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.DEBUG_LINES);
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
        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = dy / len, ny = -dx / len;

        float halfT = thickness * 0.5f;
        float halfAA = AA_WIDTH * 0.5f;
        float solidHalf = Math.max(0f, halfT - halfAA);
        float outerHalf = halfT + halfAA;

        float p1a_x = x1 - nx * solidHalf, p1a_y = y1 - ny * solidHalf;
        float p1b_x = x1 + nx * solidHalf, p1b_y = y1 + ny * solidHalf;
        float p2a_x = x2 - nx * solidHalf, p2a_y = y2 - ny * solidHalf;
        float p2b_x = x2 + nx * solidHalf, p2b_y = y2 + ny * solidHalf;

        batch.vertex2D(p1a_x, p1a_y, r, g, b, a);
        batch.vertex2D(p1b_x, p1b_y, r, g, b, a);
        batch.vertex2D(p2b_x, p2b_y, r, g, b, a);
        batch.vertex2D(p1a_x, p1a_y, r, g, b, a);
        batch.vertex2D(p2b_x, p2b_y, r, g, b, a);
        batch.vertex2D(p2a_x, p2a_y, r, g, b, a);

        float p1c_x = x1 + nx * outerHalf, p1c_y = y1 + ny * outerHalf;
        float p2c_x = x2 + nx * outerHalf, p2c_y = y2 + ny * outerHalf;
        batch.vertex2D(p1b_x, p1b_y, r, g, b, a);
        batch.vertex2D(p1c_x, p1c_y, r, g, b, 0f);
        batch.vertex2D(p2c_x, p2c_y, r, g, b, 0f);
        batch.vertex2D(p1b_x, p1b_y, r, g, b, a);
        batch.vertex2D(p2c_x, p2c_y, r, g, b, 0f);
        batch.vertex2D(p2b_x, p2b_y, r, g, b, a);

        float p1d_x = x1 - nx * outerHalf, p1d_y = y1 - ny * outerHalf;
        float p2d_x = x2 - nx * outerHalf, p2d_y = y2 - ny * outerHalf;
        batch.vertex2D(p1d_x, p1d_y, r, g, b, 0f);
        batch.vertex2D(p1a_x, p1a_y, r, g, b, a);
        batch.vertex2D(p2a_x, p2a_y, r, g, b, a);
        batch.vertex2D(p1d_x, p1d_y, r, g, b, 0f);
        batch.vertex2D(p2a_x, p2a_y, r, g, b, a);
        batch.vertex2D(p2d_x, p2d_y, r, g, b, 0f);
    }

    public void addCircle(float cx, float cy, float radius, int color) {
        if (radius <= 0) return;
        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        int segs = segsForRadius(radius);
        List<float[]> boundary = buildCircleBoundary(cx, cy, radius, 1f, segs);
        emitFilledShape(batch, boundary, cx, cy, r, g, b, a);
    }

    public void addCircleOutline(float cx, float cy, float radius, float thickness, int color) {
        if (radius <= 0 || thickness <= 0) return;
        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float innerR = Math.max(0, radius - thickness);
        int segs = segsForRadius(radius);
        List<float[]> outerB = buildCircleBoundary(cx, cy, radius, 1f, segs);
        List<float[]> innerB = buildCircleBoundary(cx, cy, innerR, -1f, segs);
        emitRing(batch, outerB, innerB, r, g, b, a);
    }

    public void addRoundedRect(float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0) return;

        if (radius <= 0.5f) {
            addRect2D(x, y, w, h, color);
            return;
        }

        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        radius = Math.max(0, Math.min(radius, Math.min(w, h) * 0.5f));
        int arcSegs = arcSegsFor(radius);
        List<float[]> boundary = buildRoundedRectBoundary(
                x, y, w, h, radius, radius, radius, radius, 1f, arcSegs);
        emitFilledShape(batch, boundary, x + w * 0.5f, y + h * 0.5f, r, g, b, a);
    }

    public void addRoundedRectCustom(float x, float y, float w, float h, float radius, int color,
                                     boolean tl, boolean tr, boolean br, boolean bl) {
        if (w <= 0 || h <= 0) return;

        if (!tl && !tr && !br && !bl) {
            addRect2D(x, y, w, h, color);
            return;
        }
        if (radius <= 0.5f) {
            addRect2D(x, y, w, h, color);
            return;
        }

        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float rTL = tl ? radius : 0f;
        float rTR = tr ? radius : 0f;
        float rBR = br ? radius : 0f;
        float rBL = bl ? radius : 0f;
        int arcSegs = arcSegsFor(radius);
        List<float[]> boundary = buildRoundedRectBoundary(
                x, y, w, h, rTL, rTR, rBR, rBL, 1f, arcSegs);
        emitFilledShape(batch, boundary, x + w * 0.5f, y + h * 0.5f, r, g, b, a);
    }

    public void addRoundedRectOutline(float x, float y, float w, float h,
                                      float radius, float thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        thickness = Math.min(thickness, Math.min(w, h) * 0.5f);
        CompiledRenderPipeline pipeline = uiColored();
        if (pipeline == null) return;
        Batch batch = getOrCreateBatch(pipeline,
                AxiomVertexFormats.POS2_COLOR, PrimitiveTopology.TRIANGLES);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        radius = Math.max(0, Math.min(radius, Math.min(w, h) * 0.5f));
        float innerR = Math.max(0, radius - thickness);
        int arcSegs = arcSegsFor(Math.max(radius, innerR));

        List<float[]> outerB = buildRoundedRectBoundary(
                x, y, w, h, radius, radius, radius, radius, 1f, arcSegs);
        List<float[]> innerB = buildRoundedRectBoundary(
                x + thickness, y + thickness,
                w - 2f * thickness, h - 2f * thickness,
                innerR, innerR, innerR, innerR, -1f, arcSegs);

        emitRing(batch, outerB, innerB, r, g, b, a);
    }

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

    // ================================================================
    //  Tekstuurit — JOKAINEN TEKSTUURI OMaan BATCHIIN
    // ================================================================

    public void addTexture(Identifier texture, float x, float y, float width, float height, int color) {
        addTexturePart(texture, x, y, width, height, 0, 0, 1, 1, color);
    }

    public void addTexturePart(Identifier texture, float x, float y, float width, float height,
                               float u1, float v1, float u2, float v2, int color) {
        if (width <= 0 || height <= 0) return;

        Batch batch = getOrCreateTextureBatch(texture);  // ← avain = texture

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
        Batch batch = getOrCreateTextureBatch(texture);  // ← avain = texture

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

        float[] xs = {-halfW, halfW, halfW, -halfW};
        float[] ys = {-halfH, -halfH, halfH, halfH};
        float[] u = {0, 1, 1, 0};
        float[] v = {0, 0, 1, 1};

        float[] xRot = new float[4];
        float[] yRot = new float[4];
        for (int i = 0; i < 4; i++) {
            xRot[i] = cx + xs[i] * cos - ys[i] * sin;
            yRot[i] = cy + xs[i] * sin + ys[i] * cos;
        }

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
        CompiledRenderPipeline pipeline = uiText();
        if (pipeline == null) return;
        Batch batch = getOrCreateTextBatch(texture);

        batch.vertexUV(x0, y0, u0, v0, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);

        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x1, y1, u1, v1, r, g, b, a);
    }

    public void addGpuTextureQuadWithPipeline(CompiledRenderPipeline pipeline,
                                              GpuTextureView view, GpuSampler sampler,
                                              float x, float y, float w, float h,
                                              float u0, float v0, float u1, float v1,
                                              int color) {
        if (view == null || w <= 0 || h <= 0) return;

        Batch batch = gpuViewBatches.computeIfAbsent(view, k -> {
            Batch b = new Batch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
            b.setMvp(currentProjectionMatrix, currentModelViewMatrix);
            return b;
        });
        batch.setSampler(sampler);
        gpuViewSamplers.put(view, sampler);
        if (pipeline != null) gpuViewPipelines.put(view, pipeline);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ((color      ) & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float x2 = x + w, y2 = y + h;
        batch.vertexUV(x,  y,  u0, v0, r, g, b, a);
        batch.vertexUV(x2, y,  u1, v0, r, g, b, a);
        batch.vertexUV(x,  y2, u0, v1, r, g, b, a);
        batch.vertexUV(x,  y2, u0, v1, r, g, b, a);
        batch.vertexUV(x2, y,  u1, v0, r, g, b, a);
        batch.vertexUV(x2, y2, u1, v1, r, g, b, a);
    }

    public void addGpuTextureQuad(GpuTextureView view, GpuSampler sampler,
                                  float x, float y, float w, float h,
                                  float u0, float v0, float u1, float v1,
                                  int color) {
        addGpuTextureQuadWithPipeline(uiTextured(), view, sampler,
                x, y, w, h, u0, v0, u1, v1, color);
    }
    public void addTextureWithPipeline(Identifier texture, CompiledRenderPipeline pipeline,
                                        float x, float y, float w, float h,
                                        float u1, float v1, float u2, float v2,
                                        int color) {
        addTextureWithPipeline(texture, pipeline, x, y, w, h,
                u1, v1, u2, v2, 0f, color);
    }

    public void addTextureWithPipeline(Identifier texture, CompiledRenderPipeline pipeline,
                                       float x, float y, float w, float h,
                                       float u1, float v1, float u2, float v2,
                                       float param, int color) {
        if (w <= 0 || h <= 0) return;

        Batch batch = getOrCreateTextureBatch(texture);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ((color      ) & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float x2 = x + w;
        float y2 = y + h;

        // Z = param (paksuus)
        batch.vertexUV(x,  y,  param, u1, v1, r, g, b, a);
        batch.vertexUV(x2, y,  param, u2, v1, r, g, b, a);
        batch.vertexUV(x,  y2, param, u1, v2, r, g, b, a);
        batch.vertexUV(x,  y2, param, u1, v2, r, g, b, a);
        batch.vertexUV(x2, y,  param, u2, v1, r, g, b, a);
        batch.vertexUV(x2, y2, param, u2, v2, r, g, b, a);

        texturePipelines.put(texture, pipeline);
    }


    // ================================================================
    //  Sulku & getterit
    // ================================================================

    public void close() {
        if (allocator != null) allocator.close();
        for (VertexBufferManager vbm : bufferManagers.values()) {
            vbm.close();
        }
        bufferManagers.clear();
    }

    public boolean isScissorEnabled() { return scissorEnabled; }
    public int getScissorX() { return scissorX; }
    public int getScissorY() { return scissorY; }
    public int getScissorW() { return scissorW; }
    public int getScissorH() { return scissorH; }
}