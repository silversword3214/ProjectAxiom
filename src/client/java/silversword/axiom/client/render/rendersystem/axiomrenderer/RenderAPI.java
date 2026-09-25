package silversword.axiom.client.render.rendersystem.axiomrenderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;

public class RenderAPI {
    private static final RenderAPI INSTANCE = new RenderAPI();

    /** HUD + GUI + world-to-screen overlays. Flushataan LevelRendererin TAIL:issa. */
    public final RenderCore core2D;
    /** 3D-maailma (ESP, chams). Flushataan LevelRendererin TAIL:issa. */
    public final RenderCore core3D;

    private Renderer2D renderer2D;
    private Renderer3D renderer3D;

    private RenderAPI() {
        core2D = new RenderCore();
        core3D = new RenderCore();
    }

    public static RenderAPI getInstance() { return INSTANCE; }

    public RenderCore getCore()   { return core2D; }
    public RenderCore getCore3D() { return core3D; }

    public void beginHUDUnscaled(GuiGraphicsExtractor graphics, float tickDelta) {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        renderer2D = new Renderer2D(graphics, core2D, proj);
    }

    public Renderer2D hud() {
        if (renderer2D == null) throw new IllegalStateException("beginHUD() not called");
        return renderer2D;
    }

    public Renderer3D world() {
        if (renderer3D == null) throw new IllegalStateException("beginWorld() not called");
        return renderer3D;
    }

    public void close() {
        core2D.close();
        core3D.close();
    }
}