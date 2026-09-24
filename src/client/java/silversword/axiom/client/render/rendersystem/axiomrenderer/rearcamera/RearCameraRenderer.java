package silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RearCameraRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("Axiom/RearCamera");

    public static final Identifier HUD_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("projectaxiom", "rearcamera_hud");

    private static RearCameraRenderTarget rearTarget;
    private static RearCameraTexture hudTexture;   // ← UUSI
    private static boolean hudRegistered = false;  // ← UUSI
    private static RenderTarget renderTargetOverride = null;
    private static boolean enabled = false;
    private static boolean rendering = false;

    private static int updateHz = 20;
    private static boolean invertPitch = true;
    private static long lastRenderMs = 0L;

    public static void init() {
        if (rearTarget == null) {
            rearTarget = new RearCameraRenderTarget();
            hudRegistered = false;
        }
    }

    private static void ensureHudTextureRegistered() {
        if (hudRegistered) return;
        if (rearTarget == null || rearTarget.getColorTexture() == null) return;

        hudTexture = new RearCameraTexture(rearTarget);
        Minecraft.getInstance().getTextureManager()
                .register(HUD_TEXTURE_ID, hudTexture);
        hudRegistered = true;
        LOG.info("RearCamera HUD texture registered: {}", HUD_TEXTURE_ID);
    }

    public static void renderRearView() {
        if (!enabled || rendering) return;

        long now = System.currentTimeMillis();
        long minInterval = 1000L / Math.max(1, updateHz);
        if (now - lastRenderMs < minInterval) return;
        lastRenderMs = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        rendering = true;
        LocalPlayer player = mc.player;
        GameRenderer gr = mc.gameRenderer;

        float savedYRot      = player.getYRot();
        float savedXRot      = player.getXRot();
        float savedYRotO     = player.yRotO;
        float savedXRotO     = player.xRotO;
        float savedYHeadRot  = player.yHeadRot;
        float savedYHeadRotO = player.yHeadRotO;
        float savedBodyRot   = player.yBodyRot;
        float savedBodyRotO  = player.yBodyRotO;

        try {
            rearTarget.syncToWindow();
            rearTarget.clear();
            ensureHudTextureRegistered();   // ← UUSI

            float newXRot = invertPitch ? -savedXRot : savedXRot;
            float newYRot = (savedYRot + 180.0f) % 360.0f;

            player.setYRot(newYRot);
            player.setXRot(newXRot);
            player.yRotO      = newYRot;
            player.xRotO      = newXRot;
            player.yHeadRot   = newYRot;
            player.yHeadRotO  = newYRot;
            player.yBodyRot   = newYRot;
            player.yBodyRotO  = newYRot;

            renderTargetOverride = rearTarget;

            gr.update(DeltaTracker.ONE);
            gr.extract(DeltaTracker.ONE, true);
            gr.renderLevel();

        } catch (Throwable t) {
            LOG.error("Rear camera render failed", t);
        } finally {
            renderTargetOverride = null;

            player.setYRot(savedYRot);
            player.setXRot(savedXRot);
            player.yRotO      = savedYRotO;
            player.xRotO      = savedXRotO;
            player.yHeadRot   = savedYHeadRot;
            player.yHeadRotO  = savedYHeadRotO;
            player.yBodyRot   = savedBodyRot;
            player.yBodyRotO  = savedBodyRotO;

            rendering = false;
        }
    }

    public static RenderTarget getRenderTargetOverride() { return renderTargetOverride; }
    public static RearCameraRenderTarget getRearTarget()  { return rearTarget; }

    public static void setUpdateHz(int hz) { updateHz = Math.max(1, Math.min(120, hz)); }
    public static void setInvertPitch(boolean v) { invertPitch = v; }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }
}