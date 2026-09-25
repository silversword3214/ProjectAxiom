package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class UiContext {

    public final Minecraft mc;
    public final GuiGraphicsExtractor draw;
    public final Theme theme;
    public final float delta;
    public final RenderCore renderCore;
    private final TextRenderer uiText;
    public final Renderer2D renderer;

    private final Stack<Rect> scissorStack = new Stack<>();
    private final List<TextEntry> textEntries = new ArrayList<>();

    public UiContext(Minecraft mc, GuiGraphicsExtractor draw, Theme theme, float delta, Renderer2D renderer) {
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
        return new Rect(0, 0, 0, 0);
    }

    public void enableScissor(int x, int y, int w, int h) {
        Rect newRect = new Rect(x, y, w, h);
        if (!scissorStack.isEmpty()) {
            Rect parent = scissorStack.peek();
            newRect = intersect(parent, newRect);
        }
        scissorStack.push(newRect);

        // Vanilla GUI -scissor (teksteille)
        draw.enableScissor(newRect.x, newRect.y,
                newRect.x + newRect.w, newRect.y + newRect.h);

        // RenderCore-scissor (recteille, ympyröille, tekstuureille)
        renderCore.enableScissor(newRect.x, newRect.y, newRect.w, newRect.h);
    }

    public void disableScissor() {
        scissorStack.pop();

        if (scissorStack.isEmpty()) {
            draw.disableScissor();
            renderCore.disableScissor();
        } else {
            Rect top = scissorStack.peek();
            draw.enableScissor(top.x, top.y,
                    top.x + top.w, top.y + top.h);
            renderCore.enableScissor(top.x, top.y, top.w, top.h);
        }
    }

    public void fill(Rect r, int argb) { renderer.drawRect(r.x, r.y, r.w, r.h, argb); }
    public void fill(int x, int y, int w, int h, int argb) { renderer.drawRect(x, y, w, h, argb); }
    public void fillRounded(Rect r, int argb, double radius) { renderer.drawRoundedRect(r.x, r.y, r.w, r.h, radius, argb); }
    public void fillRounded(int x, int y, int w, int h, int argb, double radius) { renderer.drawRoundedRect(x, y, w, h, radius, argb); }

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
    public void drawOutline(Rect r, int argb) { renderer.drawRectOutline(r.x, r.y, r.w, r.h, 1f, argb); }
    public void drawOutline(int x, int y, int w, int h, int argb) { renderer.drawRectOutline(x, y, w, h, 1f, argb); }
    public void fillCircle(int cx, int cy, double radius, int argb) { renderer.drawCircle(cx, cy, radius, argb); }

    public void text(String s, int x, int y, int argb) { renderText(s, x, y, argb, false); }
    public void textShadow(String s, int x, int y, int argb) { renderText(s, x, y, argb, true); }

    public void addTexture(Identifier textureId, double x, double y, double width, double height, Color color) {
        renderer.drawTexture(textureId, (float) x, (float) y, (float) width, (float) height, color.getARGB());
    }
    public void addTexture(Identifier textureId, double x, double y, double width, double height, double rotation, Color color) {
        renderer.drawRotatedTexture(textureId, (float) x, (float) y, (float) width, (float) height, (float) rotation, color.getARGB());
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

            renderText(ch, currentX, y, colorArgb, false);

            currentX += uiText.getCharAdvance(text.charAt(i));
        }
    }

    /**
     * Kerää teksti jonoon. Tallennetaan myös sen hetkinen scissor, jotta se
     * voidaan asettaa takaisin kun teksti varsinaisesti piirretään.
     */
    private void renderText(String text, double x, double y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        Rect scissor = scissorStack.isEmpty() ? null : scissorStack.peek();
        textEntries.add(new TextEntry(text, x, y, argb, shadow, scissor));
    }

    /** Piirrä kaikki kerätty teksti kerralla, kunnioittaen jokaisen rivin scissoria. */
    public void renderTexts() {
        for (TextEntry e : textEntries) {
            boolean scissorApplied = false;
            if (e.scissor != null && e.scissor.w > 0 && e.scissor.h > 0) {
                draw.enableScissor(
                        e.scissor.x,
                        e.scissor.y,
                        e.scissor.x + e.scissor.w,
                        e.scissor.y + e.scissor.h
                );
                scissorApplied = true;
            }

            try {
                if (uiText instanceof silversword.axiom.client.render.font.CustomTextRenderer custom) {
                    custom.render(draw, e.text, e.x, e.y, new Color(e.color), e.shadow);
                } else if (e.shadow) {
                    draw.text(mc.font, e.text, (int) e.x, (int) e.y, e.color);
                } else {
                    draw.text(mc.font, e.text, (int) e.x, (int) e.y, e.color, false);
                }
            } finally {
                if (scissorApplied) {
                    draw.disableScissor();
                }
            }
        }
        textEntries.clear();
    }

    public int textWidth(String s) { return (int) uiText.getWidth(s == null ? "" : s); }
    public int fontHeight() { return (int) uiText.getHeight(); }
    public int fontAscent() { return (int) uiText.getAscent(); }

    private static final class TextEntry {
        final String text;
        final double x, y;
        final int color;
        final boolean shadow;
        final Rect scissor;

        TextEntry(String text, double x, double y, int color, boolean shadow, Rect scissor) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.shadow = shadow;
            this.scissor = scissor;
        }
    }
}