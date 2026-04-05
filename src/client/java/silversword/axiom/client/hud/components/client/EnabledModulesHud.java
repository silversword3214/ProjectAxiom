package silversword.axiom.client.hud.components.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;

public final class EnabledModulesHud extends BaseHudElement {
    private static final int FONT_HEIGHT = 9;
    private static final int PADDING = 4;
    private static final int BAR_WIDTH = 2;
    private static final int CORNER_RADIUS = 3;

    private final SettingNumber textScale;
    private final SettingBoolean showBackground;
    private final SettingBoolean customColors;
    private final SettingColor textColor;
    private final SettingColor barColor;
    private final SettingColor backgroundColor;

    private final SettingBoolean rainbowWave;
    private final SettingNumber waveSpeed;

    public EnabledModulesHud() {
        super("Module list", 6, 20);

        textScale = new SettingNumber("Scale", 0.5, 2.0, 0.05, 1.0);
        showBackground = new SettingBoolean("Show Background", false);
        customColors = new SettingBoolean("Custom Colors", false);
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        barColor = new SettingColor("Bar Color", new Color(ThemeManager.getCurrentTheme().accent));
        backgroundColor = new SettingColor("Background Color", new Color(ThemeManager.getCurrentTheme().panel));

        waveSpeed = new SettingNumber("Wave Speed", 0.5, 5.0, 0.1, 1.0);
        rainbowWave = new SettingBoolean("Rainbow Wave", false);

        settings.addSetting(waveSpeed);
        settings.addSetting(rainbowWave);
        settings.addSetting(textScale);
        settings.addSetting(showBackground);
        settings.addSetting(customColors);

        settings.addNamedColor(new NamedColor("Text Color", textColor));
        settings.addNamedColor(new NamedColor("Bar Color", barColor));
        settings.addNamedColor(new NamedColor("BG Color", backgroundColor));
    }

    private List<String> enabledNames() {
        List<String> names = new ArrayList<>();
        for (AxiomMod m : ModuleManager.getInstance().getModules()) {
            if (m != null && m.isEnabled() && m.getCategory() != ModuleCategory.HIDDEN) {
                names.add(m.getName());
            }
        }
        // KORJAUS: Käytetään shadow = true mittaukseen, jotta lajittelu ja leveys täsmäävät
        names.sort(Comparator.comparingDouble((String s) -> TextRenderer.get().getWidth(s, true)).reversed());
        return names;
    }

    @Override
    public int width(Minecraft mc) {
        List<String> list = enabledNames();
        double scale = textScale.getValue();
        if (list.isEmpty()) return (int) (60 * scale);

        double maxWidth = 0;
        for (String s : list) {
            maxWidth = Math.max(maxWidth, TextRenderer.get().getWidth(s, true) * scale);
        }

        int paddingTotal = showBackground.get() ? (int) (PADDING * 2 * scale) : 4;
        return (int) (maxWidth + (BAR_WIDTH * scale) + paddingTotal + 8);
    }

    @Override
    public int height(Minecraft mc) {
        List<String> list = enabledNames();
        double scale = textScale.getValue();
        int count = list.isEmpty() ? (Minecraft.getInstance().screen != null ? 1 : 0) : list.size();
        return count * getItemHeight(scale);
    }

    private int getItemHeight(double scale) {
        if (showBackground.get()) {
            return (int) ((FONT_HEIGHT + 2 * PADDING) * scale);
        } else {
            return (int) (FONT_HEIGHT * scale + 3);
        }
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        List<String> list = enabledNames();
        if (list.isEmpty()) {
            if (tickCounter == null) {
                list = new ArrayList<>();
                list.add("Module List");
            } else return;
        }

        double scale = textScale.getValue();
        boolean hasBg = showBackground.get();
        int paddingScaled = (int) (PADDING * scale);
        int barWidthScaled = (int) (BAR_WIDTH * scale);
        int itemHeight = getItemHeight(scale);

        boolean barOnLeft = x < ctx.mc.getWindow().getGuiScaledWidth() / 2;

        int currentBgColor = customColors.get() ? backgroundColor.getCurrentColor().getARGB() : ctx.theme.panel;
        int currentTextColor = customColors.get() ? textColor.getCurrentColor().getARGB() : ctx.theme.text;
        int currentBarColor = customColors.get() ? barColor.getCurrentColor().getARGB() : ctx.theme.accent;

        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            int yy = y + i * itemHeight;
            double textW = TextRenderer.get().getWidth(name, true) * scale;
            int boxWidth = (int) (textW + (hasBg ? paddingScaled * 2 : 4));

            // Lasketaan palkin paikka
            int barX = barOnLeft ? x : (int) (x + width(ctx.mc) - barWidthScaled - 4);
            int boxX = barOnLeft ? barX + barWidthScaled : barX - boxWidth;
            int textX = boxX + (hasBg ? paddingScaled : 2);
            int textY = yy + (itemHeight - (int) (FONT_HEIGHT * scale)) / 2 - 3;

            // 1. Palkin piirto
            if (rainbowWave.get()) {
                drawDiagonalBar(ctx, barX, yy, barWidthScaled, itemHeight, i);
            } else {
                ctx.fill(barX, yy, barWidthScaled, itemHeight, currentBarColor);
            }

            // 2. Taustan piirto
            if (hasBg) {
                int radius = (int) (CORNER_RADIUS * scale);
                if (barOnLeft) {
                    ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radius, currentBgColor, false, true, true, false);
                } else {
                    ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radius, currentBgColor, true, false, false, true);
                }
            }

            // 3. Tekstin piirto
            if (rainbowWave.get()) {
                drawRainbowText(ctx, name, textX, textY, scale, i);
            } else {
                ctx.drawScaledText(name, textX, textY, currentTextColor, true, (float) scale);
            }
        }
    }

    private void drawDiagonalBar(HudContext ctx, int barX, int barY, int w, int h, int rowIndex) {
        float speed = (float) waveSpeed.getValue();
        RainbowPalette palette = ClickGuiConfigManager.getRainbowPalette();
        long now = System.currentTimeMillis();

        for (int i = 0; i < h; i++) {
            int color = palette.getColorForPosition(now, speed, barY + i, 0, (float) barX, (float) (barY + i));
            ctx.fill(barX, barY + i, w, 1, color);
        }
    }

    private void drawRainbowText(HudContext ctx, String text, int x, int y, double scale, int rowIndex) {
        float speed = waveSpeed != null ? (float) waveSpeed.getValue() : 1.0f;
        RainbowPalette palette = ClickGuiConfigManager.getRainbowPalette();
        long now = System.currentTimeMillis();

        float currentX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int colorArgb = palette.getColorForPosition(now, speed, i, rowIndex, currentX, y);
            ctx.drawScaledText(ch, (int) currentX, y, colorArgb, true, (float) scale);
            currentX += TextRenderer.get().getWidth(ch) * scale;
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}