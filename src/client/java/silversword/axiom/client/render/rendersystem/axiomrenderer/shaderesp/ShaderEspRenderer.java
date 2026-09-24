package silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silversword.axiom.client.mixininterface.IEntityRenderState;
import silversword.axiom.client.mixininterface.ILevelRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;

public final class ShaderEspRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("Axiom/ShaderESP");

    private static EntityMaskRenderTarget mask;
    private static OutlineRenderCommandQueue queue;
    private static boolean enabled = false;
    private static int downscale = 2;
    private static int outlineColor = 0xFFFFFFFF;
    private static Predicate<Entity> filter = e -> true;

    private static int registeredW = -1;
    private static int registeredH = -1;

    private static boolean renderChams = false;
    private static boolean renderOutline = true;
    private static int chamsTint = 0xFFFFFFFF;

    public static void setRenderChams(boolean v)  { renderChams = v; }
    public static void setRenderOutline(boolean v){ renderOutline = v; }
    public static void setChamsTint(int c)        { chamsTint = c; }

    public static final net.minecraft.resources.Identifier MASK_TEXTURE_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "projectaxiom", "shaderesp_mask");

    public static final net.minecraft.resources.Identifier OUTLINE_TEXTURE_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "projectaxiom", "shaderesp_outline");

    private static MaskTexture maskTexture;
    private static boolean maskRegistered = false;

    // Cache render dispatcher
    private static FeatureRenderDispatcher featureDispatcher;

    public static void init() {
        if (mask  == null) mask  = new EntityMaskRenderTarget();
        if (queue == null) queue = new OutlineRenderCommandQueue();
        // HUOM: featureDispatcher luodaan laiskasti vasta kun sitä tarvitaan
    }


    public static EntityMaskRenderTarget getMask()   { return mask; }
    public static void setEnabled(boolean v)         { enabled = v; }
    public static boolean isEnabled()                { return enabled; }
    public static void setDownscale(int d)           { downscale = Math.max(1, Math.min(4, d)); }
    public static void setOutlineColor(int c)        { outlineColor = c; }
    public static void setFilter(Predicate<Entity> f){ filter = f; }

    private static void ensureMaskRegistered() {
        if (mask == null || mask.getColorTexture() == null) return;
        int w = mask.width;
        int h = mask.height;
        if (w <= 0 || h <= 0) return;
        if (w == registeredW && h == registeredH && maskTexture != null) return;

        maskTexture = new MaskTexture(mask);
        var tm = Minecraft.getInstance().getTextureManager();
        tm.register(MASK_TEXTURE_ID, maskTexture);       // Chams käyttää tätä
        tm.register(OUTLINE_TEXTURE_ID, maskTexture);    // Outline käyttää tätä
        registeredW = w;
        registeredH = h;
        LOG.info("[ShaderESP] mask texture registered {}x{}", w, h);
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
                mc.gameRenderer.gameRenderState()
        );
        LOG.info("[ShaderESP] feature dispatcher created");
    }

    /**
     * Kutsutaan LevelRenderer.submitEntities() TAIL:issa.
     *
     * Tässä vaiheessa vanilla on juuri kerännyt kaikki entity-render-statet
     * ja pipeline on täysin elossa (projektio, modelview, kameran positio).
     */
    public static void submitEntities(
            LevelRenderer levelRenderer,
            PoseStack poseStack,           // ← KÄYTÄ TÄTÄ, älä luo uutta
            LevelRenderState levelState,
            EntityRenderDispatcher dispatcher) {

        if (!enabled) return;
        init();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // 1. Kerää kohde-statet
        List<EntityRenderState> targets = new ArrayList<>();
        for (EntityRenderState state : levelState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).axiom$getEntity();
            if (entity == null) continue;
            if (entity == mc.player || !entity.isAlive()) continue;
            if (!filter.test(entity)) continue;
            targets.add(state);
        }

        mask.syncToWindow(downscale);
        mask.clearMask();
        ensureMaskRegistered();
        if (targets.isEmpty()) return;

        // 2. Tyhjennä queue
        queue.getSubmitsPerOrder().clear();

        // 3. Submit jokainen target käyttäen VANILLAN poseStackia
        Vec3 camPos = levelState.cameraRenderState.pos;

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

        if (queue.getSubmitsPerOrder().isEmpty()) return;

        ensureFeatureDispatcher();
        if (featureDispatcher == null) return;

// 4. Vaihda entityOutlineTarget meidän mask-RT:ksi
        ILevelRenderer ilr = (ILevelRenderer) levelRenderer;
        ilr.axiom$pushEntityOutlineFramebuffer(mask);

        try {
            try (FeatureRenderDispatcher.PreparedFrame frame =
                         featureDispatcher.prepareFrame(queue)) {
                if (frame.isEmpty()) return;

                // KRIITTINEN: aseta kameran rotaatio ModelViewStackiin.
                // submitEntities-TAIL:ssa vanilla on jo popannut sen pois,
                // joten asetamme sen uudelleen ennen renderöintiä.
                var modelViewStack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
                modelViewStack.pushMatrix();
                modelViewStack.mul(levelState.cameraRenderState.viewRotationMatrix);
                var encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();

                try {
                    try (var pass = encoder.createRenderPass(
                            () -> "shader_esp_mask",
                            mask.getColorTextureView(),
                            java.util.Optional.empty(),
                            mask.getDepthTextureView(),
                            java.util.OptionalDouble.of(0.0))) {   // ← reverse-Z

                        com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
                        FeatureRenderDispatcher.renderAllFeatures(pass, frame);   // ← kaikki phaset
                    }
                } finally {
                    encoder.submit();
                    modelViewStack.popMatrix();
                }
            }
        } catch (Throwable t) {
            LOG.error("Outline pass failed", t);
        } finally {
            ilr.axiom$popEntityOutlineFramebuffer();
        }
    }

    /**
     * Kutsutaan HUD-renderöinnissä. Piirtää mask-RT:stä outline pää-RT:lle
     * edge-detection -shaderilla.
     */
    public static void compositeToScreen(Renderer2D hud) {
        if (!enabled || mask == null) return;
        ensureMaskRegistered();
        if (mask.getColorTextureView() == null) return;

        var window = Minecraft.getInstance().getWindow();
        int w = window.getGuiScaledWidth();
        int h = window.getGuiScaledHeight();

        // ─── Chams ───
        if (renderChams) {
            hud.drawTexturePart(
                    MASK_TEXTURE_ID,
                    0, 0, w, h,
                    0f, 1f, 1f, 0f,
                    chamsTint);   // esim. 0xFFFFFFFF = alkuperäinen, 0x80FF0000 = punainen 50%
        }

        // ─── Outline ───
        if (renderOutline) {
            hud.drawEntityEdge(
                    mask.getColorTextureView(),
                    mask.sampler(),
                    0, 0, w, h,
                    outlineColor);   // esim. 0xFFFFFFFF = valkoinen
        }
    }

    public static void shutdown() {
        if (featureDispatcher != null) {
            featureDispatcher.close();
            featureDispatcher = null;
        }
    }
}