package silversword.axiom.client.hud.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public final class HudContext extends UiContext {

    public HudContext(Minecraft mc, GuiGraphicsExtractor draw, Theme theme,
                      float delta, Renderer2D renderer) {
        super(mc, draw, theme, delta, renderer);
    }


    public void fillRounded(int x, int y, int w, int h, int radius, int argb) {
        renderer.drawRoundedRect(x, y, w, h, radius, argb);
    }

    public void fillRoundedCustom(int x, int y, int w, int h, int radius, int argb,
                                  boolean tl, boolean tr, boolean br, boolean bl) {
        renderer.drawRoundedRectCustom(x, y, w, h, radius, argb, tl, tr, br, bl);
    }

    public void drawRoundedOutline(int x, int y, int w, int h, int radius,
                                   int argb, double thickness) {
        renderer.drawRoundedRectOutline(x, y, w, h, radius, argb, thickness);
    }
}