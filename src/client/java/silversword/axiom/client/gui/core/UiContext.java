package silversword.axiom.client.gui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class UiContext {

    public final Minecraft mc;
    public final GuiGraphicsExtractor draw;
    public final Theme theme;
    public final float delta;
    public final RenderCore renderCore;
    public final Renderer2D renderer;
    private final TextRenderer uiText;

    private final Stack<Rect> scissorStack = new Stack<>();
    private final List<TextEntry> textEntries = new ArrayList<>();
    private final List<ItemEntry> items = new ArrayList<>();

    public UiContext(Minecraft mc, GuiGraphicsExtractor draw, Theme theme,
                     float delta, Renderer2D renderer) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;
        this.uiText = TextRenderer.get();
        this.renderer = renderer;
        this.renderCore = renderer.core;
    }

    // ================================================================
    //  SCISSOR
    // ================================================================

    private Rect intersect(Rect a, Rect b) {
        int left = Math.max(a.x, b.x);
        int top = Math.max(a.y, b.y);
        int right = Math.min(a.right(), b.right());
        int bottom = Math.min(a.bottom(), b.bottom());
        if (left < right && top < bottom) return new Rect(left, top, right - left, bottom - top);
        return new Rect(0, 0, 0, 0);
    }

    public void enableScissor(int x, int y, int w, int h) {
        Rect newRect = new Rect(x, y, w, h);
        if (!scissorStack.isEmpty()) newRect = intersect(scissorStack.peek(), newRect);
        scissorStack.push(newRect);
        draw.enableScissor(newRect.x, newRect.y, newRect.x + newRect.w, newRect.y + newRect.h);
        renderCore.enableScissor(newRect.x, newRect.y, newRect.w, newRect.h);
    }

    public void disableScissor() {
        scissorStack.pop();
        if (scissorStack.isEmpty()) {
            draw.disableScissor();
            renderCore.disableScissor();
        } else {
            Rect top = scissorStack.peek();
            draw.enableScissor(top.x, top.y, top.x + top.w, top.y + top.h);
            renderCore.enableScissor(top.x, top.y, top.w, top.h);
        }
    }

    // ================================================================
    //  FILL — kaikki menee core2D-batchiin
    // ================================================================

    public void fill(Rect r, int argb) { renderer.drawRect(r.x, r.y, r.w, r.h, argb); }
    public void fill(int x, int y, int w, int h, int argb) { renderer.drawRect(x, y, w, h, argb); }

    public void fillRounded(Rect r, int argb, double radius) {
        renderer.drawRoundedRect(r.x, r.y, r.w, r.h, radius, argb);
    }
    public void fillRounded(int x, int y, int w, int h, int argb, double radius) {
        renderer.drawRoundedRect(x, y, w, h, radius, argb);
    }

    public void fillRoundedCustom(Rect r, int argb, double radius,
                                  boolean tl, boolean tr, boolean br, boolean bl) {
        renderer.drawRoundedRectCustom(r.x, r.y, r.w, r.h, radius, argb, tl, tr, br, bl);
    }
    public void fillRoundedCustom(int x, int y, int w, int h, int radius, int argb,
                                  boolean tl, boolean tr, boolean br, boolean bl) {
        renderer.drawRoundedRectCustom(x, y, w, h, radius, argb, tl, tr, br, bl);
    }

    public void drawRoundedOutline(Rect r, int argb, double radius, double thickness) {
        renderer.drawRoundedRectOutline(r.x, r.y, r.w, r.h, radius, argb, thickness);
    }
    public void drawRoundedOutline(int x, int y, int w, int h, int radius, int argb, double thickness) {
        renderer.drawRoundedRectOutline(x, y, w, h, radius, argb, thickness);
    }
    public void drawOutline(Rect r, int argb) { renderer.drawRectOutline(r.x, r.y, r.w, r.h, 1f, argb); }
    public void drawOutline(int x, int y, int w, int h, int argb) {
        renderer.drawRectOutline(x, y, w, h, 1f, argb);
    }
    public void drawOutline(int x, int y, int w, int h, int argb, float thickness) {
        renderer.drawRectOutline(x, y, w, h, thickness, argb);
    }
    public void drawRectOutline(Rect r, int argb, double thickness) {
        renderer.drawRectOutline(r.x, r.y, r.w, r.h, (float) thickness, argb);
    }
    public void fillCircle(int cx, int cy, double radius, int argb) {
        renderer.drawCircle(cx, cy, radius, argb);
    }

    // ================================================================
    //  TEXT — kaikki menee vanilla-pipelineen
    // ================================================================

    public void text(String s, int x, int y, int argb) { renderText(s, x, y, argb, false, 1.0f); }
    public void text(String s, int x, int y, int argb, boolean shadow) {
        renderText(s, x, y, argb, shadow, 1.0f);
    }
    public void textShadow(String s, int x, int y, int argb) { text(s, x, y, argb, true); }
    public void drawScaledText(String s, int x, int y, int argb, boolean shadow, float scale) {
        renderText(s, x, y, argb, shadow, scale);
    }
    public void centeredText(String s, int centerX, int y, int argb) {
        text(s, centerX - textWidth(s) / 2, y, argb);
    }

    private void renderText(String text, double x, double y, int argb, boolean shadow, float scale) {
        if (text == null || text.isEmpty()) return;
        Rect scissor = scissorStack.isEmpty() ? null : scissorStack.peek();
        textEntries.add(new TextEntry(text, x, y, argb, shadow, scale, scissor));
    }

    /** Piirretään kaikki kerätty teksti — vanilla GUI pipeline. */
    public void renderTexts() {
        for (TextEntry e : textEntries) {
            boolean scissorApplied = false;
            if (e.scissor != null && e.scissor.w > 0 && e.scissor.h > 0) {
                draw.enableScissor(e.scissor.x, e.scissor.y,
                        e.scissor.x + e.scissor.w, e.scissor.y + e.scissor.h);
                scissorApplied = true;
            }
            try {
                boolean wasBuilding = uiText.isBuilding();
                if (!wasBuilding) uiText.begin(e.scale, false, false);
                if (uiText instanceof silversword.axiom.client.render.font.CustomTextRenderer custom) {
                    custom.render(draw, e.text, e.x, e.y, new Color(e.color), e.shadow);
                } else if (e.shadow) {
                    draw.text(mc.font, e.text, (int) e.x, (int) e.y, e.color);
                } else {
                    draw.text(mc.font, e.text, (int) e.x, (int) e.y, e.color, false);
                }
                if (!wasBuilding) uiText.end();
            } finally {
                if (scissorApplied) draw.disableScissor();
            }
        }
        textEntries.clear();
    }

    public int textWidth(String s) { return (int) uiText.getWidth(s == null ? "" : s); }
    public int fontHeight() { return (int) uiText.getHeight(); }
    public int fontAscent() { return (int) uiText.getAscent(); }

    public void drawVanillaText(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) draw.text(mc.font, s, x, y, argb);
        else draw.text(mc.font, s, x, y, argb, false);
    }
    public void drawVanillaText(String s, int x, int y, int argb) {
        drawVanillaText(s, x, y, argb, false);
    }
    public int getVanillaTextWidth(String s) { return mc.font.width(s); }
    public int getVanillaFontHeight() { return mc.font.lineHeight; }
    public GuiGraphicsExtractor getVanillaContext() { return draw; }

    // ================================================================
    //  EFFECTS
    // ================================================================

    public void drawVanillaEffectIcon(MobEffectInstance effect, int x, int y,
                                      int size, float alpha) {
        if (effect == null) return;
        Holder<MobEffect> entry = effect.getEffect();
        Identifier tex = entry.unwrapKey()
                .map(key -> key.identifier().withPrefix("mob_effect/"))
                .orElse(null);
        if (tex == null) return;
        draw.blitSprite(RenderPipelines.GUI_TEXTURED, tex, x, y, size, size,
                ARGB.white(clamp01(alpha)));
    }

    // ================================================================
    //  ITEMIT
    // ================================================================

    public void item(ItemStack stack, int x, int y) { item(stack, x, y, 16); }
    public void item(ItemStack stack, int x, int y, int size) {
        if (!stack.isEmpty()) items.add(new ItemEntry(stack, x, y, size));
    }
    public void drawItem(ItemStack stack, int x, int y) { item(stack, x, y); }
    public void drawItem(ItemStack stack, int x, int y, int size) { item(stack, x, y, size); }
    public List<ItemEntry> getItems() { return items; }

    // ================================================================
    //  TEKSTUURIT
    // ================================================================

    public void addTexture(Identifier textureId, double x, double y,
                           double width, double height, Color color) {
        renderer.drawTexture(textureId, (float) x, (float) y,
                (float) width, (float) height, color.getARGB());
    }
    public void addTexture(Identifier textureId, double x, double y,
                           double width, double height, double rotation, Color color) {
        renderer.drawRotatedTexture(textureId, (float) x, (float) y,
                (float) width, (float) height, (float) rotation, color.getARGB());
    }

    // ================================================================
    //  RAINBOW
    // ================================================================

    public void drawRainbowText(String text, float x, float y, int rowIndex) {
        float speed = silversword.axiom.client.config.ClickGuiConfigManager.getRainbowWaveSpeed();
        var palette = silversword.axiom.client.config.ClickGuiConfigManager.getRainbowPalette();
        long now = System.currentTimeMillis();
        float currentX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int colorArgb = palette.getColorForPosition(now, speed, i, rowIndex, currentX, y);
            renderText(ch, currentX, y, colorArgb, false, 1.0f);
            currentX += uiText.getCharAdvance(text.charAt(i));
        }
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static final class TextEntry {
        final String text;
        final double x, y;
        final int color;
        final boolean shadow;
        final float scale;
        final Rect scissor;
        TextEntry(String text, double x, double y, int color, boolean shadow,
                  float scale, Rect scissor) {
            this.text = text; this.x = x; this.y = y;
            this.color = color; this.shadow = shadow;
            this.scale = scale; this.scissor = scissor;
        }
    }

    public static class ItemEntry {
        public final ItemStack stack;
        public final int x, y, size;
        public ItemEntry(ItemStack stack, int x, int y, int size) {
            this.stack = stack; this.x = x; this.y = y; this.size = size;
        }
    }
}