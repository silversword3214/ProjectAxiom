package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.font.TextRenderer;

public class UiContext {
    public final Minecraft mc;
    public final GuiGraphics draw;
    public final Theme theme;
    public final float delta;
    private final TextRenderer uiText;

    public UiContext(Minecraft mc, GuiGraphics draw, Theme theme, float delta) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;
        this.uiText = TextRenderer.get();
    }

    public void fill(Rect r, int argb) {
        Renderer2D.COLOR.quad(r.x, r.y, r.w, r.h, new Color(argb));
    }

    public void fill(int x, int y, int w, int h, int argb) {
        Renderer2D.COLOR.quad(x, y, w, h, new Color(argb));
    }

    public void fillRounded(Rect r, int argb, double radius) {
        Renderer2D.COLOR.drawRoundedRect(r.x, r.y, r.w, r.h, radius, new Color(argb));
    }

    public void fillRounded(int x, int y, int w, int h, int argb, double radius) {
        Renderer2D.COLOR.drawRoundedRect(x, y, w, h, radius, new Color(argb));
    }

    public void fillRoundedCustom(Rect r, int argb, double radius,
                                  boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        Renderer2D.COLOR.drawRoundedRectCustom(r.x, r.y, r.w, r.h, radius, new Color(argb),
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void drawRoundedOutline(Rect r, int argb, double radius, double thickness) {
        Renderer2D.COLOR.drawRoundedRectOutline(r.x, r.y, r.w, r.h, radius, new Color(argb), thickness);
    }

    public void drawRectOutline(Rect r, int argb, double thickness) {
        Color color = new Color(argb);
        Renderer2D.COLOR.lineThick(r.x, r.y, r.x + r.w, r.y, thickness, color); // ylä
        Renderer2D.COLOR.lineThick(r.x + r.w, r.y, r.x + r.w, r.y + r.h, thickness, color); // oikea
        Renderer2D.COLOR.lineThick(r.x + r.w, r.y + r.h, r.x, r.y + r.h, thickness, color); // ala
        Renderer2D.COLOR.lineThick(r.x, r.y + r.h, r.x, r.y, thickness, color); // vasen
    }

    public void drawOutline(Rect r, int argb) {
        Renderer2D.COLOR.boxLines(r.x, r.y, r.w, r.h, new Color(argb));
    }

    public void drawOutline(int x, int y, int w, int h, int argb) {
        Renderer2D.COLOR.boxLines(x, y, w, h, new Color(argb));
    }

    public void text(String s, int x, int y, int argb) {
        uiText.render(s, x, y, new Color(argb), false);
    }

    public void textShadow(String s, int x, int y, int argb) {
        uiText.render(s, x, y, new Color(argb), true);
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