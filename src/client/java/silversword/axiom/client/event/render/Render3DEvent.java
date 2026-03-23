package silversword.axiom.client.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import silversword.axiom.client.render.rendersystem.Renderer3D;


public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();

    public PoseStack matrices;
    public Renderer3D render;
    public Renderer3D depthRender;
    public float tickDelta;
    public double offsetX, offsetY, offsetZ;

    public double cameraX;
    public double cameraY;
    public double cameraZ;

    public static Render3DEvent get(PoseStack matrices, Renderer3D renderer, Renderer3D depthRenderer, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.render = renderer;
        INSTANCE.depthRender = depthRenderer;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;

        if (renderer != null) renderer.begin();
        if (depthRenderer != null) depthRenderer.begin();

        return INSTANCE;
    }
}