package silversword.axiom.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.mixininterface.IEntityRenderState;
import silversword.axiom.client.mixininterface.IWorldRenderer;
import silversword.axiom.client.modules.render.Chams;
import silversword.axiom.client.modules.render.ShaderESP;
import silversword.axiom.client.render.rendersystem.utils.OutlineRenderCommandQueue;
import silversword.axiom.client.render.rendersystem.utils.WrapperImmediateVertexConsumerProvider;
import silversword.axiom.client.render.rendersystem.utils.NoopOutlineVertexConsumerProvider;
import silversword.axiom.client.render.rendersystem.utils.NoopImmediateVertexConsumerProvider;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.postprocess.EntityShader;
import silversword.axiom.client.render.rendersystem.utils.postprocess.PostProcessShaders;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

import static silversword.axiom.client.main.AxiomInitialize.mc;
import static silversword.axiom.client.render.rendersystem.utils.postprocess.PostProcessShaders.ENTITY_OUTLINE;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements IWorldRenderer {
    // Shader ESP
    @Unique private ShaderESP esp;
    @Shadow
    private RenderTarget entityOutlineTarget;

    @Shadow @Final private LevelTargetBundle targets;
    @Unique private final Stack<ResourceHandle<RenderTarget>> framebufferHandleStack = new ObjectArrayList<>();
    @Unique
    @Final
    private EntityRenderDispatcher entityRenderManager;

    @Unique
    private final ThreadLocal<Deque<RenderTarget>> framebufferStack = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Unique
    private MultiBufferSource provider;

    @Unique
    private FeatureRenderDispatcher renderDispatcher;

    @Unique
    private Minecraft getMc() {
        return Minecraft.getInstance();
    }

    @Override
    public void axiom$pushEntityOutlineFramebuffer(RenderTarget fb) {
        framebufferStack.get().push(this.entityOutlineTarget);
        this.entityOutlineTarget = fb;

        framebufferHandleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> fb;
    }

    @Override
    public void axiom$popEntityOutlineFramebuffer() {
        Deque<RenderTarget> stack = framebufferStack.get();
        if (!stack.isEmpty()) {
            this.entityOutlineTarget = stack.pop();
            this.targets.entityOutline = framebufferHandleStack.pop();
        }
    }




    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderHead(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix4f2, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        RenderUtils.currentCameraPos = camera.position();
        RenderUtils.updateMatrices(projectionMatrix, positionMatrix);
        RenderUtils.updateScreenCenter(projectionMatrix, positionMatrix);
        PostProcessShaders.beginRender();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetWorld(ClientLevel world, CallbackInfo ci) {
        esp = ModuleManager.getInstance().getModule(ShaderESP.class);
    }

    @ModifyExpressionValue(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    boolean fillEntityRenderStatesIsRenderingReady(boolean original) {
        if (esp != null && esp.forceRender()) return true;
        return original;
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onPushEntityRenders(PoseStack matrices, LevelRenderState worldState, SubmitNodeCollector queue, CallbackInfo ci) {
        Minecraft mc = getMc();
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                    outlineRenderCommandQueue,
                    mc.getBlockRenderer(),
                    new WrapperImmediateVertexConsumerProvider(() -> provider),
                    mc.getAtlasManager(),
                    NoopOutlineVertexConsumerProvider.INSTANCE,
                    NoopImmediateVertexConsumerProvider.INSTANCE,
                    mc.font
            );
        }

        if (PostProcessShaders.CHAMS != null) {
            draw(worldState, matrices, PostProcessShaders.CHAMS, entity -> {
                Chams chams = ModuleManager.getInstance().getModule(Chams.class);
                if (chams != null && chams.isEnabled()) {
                    return new Color(255, 255, 255, 255);
                }
                return null;
            });
        }

        draw(worldState, matrices, ENTITY_OUTLINE, entity -> {
            ShaderESP shaderESP = ModuleManager.getInstance().getModule(ShaderESP.class);
            if (shaderESP != null && shaderESP.isShader()) {
                return shaderESP.getColor(entity);
            }
            return null;
        });
    }

    @Unique
    private void draw(LevelRenderState worldState, PoseStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        if (shader == null || !shader.shouldDrawShader()) return;

        var camera = worldState.cameraRenderState.pos;
        boolean empty = true;

        EntityRenderDispatcher erm = mc.getEntityRenderDispatcher();
        if (erm == null) return;

        for (var state : worldState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).axiom$getEntity();
            if (entity == null) continue;

            if (!shader.shouldDraw(entity)) continue;

            Color color = colorGetter.apply(entity);
            if (color == null) continue;
            outlineRenderCommandQueue.setColor(color);

            var renderer = erm.getRenderer(state);
            var offset = renderer.getRenderOffset(state);

            matrices.pushPose();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.submit(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.popPose();

            empty = false;
        }

        if (!empty) {
            axiom$pushEntityOutlineFramebuffer(shader.getFramebuffer());
            provider = shader.vertexConsumerProvider;
            try {
                renderDispatcher.renderAllFeatures();
            } finally {
                outlineRenderCommandQueue.endFrame();
            }
            provider = null;
            axiom$popEntityOutlineFramebuffer();
        } else {
            outlineRenderCommandQueue.endFrame();
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRender(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f matrix4f, Matrix4f projectionMatrix, Matrix4f matrix4f2, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();

        PoseStack stack = new PoseStack();
        float delta = tickCounter.getGameTimeDeltaPartialTick(false);
        Vec3 camPos = camera.position();

        Render3DEvent event = Render3DEvent.get(
                stack,
                RenderUtils.renderer3D,
                null,
                delta,
                camPos.x, camPos.y, camPos.z
        );

        event.cameraX = camPos.x;
        event.cameraY = camPos.y;
        event.cameraZ = camPos.z;

        if (event.render != null) {
            event.render.begin();
            try {
                AxiomInitialize.EVENT_BUS.post(event);
                event.render.render(stack);
            } finally {
                event.render.lines.end();
                event.render.triangles.end();
            }
        }
    }
}