package silversword.axiom.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
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

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {
    // Shader ESP
    @Unique private ShaderESP esp;
    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    @Unique private final Stack<Handle<Framebuffer>> framebufferHandleStack = new ObjectArrayList<>();
    @Unique
    @Final
    private EntityRenderManager entityRenderManager;

    @Unique
    private final ThreadLocal<Deque<Framebuffer>> framebufferStack = ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Unique
    private VertexConsumerProvider provider;

    @Unique
    private RenderDispatcher renderDispatcher;

    @Unique
    private MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }

    @Override
    public void axiom$pushEntityOutlineFramebuffer(Framebuffer fb) {
        framebufferStack.get().push(this.entityOutlineFramebuffer);
        this.entityOutlineFramebuffer = fb;

        framebufferHandleStack.push(this.framebufferSet.entityOutlineFramebuffer);
        this.framebufferSet.entityOutlineFramebuffer = () -> fb;
    }

    @Override
    public void axiom$popEntityOutlineFramebuffer() {
        Deque<Framebuffer> stack = framebufferStack.get();
        if (!stack.isEmpty()) {
            this.entityOutlineFramebuffer = stack.pop();
            this.framebufferSet.entityOutlineFramebuffer = framebufferHandleStack.pop();
        }
    }




    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix4f2, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        RenderUtils.currentCameraPos = camera.getCameraPos();
        RenderUtils.updateMatrices(projectionMatrix, positionMatrix);
        RenderUtils.updateScreenCenter(projectionMatrix, positionMatrix);
        PostProcessShaders.beginRender();
    }

    @Inject(method = "setWorld", at = @At("TAIL"))
    private void onSetWorld(ClientWorld world, CallbackInfo ci) {
        esp = ModuleManager.getInstance().getModule(ShaderESP.class);
    }

    @ModifyExpressionValue(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;isRenderingReady(Lnet/minecraft/util/math/BlockPos;)Z"))
    boolean fillEntityRenderStatesIsRenderingReady(boolean original) {
        if (esp != null && esp.forceRender()) return true;
        return original;
    }

    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void onPushEntityRenders(MatrixStack matrices, WorldRenderState worldState, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        MinecraftClient mc = getMc();
        if (renderDispatcher == null) {
            renderDispatcher = new RenderDispatcher(
                    outlineRenderCommandQueue,
                    mc.getBlockRenderManager(),
                    new WrapperImmediateVertexConsumerProvider(() -> provider),
                    mc.getAtlasManager(),
                    NoopOutlineVertexConsumerProvider.INSTANCE,
                    NoopImmediateVertexConsumerProvider.INSTANCE,
                    mc.textRenderer
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
    private void draw(WorldRenderState worldState, MatrixStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        if (shader == null || !shader.shouldDrawShader()) return;

        var camera = worldState.cameraRenderState.pos;
        boolean empty = true;

        EntityRenderManager erm = mc.getEntityRenderDispatcher();
        if (erm == null) return;

        for (var state : worldState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).axiom$getEntity();
            if (entity == null) continue;

            if (!shader.shouldDraw(entity)) continue;

            Color color = colorGetter.apply(entity);
            if (color == null) continue;
            outlineRenderCommandQueue.setColor(color);

            var renderer = erm.getRenderer(state);
            var offset = renderer.getPositionOffset(state);

            matrices.push();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.render(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.pop();

            empty = false;
        }

        if (!empty) {
            axiom$pushEntityOutlineFramebuffer(shader.getFramebuffer());
            provider = shader.vertexConsumerProvider;
            try {
                renderDispatcher.render();
            } finally {
                outlineRenderCommandQueue.onNextFrame();
            }
            provider = null;
            axiom$popEntityOutlineFramebuffer();
        } else {
            outlineRenderCommandQueue.onNextFrame();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f matrix4f, Matrix4f projectionMatrix, Matrix4f matrix4f2, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();

        MatrixStack stack = new MatrixStack();
        float delta = tickCounter.getTickProgress(false);
        Vec3d camPos = camera.getCameraPos();

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