package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.Phase;
import silversword.axiom.client.modules.render.NoHurtCam;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;

public class RenderUtils {

    public static Vec3 center = Vec3.ZERO;

    public static final Matrix4f projection = new Matrix4f();


    /**
     * Gets the current game's projection matrix for world rendering.
     *
     * @param tickDelta The partial tick time (from DeltaTracker)
     * @return The projection matrix
     */

    public static Matrix4f getProjectionMatrix(float tickDelta) {
        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        Camera camera = gameRenderer.getMainCamera();
        float fov = gameRenderer.getFov(camera, tickDelta, true);
        return gameRenderer.getProjectionMatrix(fov);
    }

    public static Matrix4f getViewMatrix(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        float tickDelta = getTickDelta();

        Matrix4f matrix = new Matrix4f();
        Matrix4f bobCorrection = new Matrix4f();

        NoViewBobbingTilt bobMod = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        NoHurtCam hurtMod = ModuleManager.getInstance().getModule(NoHurtCam.class);
        Phase phaseMod = ModuleManager.getInstance().getModule(Phase.class);

        boolean bobbingCancelledByMod = (bobMod != null && bobMod.isEnabled());

        boolean isHurtTiltCancelled = (hurtMod != null && hurtMod.isEnabled()) ||
                (bobMod != null && bobMod.isEnabled() && bobMod.disableHurtTilt.get()) ||
                (phaseMod != null && phaseMod.isEnabled());

        if (mc.getCameraEntity() instanceof net.minecraft.client.player.AbstractClientPlayer player) {

            if (mc.options.bobView().get() && !bobbingCancelledByMod) {
                var state = player.avatarState();
                float g = state.getBackwardsInterpolatedWalkDistance(tickDelta);
                float h = state.getInterpolatedBob(tickDelta);

                float translateX = (float)Math.sin(g * (float)Math.PI) * h * 0.5F;
                float translateY = Math.abs((float)Math.cos(g * (float)Math.PI) * h);
                float rotateZ = (float)Math.sin(g * (float)Math.PI) * h * 3.0F;
                float rotateX = Math.abs((float)Math.cos(g * (float)Math.PI - 0.2F) * h) * 5.0F;

                bobCorrection.rotateX(rotateX * 0.017453292F);
                bobCorrection.rotateZ(rotateZ * 0.017453292F);
                bobCorrection.translate(translateX, -translateY, 0.0F);
            }

            if (mc.options.damageTiltStrength().get() > 0 && !isHurtTiltCancelled) {
                float g = (float)player.hurtTime - tickDelta;

                if (g >= 0.0F) {
                    g /= (float)player.hurtDuration;
                    g = net.minecraft.util.Mth.sin(g * g * g * g * (float)Math.PI);
                    float h = player.getHurtDir();
                    float i = (float)((double)(-g) * 14.0D * mc.options.damageTiltStrength().get());

                    // REVERSE: Kumotaan pystysuuntainen kallistus
                    bobCorrection.rotateY(h * 0.017453292F);
                    bobCorrection.rotateZ(i * 0.017453292F);
                    bobCorrection.rotateY(h * 0.017453292F);
                }
            }
        }

        // 2. YHDISTÄMINEN
        matrix.mul(bobCorrection);
        matrix.rotate(camera.rotation().conjugate());

        net.minecraft.world.phys.Vec3 pos = camera.position();
        matrix.translate(-(float) pos.x, -(float) pos.y, -(float) pos.z);

        return matrix;
    }

    public static float getTickDelta() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    // Screen center
    public static void updateScreenCenter(Matrix4f projection, Matrix4f view) {
        Matrix4f invProj = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();
        Vector4f center4 = new Vector4f(0, 0, 0, 1).mul(invProj).mul(invView);
        center4.div(center4.w);
        center = new Vec3(center4.x, center4.y, center4.z);
    }

    public static Matrix4f getScaledProjection(GuiGraphics graphics) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        return new Matrix4f().setOrtho(0, w, h, 0, -1000, 1000);
    }

    public static Matrix4f getUnscaledProjection() {
        var window = Minecraft.getInstance().getWindow();
        int w = window.getWidth();
        int h = window.getHeight();
        return new Matrix4f().setOrtho(0, w, h, 0, -1000, 1000);
    }
}