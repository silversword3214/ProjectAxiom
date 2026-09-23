package silversword.axiom.client.event.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class Render2DEvent extends RenderEvent {
    private final Renderer2D renderer;
    private final GuiGraphicsExtractor guiGraphics;
    private final int screenWidth;
    private final int screenHeight;

    public Render2DEvent(Renderer2D renderer, float tickDelta, GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight) {
        super(tickDelta);
        this.renderer = renderer;
        this.guiGraphics = guiGraphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public Renderer2D getRenderer() {
        return renderer;
    }

    public GuiGraphicsExtractor getGuiGraphics() {
        return guiGraphics;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}