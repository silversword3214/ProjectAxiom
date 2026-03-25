package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

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
        Vec3 pos = camera.position();
        Quaternionf rot = camera.rotation();
        return new Matrix4f()
                .rotate(rot.conjugate())
                .translate(-(float) pos.x, -(float) pos.y, -(float) pos.z);
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