package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.utils.render.DrawTexture;

import java.util.Stack;

public class UiContext {

    public final Minecraft mc;
    public final GuiGraphics draw;
    public final Theme theme;
    public final float delta;
    public final RenderCore renderCore;
    private final TextRenderer uiText;
    public final Renderer2D renderer;

    private final Stack<Rect> scissorStack = new Stack<>();


    public UiContext(Minecraft mc, GuiGraphics draw, Theme theme, float delta, Renderer2D renderer) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;
        this.uiText = TextRenderer.get();
        this.renderer = renderer;
        this.renderCore = renderer.core;
    }

    private Rect intersect(Rect a, Rect b) {
        int left = Math.max(a.x, b.x);
        int top = Math.max(a.y, b.y);
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (left < right && top < bottom) {
            return new Rect(left, top, right - left, bottom - top);
        }
        // Jos eivät leikkaa, palauta tyhjä scissor (nolla-alue)
        return new Rect(0, 0, 0, 0);
    }

    public void enableScissor(int x, int y, int w, int h) {
        renderCore.flush();
        Rect newRect = new Rect(x, y, w, h);
        if (!scissorStack.isEmpty()) {
            Rect parent = scissorStack.peek();
            newRect = intersect(parent, newRect); // Leikkaa edellisen kanssa
        }
        scissorStack.push(newRect);
        renderCore.enableScissor(newRect.x, newRect.y, newRect.w, newRect.h);
    }

    public void disableScissor() {
        renderCore.flush();
        scissorStack.pop();
        if (scissorStack.isEmpty()) {
            renderCore.disableScissor();
        } else {
            Rect top = scissorStack.peek();
            renderCore.enableScissor(top.x, top.y, top.w, top.h);
        }
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

    public void addTexture(Identifier textureId, double x, double y, double width, double height, Color color) {
        renderer.drawTexture(textureId, (float) x, (float) y, (float) width, (float) height, color.getARGB());
    }

    public void addTexture(Identifier textureId, double x, double y, double width, double height, double rotation, Color color) {
        renderer.core.addRotatedTexture(textureId, (float) x, (float) y, (float) width, (float) height, (float) rotation, color.getARGB());
    }

    public void textShadow(String s, int x, int y, int argb) {
        uiText.render(s, x, y, new silversword.axiom.client.render.rendersystem.utils.color.Color(argb), true);
    }

    public void centeredText(String s, int centerX, int y, int argb) {
        int width = textWidth(s);
        text(s, centerX - (width / 2), y, argb);
    }

    public void drawRainbowText(String text, float x, float y, int rowIndex) {
        float speed = silversword.axiom.client.config.ClickGuiConfigManager.getRainbowWaveSpeed();
        silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette palette =
                silversword.axiom.client.config.ClickGuiConfigManager.getRainbowPalette();
        long now = System.currentTimeMillis();

        float currentX = x;

        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int colorArgb = palette.getColorForPosition(now, speed, i, rowIndex, currentX, y);

            // 1. Piirretään ilman varjoa (käyttää sisäisesti scale / 2.3)
            uiText.render(ch, currentX, y, new silversword.axiom.client.render.rendersystem.utils.color.Color(colorArgb), false);

            // 2. KORJAUS: Lasketaan leveys samalla 2.3 jakajalla kuin render
            // Haetaan raaka leveys ilman rendererin omaa skaalausta ja lasketaan se itse
            double rawWidth = uiText.getWidth(ch, false);

            // Koska CustomTextRenderer.getWidth käyttää jakajaa 1.5, meidän pitää "kumota" se
            // ja käyttää 2.3 jakajaa, jotta väli täsmää piirrettyyn jälkeen.
            currentX += (rawWidth * 1.5) / 2.3;
        }
    }

    public int textWidth(String s) {
        return (int) (uiText.getWidth(s));
    }

    public int fontHeight() {
        return (int) (uiText.getHeight());
    }

    public int fontAscent() {
        return (int) uiText.getAscent();
    }
}