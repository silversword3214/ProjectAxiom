package silversword.axiom.client.rendersystem.axiomrenderer.engine;

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
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silversword.axiom.client.rendersystem.utils.texture.Texture;

import java.nio.ByteBuffer;
import java.util.*;

public class RenderCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderCore.class);

    private static final Vector4f ONE_WHITE = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f ZERO_VEC  = new Vector3f(0f, 0f, 0f);
    private static final Matrix4f IDENTITY_MAT = new Matrix4f();

    // ---------- Batch pool ----------
    private record BatchKey(VertexFormat format, PrimitiveTopology mode) {}
    private final Map<BatchKey, ArrayDeque<Batch>> batchPool = new HashMap<>();

    // ---------- Active group maps (reused across commits) ----------
    private final Map<CompiledRenderPipeline, Batch> batches = new LinkedHashMap<>();
    private final Map<Identifier, Batch> textureBatches = new LinkedHashMap<>();
    private final Map<Texture, Batch> textBatches = new LinkedHashMap<>();
    private final Map<GpuTextureView, Batch> gpuViewBatches = new LinkedHashMap<>();
    private final Map<GpuTextureView, GpuSampler> gpuViewSamplers = new HashMap<>();
    private final Map<GpuTextureView, CompiledRenderPipeline> gpuViewPipelines = new HashMap<>();
    private final Map<Identifier, CompiledRenderPipeline> texturePipelines = new HashMap<>();

    // ---------- Committed groups (pooled) ----------
    private final List<CommittedGroup> groupPool = new ArrayList<>();
    private int groupUsed = 0;

    private final VertexBufferManager vertexBuffers = new VertexBufferManager();

    // ---------- Per-flush cache ----------
    private RenderTarget flushRenderTarget;
    private int flushTargetWidth;
    private int flushTargetHeight;
    private int flushScaledWidth;
    private int flushScaledHeight;
    private GpuBufferSlice sharedTransforms;

    // ---------- Prepared pool ----------
    private final List<Prepared> preparedPool = new ArrayList<>();
    private int preparedUsed = 0;

    // ---------- Index buffer cache ----------
    private final EnumMap<PrimitiveTopology, RenderSystem.AutoStorageIndexBuffer> indexBufferByMode
            = new EnumMap<>(PrimitiveTopology.class);

    private Matrix4f currentProjectionMatrix;
    private Matrix4f currentModelViewMatrix;

    private boolean scissorEnabled = false;
    private int scissorX, scissorY, scissorW, scissorH;

    private static RenderTarget targetOverride = null;
    public static void setTargetOverride(RenderTarget rt) { targetOverride = rt; }

    // ================================================================
    //  Internal types
    // ================================================================
    private static final class CommittedGroup {
        final Map<CompiledRenderPipeline, Batch> batches = new LinkedHashMap<>();
        final Map<Identifier, Batch> textureBatches = new LinkedHashMap<>();
        final Map<Texture, Batch> textBatches = new LinkedHashMap<>();
        final Map<GpuTextureView, Batch> gpuViewBatches = new LinkedHashMap<>();
        final Map<GpuTextureView, GpuSampler> gpuViewSamplers = new HashMap<>();
        final Map<GpuTextureView, CompiledRenderPipeline> gpuViewPipelines = new HashMap<>();
        final Map<Identifier, CompiledRenderPipeline> texturePipelines = new HashMap<>();
        boolean scissorEnabled;
        int sx, sy, sw, sh;

        void clear() {
            batches.clear();
            textureBatches.clear();
            textBatches.clear();
            gpuViewBatches.clear();
            gpuViewSamplers.clear();
            gpuViewPipelines.clear();
            texturePipelines.clear();
        }
    }

    private static final class Prepared {
        CompiledRenderPipeline pipeline;
        GpuBufferSlice vertices;
        GpuBuffer indices;
        IndexType indexType;
        int indexCount;
        GpuBufferSlice transforms;
        GpuTextureView textureView;
        GpuSampler sampler;
    }

    public RenderCore() {}

    // ================================================================
    //  Pipeline-getterit
    // ================================================================
    public static CompiledRenderPipeline uiColored() {
        CompiledRenderPipeline p = RenderPipelines.UI_COLORED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_COLORED; }
        return p;
    }
    public static CompiledRenderPipeline uiColoredLines() {
        CompiledRenderPipeline p = RenderPipelines.UI_COLORED_LINES;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_COLORED_LINES; }
        return p;
    }
    public static CompiledRenderPipeline uiTextured() {
        CompiledRenderPipeline p = RenderPipelines.UI_TEXTURED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_TEXTURED; }
        return p;
    }
    static CompiledRenderPipeline uiText() {
        CompiledRenderPipeline p = RenderPipelines.UI_TEXT;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.UI_TEXT; }
        return p;
    }
    public static CompiledRenderPipeline worldColored() {
        CompiledRenderPipeline p = RenderPipelines.WORLD_COLORED;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.WORLD_COLORED; }
        return p;
    }
    public static CompiledRenderPipeline worldColoredLines() {
        CompiledRenderPipeline p = RenderPipelines.WORLD_COLORED_LINES;
        if (p == null) { RenderPipelines.rebuildAll(); p = RenderPipelines.WORLD_COLORED_LINES; }
        return p;
    }

    // ================================================================
    //  Batch pool
    // ================================================================
    private Batch acquireBatch(VertexFormat format, PrimitiveTopology mode) {
        BatchKey key = new BatchKey(format, mode);
        ArrayDeque<Batch> pool = batchPool.computeIfAbsent(key, k -> new ArrayDeque<>());
        Batch b = pool.pollLast();
        return (b != null) ? b : new Batch(format, mode);
    }

    private void releaseBatch(Batch b) {
        b.clear();
        BatchKey key = new BatchKey(b.getFormat(), b.getMode());
        batchPool.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(b);
    }

    // ================================================================
    //  Batch-hallinta
    // ================================================================
    public Batch batchFor(CompiledRenderPipeline pipeline,
                          VertexFormat format,
                          PrimitiveTopology mode) {
        Batch existing = batches.get(pipeline);
        if (existing != null) return existing;
        Batch b = acquireBatch(format, mode);
        batches.put(pipeline, b);
        return b;
    }

    public Batch textureBatchFor(Identifier texture) {
        Batch existing = textureBatches.get(texture);
        if (existing != null) return existing;
        Batch b = acquireBatch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
        b.setTexture(texture);
        textureBatches.put(texture, b);
        return b;
    }

    Batch textBatchFor(Texture texture) {
        Batch existing = textBatches.get(texture);
        if (existing != null) return existing;
        Batch b = acquireBatch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
        textBatches.put(texture, b);
        return b;
    }

    public Batch gpuViewBatchFor(GpuTextureView view) {
        Batch existing = gpuViewBatches.get(view);
        if (existing != null) return existing;
        Batch b = acquireBatch(AxiomVertexFormats.POS2_UV_COLOR, PrimitiveTopology.TRIANGLES);
        gpuViewBatches.put(view, b);
        return b;
    }

    public void registerGpuViewSampler(GpuTextureView view, GpuSampler sampler) {
        gpuViewSamplers.put(view, sampler);
    }
    public void registerGpuViewPipeline(GpuTextureView view, CompiledRenderPipeline pipeline) {
        if (pipeline != null) gpuViewPipelines.put(view, pipeline);
    }
    public void registerTexturePipeline(Identifier texture, CompiledRenderPipeline pipeline) {
        if (pipeline != null) texturePipelines.put(texture, pipeline);
    }

    // ================================================================
    //  Elinkaari
    // ================================================================
    public void beginFrame(Matrix4f projection, Matrix4f modelView) {
        commitCurrentGroup();
        this.currentProjectionMatrix = projection;
        this.currentModelViewMatrix = modelView;
    }

    public void flush() {
        commitCurrentGroup();
        if (groupUsed == 0) return;

        vertexBuffers.beginFrame();

        this.flushRenderTarget = getRenderTarget();
        Minecraft mc = Minecraft.getInstance();
        this.flushTargetWidth  = flushRenderTarget.width;
        this.flushTargetHeight = flushRenderTarget.height;
        this.flushScaledWidth  = mc.getWindow().getGuiScaledWidth();
        this.flushScaledHeight = mc.getWindow().getGuiScaledHeight();

        Matrix4f mvp = new Matrix4f(currentProjectionMatrix).mul(currentModelViewMatrix);
        this.sharedTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                mvp, ONE_WHITE, ZERO_VEC, IDENTITY_MAT);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        try {
            for (int i = 0; i < groupUsed; i++) {
                drawGroup(encoder, groupPool.get(i));
            }
            GpuFence fence = encoder.createFence();
            vertexBuffers.setFenceAll(fence);
        } catch (Exception e) {
            LOGGER.error("RenderCore.flush failed", e);
        } finally {
            encoder.submit();
            for (int i = 0; i < groupUsed; i++) {
                releaseGroupBatches(groupPool.get(i));
            }
            groupUsed = 0;
        }
    }

    private void releaseGroupBatches(CommittedGroup g) {
        for (Batch b : g.batches.values())         releaseBatch(b);
        for (Batch b : g.textureBatches.values())  releaseBatch(b);
        for (Batch b : g.textBatches.values())     releaseBatch(b);
        for (Batch b : g.gpuViewBatches.values())  releaseBatch(b);
    }

    public void enableScissor(int x, int y, int w, int h) {
        if (!scissorEnabled || scissorX != x || scissorY != y
                || scissorW != w || scissorH != h) {
            commitCurrentGroup();
        }
        this.scissorEnabled = true;
        this.scissorX = x; this.scissorY = y;
        this.scissorW = w; this.scissorH = h;
    }

    public void disableScissor() {
        if (scissorEnabled) commitCurrentGroup();
        this.scissorEnabled = false;
    }

    public void close() {
        vertexBuffers.close();
    }

    public boolean isScissorEnabled() { return scissorEnabled; }
    public int getScissorX() { return scissorX; }
    public int getScissorY() { return scissorY; }
    public int getScissorW() { return scissorW; }
    public int getScissorH() { return scissorH; }

    // ================================================================
    //  Ryhmän piirto
    // ================================================================
    private void drawGroup(CommandEncoder encoder, CommittedGroup g) {
        boolean savedSE = scissorEnabled;
        int savedSx = scissorX, savedSy = scissorY;
        int savedSw = scissorW, savedSh = scissorH;
        scissorEnabled = g.scissorEnabled;
        scissorX = g.sx; scissorY = g.sy;
        scissorW = g.sw; scissorH = g.sh;

        try {
            int count = prepareGroup(encoder, g);
            if (count == 0) return;

            var rt = flushRenderTarget;
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "axiom_group",
                    rt.getColorTextureView(),
                    Optional.empty(),
                    rt.getDepthTextureView(),
                    OptionalDouble.empty())) {

                if (!applyScissor(pass)) return;

                RenderSystem.bindDefaultUniforms(pass);

                for (int i = 0; i < count; i++) {
                    Prepared p = preparedPool.get(i);
                    pass.setPipeline(p.pipeline);
                    if (p.textureView != null) {
                        pass.setUniform("u_Texture", p.textureView, p.sampler);
                    }
                    pass.setUniform("DynamicTransforms", p.transforms);
                    pass.setVertexBuffer(0, p.vertices);
                    pass.setIndexBuffer(p.indices, p.indexType);
                    pass.drawIndexed(p.indexCount, 1, 0, 0, 0);
                }
            }
        } finally {
            scissorEnabled = savedSE;
            scissorX = savedSx; scissorY = savedSy;
            scissorW = savedSw; scissorH = savedSh;
        }
    }

    private Prepared nextPrepared() {
        Prepared p;
        if (preparedUsed < preparedPool.size()) {
            p = preparedPool.get(preparedUsed);
        } else {
            p = new Prepared();
            preparedPool.add(p);
        }
        preparedUsed++;
        return p;
    }

    private int prepareGroup(CommandEncoder encoder, CommittedGroup g) {
        preparedUsed = 0;

        for (Map.Entry<CompiledRenderPipeline, Batch> e : g.batches.entrySet()) {
            prepare(encoder, e.getKey(), e.getValue(), null, null);
        }

        CompiledRenderPipeline textPipe = uiText();
        if (textPipe != null) {
            for (Map.Entry<Texture, Batch> e : g.textBatches.entrySet()) {
                Texture tex = e.getKey();
                prepare(encoder, textPipe, e.getValue(), tex.textureView(), tex.sampler());
            }
        }

        CompiledRenderPipeline defaultTex = uiTextured();
        var texMgr = Minecraft.getInstance().getTextureManager();
        for (Map.Entry<Identifier, Batch> e : g.textureBatches.entrySet()) {
            Identifier id = e.getKey();
            Batch batch = e.getValue();
            CompiledRenderPipeline pipe = g.texturePipelines.getOrDefault(id, defaultTex);
            if (pipe == null) continue;

            GpuTextureView view = null;
            GpuSampler sampler = null;
            var abstractTexture = texMgr.getTexture(id);
            if (abstractTexture != null) {
                view = abstractTexture.getTextureView();
                sampler = abstractTexture.getSampler();
            }
            prepare(encoder, pipe, batch, view, sampler);
        }

        for (Map.Entry<GpuTextureView, Batch> e : g.gpuViewBatches.entrySet()) {
            GpuTextureView view = e.getKey();
            Batch batch = e.getValue();
            CompiledRenderPipeline pipe = g.gpuViewPipelines.getOrDefault(view, defaultTex);
            if (pipe == null) continue;
            GpuSampler sampler = g.gpuViewSamplers.get(view);
            batch.setSampler(sampler);
            prepare(encoder, pipe, batch, view, sampler);
        }

        return preparedUsed;
    }

    private void prepare(CommandEncoder encoder,
                         CompiledRenderPipeline pipeline,
                         Batch batch,
                         GpuTextureView textureView,
                         GpuSampler sampler) {
        if (pipeline == null || batch.vertexCount() == 0) return;

        GpuBufferSlice vertices = uploadBatch(encoder, batch);
        if (vertices == null) return;

        int indexCount = batch.vertexCount();
        RenderSystem.AutoStorageIndexBuffer isb = indexBufferFor(batch.getMode());
        GpuBuffer indices = isb.getBuffer(indexCount);
        IndexType indexType = isb.type();

        Prepared p = nextPrepared();
        p.pipeline = pipeline;
        p.vertices = vertices;
        p.indices = indices;
        p.indexType = indexType;
        p.indexCount = indexCount;
        p.transforms = sharedTransforms;
        p.textureView = textureView;
        p.sampler = sampler;
    }

    private GpuBufferSlice uploadBatch(CommandEncoder encoder, Batch batch) {
        int size = batch.byteCount();
        if (size == 0) return null;
        ByteBuffer data = batch.data();
        data.flip();  // position=0, limit=size
        return vertexBuffers.upload(data, size, encoder);
    }

    private RenderSystem.AutoStorageIndexBuffer indexBufferFor(PrimitiveTopology mode) {
        RenderSystem.AutoStorageIndexBuffer cached = indexBufferByMode.get(mode);
        if (cached != null) return cached;
        cached = RenderSystem.getSequentialBuffer(mode);
        indexBufferByMode.put(mode, cached);
        return cached;
    }

    // ================================================================
    //  Commit
    // ================================================================
    private void commitCurrentGroup() {
        if (batches.isEmpty() && textureBatches.isEmpty()
                && textBatches.isEmpty() && gpuViewBatches.isEmpty()) {
            return;
        }

        CommittedGroup g;
        if (groupUsed < groupPool.size()) {
            g = groupPool.get(groupUsed);
            g.clear();
        } else {
            g = new CommittedGroup();
            groupPool.add(g);
        }
        groupUsed++;

        g.batches.putAll(batches);
        g.textureBatches.putAll(textureBatches);
        g.textBatches.putAll(textBatches);
        g.gpuViewBatches.putAll(gpuViewBatches);
        g.gpuViewSamplers.putAll(gpuViewSamplers);
        g.gpuViewPipelines.putAll(gpuViewPipelines);
        g.texturePipelines.putAll(texturePipelines);
        g.scissorEnabled = scissorEnabled;
        g.sx = scissorX; g.sy = scissorY;
        g.sw = scissorW; g.sh = scissorH;

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

    private boolean applyScissor(RenderPass pass) {
        if (!scissorEnabled) { pass.disableScissor(); return true; }

        if (flushTargetWidth <= 0 || flushTargetHeight <= 0
                || flushScaledWidth <= 0 || flushScaledHeight <= 0) {
            pass.disableScissor();
            return true;
        }

        float scaleX = (float) flushTargetWidth / flushScaledWidth;
        float scaleY = (float) flushTargetHeight / flushScaledHeight;

        int fbMinX = Math.round(scissorX * scaleX);
        int fbMaxX = Math.round((scissorX + scissorW) * scaleX);
        int fbMinY = flushTargetHeight - Math.round((scissorY + scissorH) * scaleY);
        int fbMaxY = flushTargetHeight - Math.round(scissorY * scaleY);

        int clampedMinX = Math.max(0, Math.min(fbMinX, flushTargetWidth));
        int clampedMaxX = Math.max(0, Math.min(fbMaxX, flushTargetWidth));
        int clampedMinY = Math.max(0, Math.min(fbMinY, flushTargetHeight));
        int clampedMaxY = Math.max(0, Math.min(fbMaxY, flushTargetHeight));

        int glW = clampedMaxX - clampedMinX;
        int glH = clampedMaxY - clampedMinY;
        if (glW <= 0 || glH <= 0) return false;

        pass.enableScissor(clampedMinX, clampedMinY, glW, glH);
        return true;
    }

    // ================================================================
    //  Teksti-glyph-kvadi
    // ================================================================
    public void addTextQuadMesh(Texture texture,
                                float x0, float y0, float x1, float y1,
                                float u0, float v0, float u1, float v1,
                                float r, float g, float b, float a) {
        CompiledRenderPipeline pipeline = uiText();
        if (pipeline == null) return;

        Batch batch = textBatchFor(texture);

        batch.vertexUV(x0, y0, u0, v0, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);

        batch.vertexUV(x0, y1, u0, v1, r, g, b, a);
        batch.vertexUV(x1, y0, u1, v0, r, g, b, a);
        batch.vertexUV(x1, y1, u1, v1, r, g, b, a);
    }
}