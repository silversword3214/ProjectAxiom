package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.mixininterface.ILevelRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.blockchams.BlockChamsRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp.ShaderEspRenderer;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import java.util.Deque;
import java.util.LinkedList;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements ILevelRenderer {

    // ─── Shadow-kentät ─────────────────────────────────────────────

    @Shadow
    @Final
    @Mutable
    private RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    // ─── ILevelRenderer -tila ──────────────────────────────────────

    @Unique
    private final Deque<RenderTarget> axiom$fbStack = new LinkedList<>();

    @Unique
    private final Deque<ResourceHandle<RenderTarget>> axiom$handleStack = new LinkedList<>();

    @Override
    public void axiom$pushEntityOutlineFramebuffer(RenderTarget fb) {
        axiom$fbStack.push(this.entityOutlineTarget);
        this.entityOutlineTarget = fb;

        axiom$handleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> fb;
    }

    @Override
    public void axiom$popEntityOutlineFramebuffer() {
        this.entityOutlineTarget = axiom$fbStack.pop();
        this.targets.entityOutline = axiom$handleStack.pop();
    }

    // ─── 3D-render-event (ESP-moduulin oma piirto) ────────────────

    @Inject(method = "render", at = @At("TAIL"))
    private void axiom$onRenderLevelTail(
            GraphicsResourceAllocator resourceAllocator, boolean renderOutline,
            CameraRenderState cameraState, GpuBufferSlice terrainFog,
            Vector4f fogColor, boolean shouldRenderSky,
            boolean consistentDepthRequired, CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float tickDelta = RenderUtils.getTickDelta();
        Camera camera = mc.gameRenderer.mainCamera();
        Matrix4f projection = RenderUtils.getProjectionMatrix(tickDelta);
        Matrix4f view = RenderUtils.getViewMatrix(camera);

        RenderAPI api = RenderAPI.getInstance();
        Renderer3D renderer = new Renderer3D(api.getCore3D(), projection, view, tickDelta);

        Render3DEvent event = new Render3DEvent(
                renderer, tickDelta, camera.position(), projection, view);

        AxiomInitialize.EVENT_BUS.post(event);
        api.getCore3D().flush();
        api.getCore().flush();
    }

    // Shader ESP: submitEntities TAIL

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void axiom$shaderEspSubmitEntities(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector output,
            CallbackInfo ci) {

        ShaderEspRenderer.submitEntities(
                (LevelRenderer)(Object) this,
                poseStack,
                levelRenderState,
                this.entityRenderDispatcher);

        // Block chams
        BlockChamsRenderer.submitAll(
                (LevelRenderer)(Object) this,
                poseStack,
                levelRenderState,
                Minecraft.getInstance().getBlockEntityRenderDispatcher());

    }

    // ─── Reset-suojaus (säilytetty) ────────────────────────────────

    @Inject(
            method = "resetLevelRenderData()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void axiom$safeResetLevelRenderData(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        axiom$invokeSafeChunkReload((LevelRenderer)(Object) this);
        ci.cancel();
    }

    private static void axiom$invokeSafeChunkReload(LevelRenderer lr) {
        for (String name : new String[]{"invalidateCompiledGeometry", "allChanged"}) {
            try {
                var m = LevelRenderer.class.getDeclaredMethod(name);
                m.setAccessible(true);
                m.invoke(lr);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                System.err.println("[Axiom] LevelRenderer." + name + " failed: " + t);
                return;
            }
        }
    }
}