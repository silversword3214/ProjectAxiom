package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.ShaderESP;

import java.util.List;
public final class ShaderRenderer implements AutoCloseable {
    private static final ShaderRenderer INSTANCE = new ShaderRenderer();

    private final Minecraft mc = Minecraft.getInstance();
    private final ShaderFramebufferManager framebufferManager = new ShaderFramebufferManager();
    private final ShaderPostProcessor postProcessor = new ShaderPostProcessor();
    private final ShaderMaskBufferSource maskBufferSource = new ShaderMaskBufferSource();
    private final ShaderOutlineBufferSource outlineBufferSource = new ShaderOutlineBufferSource(maskBufferSource);
    private SubmitNodeStorage submitNodes;
    private FeatureRenderDispatcher featureRenderDispatcher;


    private ShaderRenderer() {}

    public static ShaderRenderer getInstance() {
        return INSTANCE;
    }

    public ShaderFramebufferManager getFramebufferManager() {
        return framebufferManager;
    }

    public void render(Camera camera, float tickDelta) {
        ShaderESP module = ModuleManager.getInstance().getModule(ShaderESP.class);
        if (module == null || !module.isEnabled() || mc.level == null || camera == null) return;

        List<Entity> targets = module.collectTargets(camera.position());
        if (targets.isEmpty()) return;

        framebufferManager.ensureTargets(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        RenderTarget maskTarget = framebufferManager.getMaskTarget();
        RenderTarget postTarget = framebufferManager.getPostTarget();
        if (maskTarget == null || postTarget == null) return;

        renderMask(maskTarget, camera, tickDelta, targets);
        postProcessor.run(maskTarget, postTarget, module);
        postProcessor.composite(postTarget, mc.getMainRenderTarget());
    }

    private void renderMask(RenderTarget maskTarget, Camera camera, float tickDelta, List<Entity> targets) {
        ensureFeatureDispatcher();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.clearColorAndDepthTextures(maskTarget.getColorTexture(), 0x00000000, maskTarget.getDepthTexture(), 1.0f);

        submitNodes.clear();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.prepare(camera, camera.entity());

        CameraRenderState cameraState = new CameraRenderState();
        cameraState.initialized = true;
        cameraState.pos = camera.position();
        cameraState.orientation = camera.rotation();
        cameraState.entityPos = camera.entity().position();

        Vec3 cameraPos = camera.position();
        for (Entity entity : targets) {
            var renderState = dispatcher.extractEntity(entity, tickDelta);

            double x = Mth.lerp(tickDelta, entity.xOld, entity.getX()) - cameraPos.x;
            double y = Mth.lerp(tickDelta, entity.yOld, entity.getY()) - cameraPos.y;
            double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ()) - cameraPos.z;

            dispatcher.submit(renderState, cameraState, x, y, z, new PoseStack(), submitNodes);
        }

        featureRenderDispatcher.renderAllFeatures();
        maskBufferSource.endBatch();
        featureRenderDispatcher.endFrame();
    }

    private void ensureFeatureDispatcher() {
        if (featureRenderDispatcher != null) return;

        submitNodes = new SubmitNodeStorage();
        featureRenderDispatcher = new FeatureRenderDispatcher(
                submitNodes,
                mc.getBlockRenderer(),
                maskBufferSource,
                mc.getModelManager().atlasManager,
                outlineBufferSource,
                maskBufferSource,
                mc.font
        );
    }

    @Override
    public void close() {
        framebufferManager.close();
        postProcessor.close();
        if (featureRenderDispatcher != null) {
            featureRenderDispatcher.close();
            featureRenderDispatcher = null;
        }
        maskBufferSource.close();
    }
}
