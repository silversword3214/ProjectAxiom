package silversword.axiom.client.hud.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.List;

public final class HudContext {
    public final MinecraftClient mc;
    public final DrawContext draw;
    public final Theme theme;
    public final float delta;


    // Lista teksteistä, jotka piirretään fillien jälkeen
    private final List<TextEntry> textEntries = new ArrayList<>();

    public HudContext(MinecraftClient mc, DrawContext draw, Theme theme, float delta) {
        this.mc = mc;
        this.draw = draw;
        this.theme = theme;
        this.delta = delta;

    }

    public void fill(int x, int y, int w, int h, int argb) {
        Renderer2D.COLOR.quad(x, y, w, h, new Color(argb));
    }

    public void fillRounded(int x, int y, int w, int h, int radius, int argb) {
        Renderer2D.COLOR.drawRoundedRect(x, y, w, h, radius, new Color(argb));
    }

    public void fillRoundedCustom(int x, int y, int w, int h, int radius, int argb,
                                  boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        Renderer2D.COLOR.drawRoundedRectCustom(x, y, w, h, radius, new Color(argb),
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argb) {
        Renderer2D.COLOR.triangle(x1, y1, x2, y2, x3, y3, new Color(argb));
    }

    public void drawCircle(int cx, int cy, int radius, int argb) {
        Renderer2D.COLOR.drawCircle(cx, cy, radius, new Color(argb));
    }

    public void drawVanillaEffectIcon(StatusEffectInstance effect, int x, int y, int size, float alpha) {
        if (effect == null) return;
        RegistryEntry<StatusEffect> entry = effect.getEffectType();
        Identifier tex;
        try {
            tex = InGameHud.getEffectTexture(entry);
        } catch (Throwable t) {
            return;
        }
        float a = clamp01(alpha);
        int color = ColorHelper.getWhite(a);
        draw.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, size, size, color);
    }

    public void drawItem(ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            draw.drawItem(stack, x, y);
        }
    }

    public void drawItem(ItemStack stack, int x, int y, int size) {
        drawItem(stack, x, y);
    }

    // --- Tekstin lisäys listaan (ei piirretä heti) ---
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

    // Piirretään kaikki kerätyt tekstit (kutsutaan fillien jälkeen)
    public void renderTexts() {
        for (TextEntry e : textEntries) {
            // Tallenna nykyinen tila (jos jokin muu on kesken – ei pitäisi olla)
            boolean wasBuilding = TextRenderer.get().isBuilding();
            if (wasBuilding) {
                TextRenderer.get().end();
            }
            // Aloita uusi batch halutulla skaalalla
            TextRenderer.get().begin(e.scale, false, false);
            // Piirrä teksti
            TextRenderer.get().render(e.text, e.x, e.y, new Color(e.color), e.shadow);
            // Lopeta tämä batch
            TextRenderer.get().end();
            // Palauta alkuperäinen tila (jos oli)
            if (wasBuilding) {
                TextRenderer.get().begin(1.0, false, false);
            }
        }
        textEntries.clear();
    }

    // --- Vanhat apumetodit ---
    public int textWidth(String s) {
        return (int) TextRenderer.get().getWidth(s);
    }

    public int fontHeight() {
        return (int) TextRenderer.get().getHeight();
    }

    public int fontAscent() {
        return (int) TextRenderer.get().getAscent();
    }

    // --- Vanilla-tekstinpiirto (vain hätätapauksissa) ---
    public void drawVanillaText(String s, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            draw.drawTextWithShadow(mc.textRenderer, s, x, y, argb);
        } else {
            draw.drawText(mc.textRenderer, s, x, y, argb, false);
        }
    }

    public void drawVanillaText(String s, int x, int y, int argb) {
        drawVanillaText(s, x, y, argb, false);
    }

    public void drawVanillaTextShadow(String s, int x, int y, int argb) {
        drawVanillaText(s, x, y, argb, true);
    }

    public int getVanillaTextWidth(String s) {
        return mc.textRenderer.getWidth(s);
    }

    public int getVanillaFontHeight() {
        return mc.textRenderer.fontHeight;
    }

    public DrawContext getVanillaContext() {
        return draw;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    // Apuluokka tekstin tallentamiseen
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
}