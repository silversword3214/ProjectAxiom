package silversword.axiom.client.event.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.RenderEvent;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;

public class Render3DEvent extends RenderEvent {
    private final Renderer3D renderer;
    private final Vec3 cameraPos;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;

    public Render3DEvent(Renderer3D renderer, float tickDelta, Vec3 cameraPos, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        super(tickDelta);
        this.renderer = renderer;
        this.cameraPos = cameraPos;
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;
    }

    public Renderer3D getRenderer() {
        return renderer;
    }

    public Vec3 getCameraPos() {
        return cameraPos;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
}