package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;

import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void axiom$onRenderLevelTail(
            GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        float tickDelta = RenderUtils.getTickDelta();

        Camera camera = mc.gameRenderer.mainCamera();

        Matrix4f projection = RenderUtils.getProjectionMatrix(tickDelta);
        Matrix4f view = RenderUtils.getViewMatrix(camera);

        RenderAPI api = RenderAPI.getInstance();

        Renderer3D renderer = new Renderer3D(
                api.getCore(),
                projection,
                view,
                tickDelta
        );

        Render3DEvent event = new Render3DEvent(
                renderer,
                tickDelta,
                camera.position(),
                projection,
                view
        );

        AxiomInitialize.EVENT_BUS.post(event);

        api.end();
    }

    @Inject(
            method = "resetLevelRenderData()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$safeResetLevelRenderData(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        axiom$invokeSafeChunkReload((LevelRenderer) (Object) this);
        ci.cancel();
    }

    private static void axiom$invokeSafeChunkReload(LevelRenderer lr) {
        for (String name : new String[] { "invalidateCompiledGeometry", "allChanged" }) {
            try {
                java.lang.reflect.Method m = LevelRenderer.class.getDeclaredMethod(name);
                m.setAccessible(true);
                m.invoke(lr);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                System.err.println("[Axiom] LevelRenderer." + name + " failed: " + t);
                return;
            }
        }
        System.err.println("[Axiom] No safe chunk-reload method found on LevelRenderer. "
                + "resetLevelRenderData() was cancelled to prevent a crash.");
    }
}

