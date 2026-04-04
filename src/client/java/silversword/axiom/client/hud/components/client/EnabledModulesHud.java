package silversword.axiom.client.hud.components.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
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
        settings.addSetting(waveSpeed);
        rainbowWave = new SettingBoolean("Rainbow Wave", false);
        settings.addSetting(rainbowWave);

        // Lisätään asetukset yksitellen, jotta 'addSettings' -virhe poistuu
        settings.addSetting(textScale);
        settings.addSetting(showBackground);
        settings.addSetting(customColors);

        // NamedColorit lisätään omalla metodillaan
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
        // Lajitellaan leveyden mukaan (levein ylhäällä)
        names.sort(Comparator.comparingDouble((String s) -> TextRenderer.get().getWidth(s)).reversed());
        return names;
    }

    @Override
    public int width(Minecraft mc) {
        List<String> list = enabledNames();
        double scale = textScale.getValue();
        if (list.isEmpty()) return (int) (60 * scale);

        double maxWidth = 0;
        for (String s : list) {
            maxWidth = Math.max(maxWidth, TextRenderer.get().getWidth(s) * scale);
        }

        int paddingTotal = showBackground.get() ? (int) (PADDING * 2 * scale) : 4;
        return (int) (maxWidth + (BAR_WIDTH * scale) + paddingTotal + 4);
    }

    @Override
    public int height(Minecraft mc) {
        List<String> list = enabledNames();
        double scale = textScale.getValue();
        int count = list.isEmpty() ? (Minecraft.getInstance().screen != null ? 1 : 0) : list.size();
        if (count == 0) return 0;

        int itemHeight = getItemHeight(scale);
        return count * itemHeight;
    }

    private int getItemHeight(double scale) {
        if (showBackground.get()) {
            return (int) ((FONT_HEIGHT + 2 * PADDING) * scale);
        } else {
            // Jos ei taustaa, korkeus on tekstin korkeus + 2px väli (gap)
            return (int) (FONT_HEIGHT * scale + 3);
        }
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        List<String> list = enabledNames();
        boolean isEditMode = (tickCounter == null);

        if (list.isEmpty()) {
            if (isEditMode) {
                list = new ArrayList<>();
                list.add("Module List");
            } else {
                return;
            }
        }

        double scale = textScale.getValue();
        boolean hasBg = showBackground.get();
        int paddingScaled = (int) (PADDING * scale);
        int barWidthScaled = (int) (BAR_WIDTH * scale);
        int radiusScaled = (int) (CORNER_RADIUS * scale);
        int itemHeight = getItemHeight(scale);

        // Päätetään kummalla puolella palkki on (vasen/oikea) ruudun puolivälin mukaan
        boolean barOnLeft = x < ctx.mc.getWindow().getGuiScaledWidth() / 2;

        double maxWidth = 0;
        for (String s : list) {
            maxWidth = Math.max(maxWidth, TextRenderer.get().getWidth(s) * scale);
        }

        int totalHeight = list.size() * itemHeight;
        int currentBarColor = 0x0000000;
        int currentBgColor = customColors.get() ? backgroundColor.getCurrentColor().getARGB() : ctx.theme.panel;
        int currentTextColor = customColors.get() ? textColor.getCurrentColor().getARGB() : ctx.theme.text;

        // 1. Piirretään pystybaari
        int barX = barOnLeft ? x : (int) (x + maxWidth + (hasBg ? paddingScaled * 2 : 6));
        ctx.fillRounded(barX, y, barWidthScaled, totalHeight, radiusScaled, currentBarColor);

        // 2. Piirretään moduulit
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            int yy = y + i * itemHeight;
            double textW = TextRenderer.get().getWidth(name) * scale;
            int boxWidth = (int) (textW + (hasBg ? paddingScaled * 2 : 4));

            int boxX;
            int textX;
            int textY = yy + (itemHeight - (int) (FONT_HEIGHT * scale)) / 2 - 3;

            if (barOnLeft) {
                boxX = barX + barWidthScaled;
                textX = boxX + (hasBg ? paddingScaled : 2);
                if (hasBg) {
                    ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radiusScaled, currentBgColor, false, true, true, false);
                }
            } else {
                boxX = barX - boxWidth;
                textX = barX - (int) textW - (hasBg ? paddingScaled : 2);
                if (hasBg) {
                    ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radiusScaled, currentBgColor, true, false, false, true);
                }
            }

            if (rainbowWave.get()) {
                // Välitetään mukaan rivin indeksi i ja y-koordinaatti (tai vain i)
                drawRainbowText(ctx, name, textX, textY, scale, i);
            } else {
                ctx.drawScaledText(name, textX, textY, currentTextColor, true, (float) scale);
            }
        }
    }

    private void drawRainbowText(HudContext ctx, String text, int x, int y, double scale, int rowIndex) {
        float speed = waveSpeed != null ? (float) waveSpeed.getValue() : 1.0f;
        long now = System.currentTimeMillis();
        // Ajan mukaan muuttuva perushue (0-360)
        float timeHue = (now % (long)(5000 / speed)) / (5000f / speed) * 360f;

        // Askeleet: kuinka paljon hue muuttuu per merkki (x) ja per rivi (y)
        float xStep = 12f;   // astetta per merkki
        float yStep = 18f;   // astetta per rivi (voit säätää haluamaksesi)

        float currentX = x;
        for (int charIndex = 0; charIndex < text.length(); charIndex++) {
            String ch = String.valueOf(text.charAt(charIndex));
            // Diagonaalinen hue: aika + (x-paikka) + (rivi * yStep)
            float hue = (timeHue + charIndex * xStep + rowIndex * yStep) % 360;
            Color color = Color.fromHsv(hue, 1.0f, 1.0f);
            ctx.drawScaledText(ch, (int) currentX, y, color.getARGB(), true, (float) scale);
            currentX += TextRenderer.get().getWidth(ch) * scale;
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}