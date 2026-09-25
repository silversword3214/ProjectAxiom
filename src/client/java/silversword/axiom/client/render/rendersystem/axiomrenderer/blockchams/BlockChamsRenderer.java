package silversword.axiom.client.render.rendersystem.axiomrenderer.blockchams;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.mixininterface.ILevelRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

import java.util.*;
import java.util.function.Predicate;

/**
 * Itsenäinen block chams -pipeline. Ei riipu ShaderESP-luokista.
 * Käyttää omia BlockMaskRenderTarget / BlockMaskTexture / BlockSubmitQueue -luokkia.
 */
public final class BlockChamsRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("Axiom/BlockChams");

    /** Yksi instanssi EVENT_BUS:iin. Rekisteröi AxiomInitialize:ssa. */
    public static final BlockChamsRenderer INSTANCE = new BlockChamsRenderer();

    private static BlockSubmitQueue        queue;
    private static FeatureRenderDispatcher featureDispatcher;

    private static final Map<String, Layer> layers = new LinkedHashMap<>();

    private BlockChamsRenderer() {}

    // ═══════════════════════════════════════════════════════════════
    //  LAYER — täysin julkinen konfiguraatio
    // ═══════════════════════════════════════════════════════════════

    public static final class Layer {
        public final String id;
        public BlockMaskRenderTarget mask;
        public Identifier chamsId, fillId, edgeId;
        public BlockMaskTexture texture;
        public int registeredW = -1, registeredH = -1;
        public boolean active = false;

        // Konfiguraatio
        public Predicate<BlockEntity> filter = be -> false;
        public int   chamsTint        = 0xFFFFFFFF;
        public int   fillColor        = 0x80FFFFFF;
        public int   outlineColor     = 0xFFFFFFFF;
        public float outlineThickness = 2f;
        public int   downscale        = 2;
        public int   scanRadiusChunks = 8;

        public boolean renderChams   = true;
        public boolean renderFill    = false;
        public boolean renderOutline = false;
        public boolean enabled       = false;

        public Layer(String id) {
            this.id = id;
            String safe = id.replaceAll("[^a-zA-Z0-9_]", "_");
            this.mask    = new BlockMaskRenderTarget("BlockChams_" + safe);
            this.chamsId = Identifier.fromNamespaceAndPath("projectaxiom", "blockchams_" + safe + "_chams");
            this.fillId  = Identifier.fromNamespaceAndPath("projectaxiom", "blockchams_" + safe + "_fill");
            this.edgeId  = Identifier.fromNamespaceAndPath("projectaxiom", "blockchams_" + safe + "_edge");
        }
    }

    public static Layer layer(String id) {
        return layers.computeIfAbsent(id, Layer::new);
    }

    // ═══════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════

    public static void init() {
        if (queue == null) queue = new BlockSubmitQueue();
        ensureFeatureDispatcher();
    }

    public static boolean anyEnabled() {
        for (Layer l : layers.values()) {
            if (l.enabled) return true;
        }
        return false;
    }

    private static void ensureFeatureDispatcher() {
        if (featureDispatcher != null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        featureDispatcher = new FeatureRenderDispatcher(
                mc.gameRenderer.renderBuffers(),
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState());
        LOG.info("[BlockChams] feature dispatcher created");
    }

    private static void ensureLayerTextures(Layer l) {
        if (l.mask.getColorTexture() == null) return;
        int w = l.mask.width;
        int h = l.mask.height;
        if (w <= 0 || h <= 0) return;
        if (w == l.registeredW && h == l.registeredH && l.texture != null) return;

        l.texture = new BlockMaskTexture(l.mask);
        var tm = Minecraft.getInstance().getTextureManager();
        tm.register(l.chamsId, l.texture);
        tm.register(l.fillId,  l.texture);
        tm.register(l.edgeId,  l.texture);

        l.registeredW = w;
        l.registeredH = h;
    }

    // ═══════════════════════════════════════════════════════════════
    //  SUBMIT — LevelRendererMixin kutsuu tätä submitEntities TAIL:ssa
    // ═══════════════════════════════════════════════════════════════

    public static void submitAll(LevelRenderer levelRenderer,
                                 PoseStack poseStack,
                                 LevelRenderState levelState,
                                 BlockEntityRenderDispatcher beDispatcher) {
        if (layers.isEmpty()) return;

        boolean anyEnabled = false;
        for (Layer l : layers.values()) {
            if (l.enabled && l.filter != null) { anyEnabled = true; break; }
        }
        if (!anyEnabled) return;

        init();
        if (featureDispatcher == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ILevelRenderer ilr = (ILevelRenderer) levelRenderer;
        Vec3 camPos = levelState.cameraRenderState.pos;
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (Layer l : layers.values()) l.active = false;

        for (Layer l : layers.values()) {
            if (!l.enabled || l.filter == null) continue;

            List<BlockEntity> targets = scanBlockEntities(mc, l);
            if (targets.isEmpty()) continue;

            l.mask.syncToWindow(l.downscale);
            l.mask.clearMask();
            ensureLayerTextures(l);
            if (l.texture == null) continue;

            queue.getSubmitsPerOrder().clear();

            int submitted = 0;
            for (BlockEntity be : targets) {
                try {
                    if (submitBlockEntity(poseStack, queue, beDispatcher, be,
                            levelState, camPos, partialTicks)) {
                        submitted++;
                    }
                } catch (Throwable t) {
                    LOG.warn("[BlockChams] submit failed for {}: {}",
                            be.getType(), t.toString());
                }
            }

            if (submitted == 0 || queue.getSubmitsPerOrder().isEmpty()) continue;

            ilr.axiom$pushEntityOutlineFramebuffer(l.mask);
            try {
                try (FeatureRenderDispatcher.PreparedFrame frame =
                             featureDispatcher.prepareFrame(queue)) {
                    if (frame.isEmpty()) continue;

                    var mvStack = RenderSystem.getModelViewStack();
                    mvStack.pushMatrix();
                    mvStack.mul(levelState.cameraRenderState.viewRotationMatrix);

                    var encoder = RenderSystem.getDevice().createCommandEncoder();
                    try (var pass = encoder.createRenderPass(
                            () -> "block_chams_mask_" + l.id.toLowerCase(),
                            l.mask.getColorTextureView(),
                            Optional.empty(),
                            l.mask.getDepthTextureView(),
                            OptionalDouble.of(0.0))) {
                        RenderSystem.bindDefaultUniforms(pass);
                        FeatureRenderDispatcher.renderAllFeatures(pass, frame);
                    } finally {
                        encoder.submit();
                        mvStack.popMatrix();
                    }
                }
            } catch (Throwable t) {
                LOG.error("[BlockChams] render failed for layer {}", l.id, t);
            } finally {
                ilr.axiom$popEntityOutlineFramebuffer();
            }

            l.active = true;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean submitBlockEntity(PoseStack poseStack,
                                             BlockSubmitQueue out,
                                             BlockEntityRenderDispatcher dispatcher,
                                             BlockEntity be,
                                             LevelRenderState levelState,
                                             Vec3 camPos,
                                             float partialTicks) {
        BlockEntityRenderer renderer = dispatcher.getRenderer(be);
        if (renderer == null) return false;

        try {
            if (!renderer.shouldRender(be, camPos)) return false;
        } catch (Throwable ignored) {}

        BlockEntityRenderState state;
        try {
            state = (BlockEntityRenderState) renderer.createRenderState();
            renderer.extractRenderState(be, state, partialTicks, camPos, null);
        } catch (Throwable t) {
            return false;
        }

        BlockPos pos = be.getBlockPos();
        poseStack.pushPose();
        poseStack.translate(
                pos.getX() - camPos.x,
                pos.getY() - camPos.y,
                pos.getZ() - camPos.z);

        try {
            renderer.submit(state, poseStack, out, levelState.cameraRenderState);
        } finally {
            poseStack.popPose();
        }
        return true;
    }

    private static List<BlockEntity> scanBlockEntities(Minecraft mc, Layer l) {
        List<BlockEntity> out = new ArrayList<>();
        if (mc.level == null || mc.player == null) return out;

        int radius = Math.max(1, l.scanRadiusChunks);
        int pcx = mc.player.chunkPosition().x();
        int pcz = mc.player.chunkPosition().z();

        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                LevelChunk chunk = mc.level.getChunkSource()
                        .getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    try {
                        if (l.filter.test(be)) out.add(be);
                    } catch (Throwable ignored) {}
                }
            }
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMPOSITE — laukeaa Render2DEventin kautta
    // ═══════════════════════════════════════════════════════════════

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        compositeToScreen(event.getRenderer());
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMPOSITE — kutsutaan SUORAAN HudManagerista, ei EVENT_BUS:in kautta
    // ═══════════════════════════════════════════════════════════════

    public static void compositeToScreen(Renderer2D hud) {
        if (hud == null) return;

        var window = Minecraft.getInstance().getWindow();
        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        for (Layer l : layers.values()) {
            if (!l.active || l.texture == null) continue;

            if (l.renderChams) {
                hud.drawEntityChams(l.chamsId, 0, 0, w, h, l.chamsTint);
            }
            if (l.renderFill) {
                hud.drawEntityFill(l.fillId, 0, 0, w, h, l.fillColor);
            }
            if (l.renderOutline) {
                hud.drawEntityEdge(l.edgeId, 0, 0, w, h, l.outlineColor, l.outlineThickness);
            }
        }
    }

    public static void shutdown() {
        if (featureDispatcher != null) {
            featureDispatcher.close();
            featureDispatcher = null;
        }
    }
}