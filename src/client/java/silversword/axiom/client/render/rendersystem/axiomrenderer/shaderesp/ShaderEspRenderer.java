package silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silversword.axiom.client.mixininterface.IEntityRenderState;
import silversword.axiom.client.mixininterface.ILevelRenderer;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

import java.util.*;
import java.util.function.Predicate;

public final class ShaderEspRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("Axiom/ShaderESP");

    private static boolean enabled = false;
    private static int downscale = 2;
    private static Predicate<Entity> filter = e -> true;

    private static boolean renderChams   = false;
    private static boolean renderFill    = false;
    private static boolean renderOutline = true;
    private static int chamsTint         = 0xFFFFFFFF;
    private static float outlineThickness = 2f;

    private static final Map<TargetGroup, GroupState> groupStates   = new EnumMap<>(TargetGroup.class);
    private static final Map<TargetGroup, Integer>    fillColors    = new EnumMap<>(TargetGroup.class);
    private static final Map<TargetGroup, Integer>    outlineColors = new EnumMap<>(TargetGroup.class);
    private static final Set<TargetGroup>             activeGroups  = EnumSet.noneOf(TargetGroup.class);

    private static OutlineRenderCommandQueue queue;
    private static FeatureRenderDispatcher   featureDispatcher;

    // ─── Inner state ──────────────────────────────────────────────────────

    private static final class GroupState {
        final EntityMaskRenderTarget mask;
        final Identifier chamsId;
        final Identifier fillId;
        final Identifier edgeId;
        MaskTexture texture;
        int registeredW = -1;
        int registeredH = -1;

        GroupState(TargetGroup g) {
            String name = g.name().toLowerCase();
            this.mask    = new EntityMaskRenderTarget("ShaderEspMask_" + g.name());
            this.chamsId = Identifier.fromNamespaceAndPath("projectaxiom", "shaderesp_" + name + "_chams");
            this.fillId  = Identifier.fromNamespaceAndPath("projectaxiom", "shaderesp_" + name + "_fill");
            this.edgeId  = Identifier.fromNamespaceAndPath("projectaxiom", "shaderesp_" + name + "_edge");
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────

    public static void init() {
        if (queue == null) queue = new OutlineRenderCommandQueue();
    }

    // ─── Setters ──────────────────────────────────────────────────────────

    public static void setEnabled(boolean v)          { enabled = v; }
    public static boolean isEnabled()                 { return enabled; }
    public static void setDownscale(int d)            { downscale = Math.max(1, Math.min(4, d)); }
    public static void setFilter(Predicate<Entity> f) { filter = f; }

    public static void setRenderChams(boolean v)      { renderChams = v; }
    public static void setRenderFill(boolean v)       { renderFill = v; }
    public static void setRenderOutline(boolean v)    { renderOutline = v; }
    public static void setChamsTint(int c)            { chamsTint = c; }

    public static void setFillColor(TargetGroup g, int argb)    { fillColors.put(g, argb); }
    public static void setOutlineColor(TargetGroup g, int argb) { outlineColors.put(g, argb); }
    public static void setOutlineThickness(float t) {
        outlineThickness = Math.max(1f, Math.min(8f, t));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private static GroupState getGroupState(TargetGroup g) {
        return groupStates.computeIfAbsent(g, GroupState::new);
    }

    private static int fillColor(TargetGroup g)    { return fillColors.getOrDefault(g, 0x80FFFFFF); }
    private static int outlineColor(TargetGroup g) { return outlineColors.getOrDefault(g, 0xFFFFFFFF); }

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
        LOG.info("[ShaderESP] feature dispatcher created");
    }

    private static void ensureGroupTextures(GroupState state) {
        if (state.mask.getColorTexture() == null) return;
        int w = state.mask.width;
        int h = state.mask.height;
        if (w <= 0 || h <= 0) return;
        if (w == state.registeredW && h == state.registeredH && state.texture != null) return;

        state.texture = new MaskTexture(state.mask);
        var tm = Minecraft.getInstance().getTextureManager();
        // Rekisteröi sama tekstuuri-instanssi kolmella eri ID:llä.
        // Tämä mahdollistaa kolme eri pipelinea samalle mask-RT:lle.
        tm.register(state.chamsId, state.texture);
        tm.register(state.fillId,  state.texture);
        tm.register(state.edgeId,  state.texture);

        state.registeredW = w;
        state.registeredH = h;
    }

    // ─── Submit ───────────────────────────────────────────────────────────

    public static void submitEntities(LevelRenderer levelRenderer,
                                      PoseStack poseStack,
                                      LevelRenderState levelState,
                                      EntityRenderDispatcher dispatcher) {
        if (!enabled) return;
        init();
        activeGroups.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ensureFeatureDispatcher();
        if (featureDispatcher == null) return;

        Map<TargetGroup, List<EntityRenderState>> grouped = new EnumMap<>(TargetGroup.class);
        for (EntityRenderState state : levelState.entityRenderStates) {
            Entity e = ((IEntityRenderState) state).axiom$getEntity();
            if (e == null || e == mc.player || !e.isAlive()) continue;
            if (!filter.test(e)) continue;
            TargetGroup g = TargetGroup.getGroup(e);
            grouped.computeIfAbsent(g, k -> new ArrayList<>()).add(state);
        }

        if (grouped.isEmpty()) return;

        Vec3 camPos = levelState.cameraRenderState.pos;
        ILevelRenderer ilr = (ILevelRenderer) levelRenderer;

        for (Map.Entry<TargetGroup, List<EntityRenderState>> entry : grouped.entrySet()) {
            TargetGroup group = entry.getKey();
            List<EntityRenderState> targets = entry.getValue();

            GroupState gs = getGroupState(group);
            gs.mask.syncToWindow(downscale);
            gs.mask.clearMask();
            ensureGroupTextures(gs);

            queue.getSubmitsPerOrder().clear();

            for (EntityRenderState state : targets) {
                try {
                    EntityRenderer<?, ? super EntityRenderState> renderer = dispatcher.getRenderer(state);
                    Vec3 offset = renderer.getRenderOffset(state);
                    poseStack.pushPose();
                    poseStack.translate(
                            state.x - camPos.x + offset.x,
                            state.y - camPos.y + offset.y,
                            state.z - camPos.z + offset.z);
                    renderer.submit(state, poseStack, queue, levelState.cameraRenderState);
                    poseStack.popPose();
                } catch (Throwable t) {
                    LOG.warn("submit failed: {}", t.toString());
                }
            }

            if (queue.getSubmitsPerOrder().isEmpty()) continue;

            ilr.axiom$pushEntityOutlineFramebuffer(gs.mask);
            try {
                try (FeatureRenderDispatcher.PreparedFrame frame = featureDispatcher.prepareFrame(queue)) {
                    if (frame.isEmpty()) continue;

                    var mvStack = RenderSystem.getModelViewStack();
                    mvStack.pushMatrix();
                    mvStack.mul(levelState.cameraRenderState.viewRotationMatrix);

                    var encoder = RenderSystem.getDevice().createCommandEncoder();
                    try (var pass = encoder.createRenderPass(
                            () -> "shader_esp_mask_" + group.name().toLowerCase(),
                            gs.mask.getColorTextureView(),
                            Optional.empty(),
                            gs.mask.getDepthTextureView(),
                            OptionalDouble.of(0.0))) {
                        RenderSystem.bindDefaultUniforms(pass);
                        FeatureRenderDispatcher.renderAllFeatures(pass, frame);
                    } finally {
                        encoder.submit();
                        mvStack.popMatrix();
                    }
                }
            } catch (Throwable t) {
                LOG.error("group render failed for {}", group, t);
            } finally {
                ilr.axiom$popEntityOutlineFramebuffer();
            }

            activeGroups.add(group);
        }
    }

    // ─── Composite ────────────────────────────────────────────────────────

    public static void compositeToScreen(Renderer2D hud) {
        if (!enabled || activeGroups.isEmpty()) return;

        var window = Minecraft.getInstance().getWindow();
        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        for (TargetGroup g : activeGroups) {
            GroupState gs = groupStates.get(g);
            if (gs == null || gs.texture == null) continue;

            // Jokainen kutsu menee eri ID:llä → eri batch → eri pipeline.
            // Kaikki kolme voivat olla yhtä aikaa päällä.
            if (renderChams) {
                hud.drawEntityChams(gs.chamsId, 0, 0, w, h, chamsTint);
            }
            if (renderFill) {
                hud.drawEntityFill(gs.fillId, 0, 0, w, h, fillColor(g));
            }
            if (renderOutline) {
                hud.drawEntityEdge(gs.edgeId, 0, 0, w, h,
                        outlineColor(g), outlineThickness);
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