package silversword.axiom.client.hud.core;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

import java.util.ArrayList;
import java.util.List;

public final class HudContext {
    public final Minecraft mc;
    public final GuiGraphics draw;
    public final Theme theme;
    public final float delta;
    public final Renderer2D renderer;

    private final List<TextEntry> textEntries = new ArrayList<>();

    private final List<ItemEntry> items = new ArrayList<>();


    public HudContext(Minecraft mc, GuiGraphics draw, Theme theme, float delta, Renderer2D renderer) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;
        this.renderer = renderer;
    }

    public void fill(int x, int y, int w, int h, int argb) {
        renderer.drawRect(x, y, w, h, argb);
    }

    public void fillRounded(int x, int y, int w, int h, int radius, int argb) {
        renderer.drawRoundedRect(x, y, w, h, radius, argb);
    }

    public void fillRoundedCustom(int x, int y, int w, int h, int radius, int argb,
                                  boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        renderer.drawRoundedRectCustom(x, y, w, h, radius, argb,
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void drawRoundedOutline(int x, int y, int w, int h, int radius, int argb, double thickness) {
        renderer.drawRoundedRectOutline(x, y, w, h, radius, argb, thickness);
    }

    public void drawOutline(int x, int y, int w, int h, int argb, float thickness) {
        renderer.drawRectOutline(x, y, w, h, thickness, argb);
    }

    public void drawOutline(int x, int y, int w, int h, int argb) {
        drawOutline(x, y, w, h, argb, 1.0f);
    }



    public void drawCircle(int cx, int cy, int radius, int argb) {
        renderer.drawCircle(cx, cy, radius, argb);
    }

    public void drawVanillaEffectIcon(MobEffectInstance effect, int x, int y, int size, float alpha) {
        if (effect == null) return;
        Holder<MobEffect> entry = effect.getEffect();
        Identifier tex;
        try {
            tex = Gui.getMobEffectSprite(entry);
        } catch (Throwable t) {
            return;
        }
        float a = clamp01(alpha);
        int color = ARGB.white(a);
        draw.blitSprite(RenderPipelines.GUI_TEXTURED, tex, x, y, size, size, color);
    }

    public void item(ItemStack stack, int x, int y) {
        item(stack, x, y, 16); // default size 16
    }

    public void item(ItemStack stack, int x, int y, int size) {
        if (!stack.isEmpty()) {
            items.add(new ItemEntry(stack, x, y, size));
        }
    }

    public void drawItem(ItemStack stack, int x, int y) {
        item(stack, x, y);
    }

    public void drawItem(ItemStack stack, int x, int y, int size) {
        item(stack, x, y);
    }

    public void text(String s, int x, int y, int argb, boolean shadow) {
        addText(s, x, y, argb, shadow, 1.0f);
    }

    public void text(String s, int x, int y, int argb) {
        text(s, x, y, argb, false);
    }

    public void textShadow(String s, int x, int y, int argb) {
        text(s, x, y, argb, true);
    }

    public void drawScaledText(String s, int x, int y, int argb, boolean shadow, float scale) {
        addText(s, x, y, argb, shadow, scale);
    }

    private void addText(String s, int x, int y, int argb, boolean shadow, float scale) {
        textEntries.add(new TextEntry(s, x, y, argb, shadow, scale));
    }


    public void renderTexts() {
        for (TextEntry e : textEntries) {
            boolean wasBuilding = TextRenderer.get().isBuilding();
            if (wasBuilding) {
                TextRenderer.get().end();
            }
            TextRenderer.get().begin(e.scale, false, false);
            TextRenderer.get().render(e.text, e.x, e.y, new silversword.axiom.client.render.rendersystem.utils.color.Color(e.color), e.shadow);
            TextRenderer.get().end();
            if (wasBuilding) {
                TextRenderer.get().begin(1.0, false, false);
            }
        }
        textEntries.clear();
    }

    public int textWidth(String s) {
        return (int) TextRenderer.get().getWidth(s);
    }

    public int fontHeight() {
        return (int) TextRenderer.get().getHeight();
    }

    public int fontAscent() {
        return (int) TextRenderer.get().getAscent();
    }

    public void drawVanillaText(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            draw.drawString(mc.font, s, x, y, argb);
        } else {
            draw.drawString(mc.font, s, x, y, argb, false);
        }
    }


    public void drawVanillaText(String s, int x, int y, int argb) {
        drawVanillaText(s, x, y, argb, false);
    }

    public void drawVanillaTextShadow(String s, int x, int y, int argb) {
        drawVanillaText(s, x, y, argb, true);
    }

    public int getVanillaTextWidth(String s) {
        return mc.font.width(s);
    }

    public int getVanillaFontHeight() {
        return mc.font.lineHeight;
    }

    public GuiGraphics getVanillaContext() {
        return draw;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static class TextEntry {
        String text;
        int x, y;
        int color;
        boolean shadow;
        float scale;
        TextEntry(String text, int x, int y, int color, boolean shadow, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.shadow = shadow;
            this.scale = scale;
        }
    }

    public List<ItemEntry> getItems() {
        return items;
    }

    public static class ItemEntry {
        public final ItemStack stack;
        public final int x, y;
        public final int size; // target size in pixels

        public ItemEntry(ItemStack stack, int x, int y, int size) {
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}