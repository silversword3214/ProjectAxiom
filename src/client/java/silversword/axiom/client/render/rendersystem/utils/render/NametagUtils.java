package silversword.axiom.client.render.rendersystem.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import org.joml.*;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.Zoom;

public class NametagUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static double scale;

    private static final Vector4f vec4 = new Vector4f();
    private static final Vector4f mmMat4 = new Vector4f();
    private static final Vector4f pmMat4 = new Vector4f();
    private static final Vector3d camera = new Vector3d();
    private static final Vector3d cameraNegated = new Vector3d();
    private static final Matrix4f model = new Matrix4f();
    private static final Matrix4f projection = new Matrix4f();
    private static double windowScale;

    public static void onRender(Matrix4f modelView) {
        model.set(modelView);
        NametagUtils.projection.set(RenderUtils.projection);

        RenderUtils.set(camera, mc.gameRenderer.getMainCamera().position());
        cameraNegated.set(camera);
        cameraNegated.negate();

        windowScale = mc.getWindow().calculateScale(1, false);
    }

    public static boolean worldToScreen(Vector3d pos, double scale) {
        return worldToScreen(pos, scale, true);
    }

    public static boolean worldToScreen(Vector3d pos, double scale, boolean distanceScaling) {
        return worldToScreen(pos, scale, distanceScaling, false);
    }

    public static boolean worldToScreen(Vector3d pos, double scale, boolean distanceScaling, boolean allowBehind) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        NametagUtils.scale = scale * zoom.getScaling();
        if (distanceScaling) {
            NametagUtils.scale *= getScale(pos);
        }

        vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1);

        vec4.mul(model, mmMat4);
        mmMat4.mul(projection, pmMat4);

        boolean behind = pmMat4.w <= 0.f;

        if (behind && !allowBehind) return false;

        toScreen(pmMat4);
        double x = pmMat4.x * mc.getWindow().getWidth();
        double y = pmMat4.y * mc.getWindow().getHeight();

        if (behind) {
            x = mc.getWindow().getWidth() - x;
            y = mc.getWindow().getHeight() - y;
        }

        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;

        pos.set(x / windowScale, mc.getWindow().getHeight() - y / windowScale, allowBehind ? pmMat4.w : pmMat4.z);
        return true;
    }

    public static void begin(Vector3d pos) {
        Matrix4fStack matrices = RenderSystem.getModelViewStack();
        begin(matrices, pos);
    }

    public static void begin(Vector3d pos, GuiGraphics drawContext) {
        begin(pos);

        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        matrices.scale(1.0f / mc.getWindow().getGuiScale());
        matrices.translate((float) pos.x, (float) pos.y);
        matrices.scale((float) scale, (float) scale);
    }

    private static void begin(Matrix4fStack matrices, Vector3d pos) {
        matrices.pushMatrix();
        matrices.translate((float) pos.x, (float) pos.y, 0);
        matrices.scale((float) scale, (float) scale, 1);
    }

    public static void end() {
        RenderSystem.getModelViewStack().popMatrix();
    }

    public static void end(GuiGraphics drawContext) {
        end();
        drawContext.pose().popMatrix();
    }

    private static double getScale(Vector3d pos) {
        double dist = camera.distance(pos);
        return Mth.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }

    private static void toScreen(Vector4f vec) {
        float newW = 1.0f / vec.w * 0.5f;

        vec.x = vec.x * newW + 0.5f;
        vec.y = vec.y * newW + 0.5f;
        vec.z = vec.z * newW + 0.5f;
        vec.w = newW;
    }

    public static Vector3d set(Vector3d vec, Entity entity, double tickDelta) {
        vec.x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        vec.y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        vec.z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());

        return vec;
    }
}