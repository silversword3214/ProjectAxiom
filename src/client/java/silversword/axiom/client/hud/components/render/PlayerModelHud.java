package silversword.axiom.client.hud.components.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.render.PlayerModelModule;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureRegion;

import java.lang.reflect.Field;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class PlayerModelHud extends BaseHudElement {
    private final PlayerModelModule module;

    private int width = 80;
    private int height = 160;
    private float fov = 70;
    private float cameraHeight = 1.5f;
    private float cameraDistance = 4.0f;
    private float cameraSide = 0.0f;
    private float renderDistance = 64;
    private boolean showBorder = true;
    private int borderColor = 0xFFFFFFFF;

    private String cameraMode = "BEHIND";    // BEHIND, FRONT, LEFT, RIGHT
    private String lookAtMode = "PLAYER";    // PLAYER, FORWARD, BACKWARD

    private Framebuffer playerModelFramebuffer;

    private static final int BASE_CORNER_RADIUS = 4;
    private static final int BASE_BG_COLOR = 0x80000000;

    private static Field cameraField;

    static {
        try {
            cameraField = GameRenderer.class.getDeclaredField("camera");
            cameraField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public PlayerModelHud(PlayerModelModule module) {
        super("PlayerModel", 10, 10);
        this.module = module;
    }

    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public void setFov(float f) { fov = f; }
    public void setCameraHeight(float h) { cameraHeight = h; }
    public void setCameraDistance(float d) { cameraDistance = d; }
    public void setCameraSide(float s) { cameraSide = s; }
    public void setRenderDistance(float d) { renderDistance = d; }
    public void setShowBorder(boolean b) { showBorder = b; }
    public void setBorderColor(int c) { borderColor = c; }
    public void setCameraMode(String mode) { cameraMode = mode; }
    public void setLookAtMode(String mode) { lookAtMode = mode; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(MinecraftClient mc) { return width; }
    @Override public int height(MinecraftClient mc) { return height; }

    private void ensureFramebuffer() {
        if (playerModelFramebuffer == null || playerModelFramebuffer.textureWidth != width || playerModelFramebuffer.textureHeight != height) {
            if (playerModelFramebuffer != null) {
                playerModelFramebuffer.delete();
            }
            playerModelFramebuffer = new SimpleFramebuffer("PlayerModel", width, height, true);
        }
    }

    public void renderWorldToFramebuffer(RenderTickCounter tickCounter) {
        if (!this.enabled() || mc.world == null || mc.player == null) return;

        ensureFramebuffer();

        GameRenderer gameRenderer = mc.gameRenderer;
        float tickDelta = tickCounter.getTickProgress(true);

        PlayerCamera playerCamera = new PlayerCamera();
        playerCamera.update(mc.world, mc.player, true, false, tickDelta); // thirdPerson = true

        // Määritä offset kameran moodin mukaan
        float surge, heave, sway;
        switch (cameraMode) {
            case "BEHIND":
                surge = -cameraDistance;
                heave = cameraHeight;
                sway = cameraSide;
                break;
            case "FRONT":
                surge = cameraDistance;
                heave = cameraHeight;
                sway = cameraSide;
                break;
            case "LEFT":
                surge = 0;
                heave = cameraHeight;
                sway = -cameraDistance;
                break;
            case "RIGHT":
                surge = 0;
                heave = cameraHeight;
                sway = cameraDistance;
                break;
            default:
                surge = -cameraDistance;
                heave = cameraHeight;
                sway = cameraSide;
        }

        // Siirrä kameraa moveBy:llä
        playerCamera.moveBy(surge, heave, sway);

        // Määritä mihin kamera katsoo ja aseta rotaatio
        Vec3d cameraPos = playerCamera.getCameraPos();
        Vec3d lookTarget;

        switch (lookAtMode) {
            case "PLAYER":
                lookTarget = mc.player.getEyePos();
                break;
            case "FORWARD":
                lookTarget = mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(10));
                break;
            case "BACKWARD":
                lookTarget = mc.player.getEyePos().subtract(mc.player.getRotationVec(1.0f).multiply(10));
                break;
            default:
                lookTarget = mc.player.getEyePos();
        }

        double dx = lookTarget.x - cameraPos.x;
        double dy = lookTarget.y - cameraPos.y;
        double dz = lookTarget.z - cameraPos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.asin(dy / Math.sqrt(dx*dx + dy*dy + dz*dz)));
        playerCamera.setRotation(yaw, pitch);

        // Vaihda kamera GameRendererissä
        Camera originalCamera = null;
        try {
            originalCamera = (Camera) cameraField.get(gameRenderer);
            cameraField.set(gameRenderer, playerCamera);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        // Aseta projektiomatriisi
        float aspect = (float) width / (float) height;
        Matrix4f projMatrix = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05f, renderDistance * 2);
        GpuBufferSlice projSlice = gameRenderer.worldProjectionMatrix.set(projMatrix);
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.PERSPECTIVE);

        // Tallenna ja aseta overridet
        GpuTextureView originalColorOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView originalDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = playerModelFramebuffer.getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = playerModelFramebuffer.getDepthAttachmentView();

        // Tyhjennä framebuffer
        var device = RenderSystem.getDevice();
        var encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(
                playerModelFramebuffer.getColorAttachment(),
                0,
                playerModelFramebuffer.getDepthAttachment(),
                1.0
        );

        // Renderöi maailma
        gameRenderer.renderWorld(tickCounter);

        // Palauta
        RenderSystem.outputColorTextureOverride = originalColorOverride;
        RenderSystem.outputDepthTextureOverride = originalDepthOverride;

        try {
            cameraField.set(gameRenderer, originalCamera);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(HudContext ctx, RenderTickCounter tickCounter) {
        if (mc.player == null || mc.world == null) return;

        Color bgColor = new Color(BASE_BG_COLOR);
        Renderer2D.COLOR.drawRoundedRect(x, y, width, height, BASE_CORNER_RADIUS, bgColor);

        if (playerModelFramebuffer != null && playerModelFramebuffer.getColorAttachmentView() != null) {
            TextureRegion region = new TextureRegion(1.0, 1.0);
            region.x1 = 1.0f;
            region.y1 = 1.0f;
            region.x2 = 0.0f;
            region.y2 = 0.0f;

            Renderer2D.TEXTURE.texQuad(x, y, width, height, region, Color.WHITE);

            var sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            Renderer2D.TEXTURE.render(playerModelFramebuffer.getColorAttachmentView(), sampler);
        }

        if (showBorder) {
            Color borderCol = new Color(borderColor);
            Renderer2D.COLOR.drawRoundedRectOutline(x, y, width, height, BASE_CORNER_RADIUS, borderCol, 1.0);
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Color bgColor = new Color(BASE_BG_COLOR);
        Renderer2D.COLOR.drawRoundedRect(x, y, width, height, BASE_CORNER_RADIUS, bgColor);
        if (showBorder) {
            Renderer2D.COLOR.drawRoundedRectOutline(x, y, width, height, BASE_CORNER_RADIUS, new Color(borderColor), 1.0);
        }
        ctx.drawScaledText("Player Model", x + 4, y + 4, 0xFFFFFFFF, true, 0.8f);
    }
}