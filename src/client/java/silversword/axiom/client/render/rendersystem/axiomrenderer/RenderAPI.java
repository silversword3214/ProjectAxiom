package silversword.axiom.client.render.rendersystem.axiomrenderer;


import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

public class RenderAPI {
    private static final RenderAPI INSTANCE = new RenderAPI();

    public final RenderCore core;
    private Renderer2D renderer2D;
    private Renderer3D renderer3D;

    private RenderAPI() {
        core = new RenderCore();
    }

    public static RenderAPI getInstance() {
        return INSTANCE;
    }

    public void beginHUDUnscaled(GuiGraphics graphics, float tickDelta) {
        var window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        renderer2D = new Renderer2D(graphics, core, proj);
    }

    public void end() {
        core.flush();
    }

    public Renderer2D hud() {
        if (renderer2D == null) {
            throw new IllegalStateException("beginHUD() not called");
        }
        return renderer2D;
    }

    public Renderer3D world() {
        if (renderer3D == null) {
            throw new IllegalStateException("beginWorld() not called");
        }
        return renderer3D;
    }

    public void close() {
        core.close();
    }

    public RenderCore getCore() {
        return core;
    }
}