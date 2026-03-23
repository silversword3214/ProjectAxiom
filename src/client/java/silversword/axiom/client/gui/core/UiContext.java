package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class UiContext {
    public final Minecraft mc;
    public final GuiGraphics draw;
    public final Theme theme;
    public final float delta;
    private final TextRenderer uiText;
    private final Renderer2D renderer;

    public UiContext(Minecraft mc, GuiGraphics draw, Theme theme, float delta, Renderer2D renderer) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;
        this.uiText = TextRenderer.get();
        this.renderer = renderer;
    }

    public void fill(Rect r, int argb) {
        renderer.drawRect(r.x, r.y, r.w, r.h, argb);
    }

    public void fill(int x, int y, int w, int h, int argb) {
        renderer.drawRect(x, y, w, h, argb);
    }

    public void fillRounded(Rect r, int argb, double radius) {
        renderer.drawRoundedRect(r.x, r.y, r.w, r.h, radius, argb);
    }

    public void fillRounded(int x, int y, int w, int h, int argb, double radius) {
        renderer.drawRoundedRect(x, y, w, h, radius, argb);
    }

    public void fillRoundedCustom(Rect r, int argb, double radius,
                                  boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        renderer.drawRoundedRectCustom(r.x, r.y, r.w, r.h, radius, argb,
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void drawRoundedOutline(Rect r, int argb, double radius, double thickness) {
        renderer.drawRoundedRectOutline(r.x, r.y, r.w, r.h, radius, argb, thickness);
    }

    public void drawRectOutline(Rect r, int argb, double thickness) {
        renderer.drawRectOutline(r.x, r.y, r.w, r.h, (float) thickness, argb);
    }

    public void drawOutline(Rect r, int argb) {
        renderer.drawRectOutline(r.x, r.y, r.w, r.h, 1f, argb);
    }

    public void drawOutline(int x, int y, int w, int h, int argb) {
        renderer.drawRectOutline(x, y, w, h, 1f, argb);
    }

    public void fillCircle(int cx, int cy, double radius, int argb) {
        renderer.drawCircle(cx, cy, radius, argb);
    }

    public void text(String s, int x, int y, int argb) {
        uiText.render(s, x, y, new silversword.axiom.client.render.rendersystem.utils.color.Color(argb), false);
    }

    public void textShadow(String s, int x, int y, int argb) {
        uiText.render(s, x, y, new silversword.axiom.client.render.rendersystem.utils.color.Color(argb), true);
    }

    public int textWidth(String s) {
        return (int) uiText.getWidth(s);
    }

    public int fontHeight() {
        return (int) uiText.getHeight();
    }

    public int fontAscent() {
        return (int) uiText.getAscent();
    }
}