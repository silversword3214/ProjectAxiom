package silversword.axiom.client.render.rendersystem.utils.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.CustomRenderingPipelineProvider;

import silversword.axiom.client.render.rendersystem.Renderer3D;
import silversword.axiom.client.render.rendersystem.ShapeMode;

import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.misc.Pool;

import java.util.List;

public class RenderUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean rendering3D = true;

    public static Vec3d center = new Vec3d(0, 0, 0);

    public static final Matrix4f projection = new Matrix4f();
    public static final Matrix4f modelView = new Matrix4f();
    public static final Matrix4f view = new Matrix4f();

    private static final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private static final List<RenderBlock> renderBlocks = new ObjectArrayList<>();
    public static final Renderer3D renderer3D = new Renderer3D(CustomRenderingPipelineProvider.WORLD_COLORED_LINES, CustomRenderingPipelineProvider.WORLD_COLORED);


    public static Vec3d currentCameraPos = Vec3d.ZERO;

    public static void init() {
        AxiomInitialize.EVENT_BUS.subscribe(RenderUtils.class);
    }



    public static void updateMatrices(Matrix4f proj, Matrix4f viewMatrix) {
        projection.set(proj);
        modelView.set(view);
        RenderUtils.view.set(viewMatrix);
    }

    public static void setup2DProjection(int width, int height) {
        projection.setOrtho(0, width, height, 0, 0, 1000);
    }

    public static void updateScreenCenter(Matrix4f projection, Matrix4f view) {
        RenderUtils.projection.set(projection);

        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();

        Vector4f center4 = new Vector4f(0, 0, 0, 1).mul(invProjection).mul(invView);
        center4.div(center4.w);

        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        center = new Vec3d(camera.x + center4.x, camera.y + center4.y, camera.z + center4.z);
    }

    public static void unscaledProjection() {
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();

        // near = -1000, far = 1000 (z=0 on välissä)
        projection.setOrtho(0, width, height, 0, 0, 1000);
        rendering3D = false;
    }

    public static void scaledProjection() {
        float width = (float) (mc.getWindow().getFramebufferWidth() / mc.getWindow().getScaleFactor());
        float height = (float) (mc.getWindow().getFramebufferHeight() / mc.getWindow().getScaleFactor());

        float tickDelta = mc.getRenderTickCounter().getDynamicDeltaTicks();
        projection.set(mc.gameRenderer.getBasicProjectionMatrix(tickDelta));
        rendering3D = true;
    }

    public static void renderTickingBlock(BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink) {
        renderBlocks.removeIf(next -> {
            if (next.pos.equals(blockPos)) {
                renderBlockPool.free(next);
                return true;
            }
            return false;
        });

        renderBlocks.add(renderBlockPool.get().set(blockPos, sideColor, lineColor, shapeMode, excludeDir, duration, fade, shrink));
    }

    public static void onTick() {
        renderBlocks.removeIf(block -> {
            block.ticks--;
            if (block.ticks <= 0) {
                renderBlockPool.free(block);
                return true;
            }
            return false;
        });
    }

    @AxiomEvent
    private static void onRender(Render3DEvent event) {
        // TÄRKEÄÄ: Box-renderöinti vaatii puskurin avaamisen ja sulkemisen tässä
        // Jos Tracers jo avaa sen, tämä saattaa aiheuttaa "begin() called twice"
        // Parempi käyttää event.renderer.box suoraan jos mahdollista
        renderBlocks.forEach(block -> block.render(event));
    }

    public static class RenderBlock {
        public final BlockPos.Mutable pos = new BlockPos.Mutable();
        public final Color sideColor = new Color();
        public final Color lineColor = new Color();
        public ShapeMode shapeMode;
        public int excludeDir;
        public int ticks, duration;
        public boolean fade, shrink;

        public RenderBlock set(BlockPos blockPos, Color side, Color line, ShapeMode mode, int exclude, int dur, boolean f, boolean s) {
            pos.set(blockPos);
            sideColor.set(side);
            lineColor.set(line);
            shapeMode = mode;
            excludeDir = exclude;
            fade = f;
            shrink = s;
            ticks = dur;
            duration = dur;
            return this;
        }

        public void render(Render3DEvent event) {
            double d = (double) ticks / duration;
            double x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ();
            double x2 = pos.getX() + 1, y2 = pos.getY() + 1, z2 = pos.getZ() + 1;

            int sideA = sideColor.a;
            int lineA = lineColor.a;

            if (fade) {
                sideColor.a *= d;
                lineColor.a *= d;
            }
            if (shrink) {
                double offset = (1.0 - d) / 2.0;
                x1 += offset; y1 += offset; z1 += offset;
                x2 -= offset; y2 -= offset; z2 -= offset;
            }

            // Käytetään eventin omaa rendereriä (Renderer3D)
            event.render.drawBox(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, excludeDir);

            sideColor.a = sideA;
            lineColor.a = lineA;
        }

    }

    public static Vector3d set(Vector3d vec, Vec3d v) {
        vec.x = v.x;
        vec.y = v.y;
        vec.z = v.z;

        return vec;
    }
}