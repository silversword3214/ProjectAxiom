package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class NametagUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Vec3 worldToScreen(Vec3 worldPos, int screenW, int screenH) {
        Camera camera = mc.gameRenderer.mainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        Matrix4f bobCorrection = RenderUtils.getBobCorrection(tickDelta);

        Matrix4f view = new Matrix4f();
        view.mul(bobCorrection);

        Quaternionf camRotCopy = new Quaternionf(camera.rotation());
        camRotCopy.conjugate();
        view.rotate(camRotCopy);

        Vec3 camPos = camera.position();
        view.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

        Matrix4f proj = RenderUtils.getProjectionMatrix(tickDelta);

        Vector4f clip = new Vector4f(
                (float) worldPos.x,
                (float) worldPos.y,
                (float) worldPos.z,
                1.0f);

        clip.mul(view);
        clip.mul(proj);

        if (clip.w <= 0.0f) return null;

        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;

        float screenX = (ndcX * 0.5f + 0.5f) * screenW;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenH;

        return new Vec3(screenX, screenY, clip.w);
    }
}