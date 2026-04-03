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
    private static final int CHAR_UNIT = 6;

    private final SettingNumber textScale;
    private final SettingBoolean customColors;
    private final SettingColor textColor;
    private final SettingColor barColor;
    private final SettingColor backgroundColor;

    public EnabledModulesHud() {
        super("Module list", 6, 20);

        textScale = new SettingNumber("Scale", 0.5, 2, 0.05, 1);
        customColors = new SettingBoolean("Custom Colors", false);
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        barColor = new SettingColor("Bar Color", new Color(ThemeManager.getCurrentTheme().accent));
        backgroundColor = new SettingColor("Background Color", new Color(ThemeManager.getCurrentTheme().panel));

        settings.addSetting(textScale);
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
        names.sort(Comparator.comparingInt((String s) -> (int) TextRenderer.get().getWidth(s)).reversed());
        return names;
    }

    @Override
    public int width(Minecraft mc) {
        List<String> list = enabledNames();
        if (list.isEmpty()) return 0;
        float scale = (float) textScale.getValue();
        int maxLen = 0;
        for (String s : list) {
            if (s.length() > maxLen) maxLen = s.length();
        }
        int bgPoints = maxLen + 1; // jos 1 piste -> tausta 2 pistettä, jne.
        int maxBoxWidth = (int) (bgPoints * CHAR_UNIT * scale);
        int barWidthScaled = (int) (BAR_WIDTH * scale);
        return barWidthScaled + maxBoxWidth;
    }

    @Override
    public int height(Minecraft mc) {
        int count = enabledNames().size();
        if (count == 0) return 0;
        float scale = (float) textScale.getValue();
        int itemHeight = (int) ((FONT_HEIGHT + 8 * PADDING) * scale);
        return count * itemHeight;
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        List<String> list = enabledNames();
        if (list.isEmpty()) return;

        float scale = (float) textScale.getValue();
        int baseY = y;

        int paddingScaled = (int) (PADDING * scale);
        int barWidthScaled = (int) (BAR_WIDTH * scale);
        int radiusScaled = (int) (CORNER_RADIUS * scale);
        int itemHeight = (int) ((FONT_HEIGHT + 2 * PADDING) * scale);

        boolean barOnLeft = x < ctx.mc.getWindow().getGuiScaledWidth() / 2;

        int maxLen = 0;
        for (String s : list) {
            maxLen = Math.max(maxLen, s.length());
        }
        int maxBoxWidth = (int) ((maxLen + 1) * CHAR_UNIT * scale);

        int[] boxWidths = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            boxWidths[i] = (int) ((list.get(i).length() + 1) * CHAR_UNIT * scale);
        }

        int totalHeight = list.size() * itemHeight;

        // Laske elementin kokonaisleveys (sama kuin width() palauttaa)
        int elementWidth = barWidthScaled + maxBoxWidth;

        int screenWidth = ctx.mc.getWindow().getGuiScaledWidth();
        int renderX = x; // alkuperäinen x, jota käytetään piirrossa

        // Jos elementti menee oikealta yli, siirrä vasemmalle
        if (renderX + elementWidth > screenWidth) {
            renderX = screenWidth - elementWidth;
        }
        // Jos siirto vei vasemmalta yli, rajoita nollaan
        if (renderX < 0) {
            renderX = 0;
        }

        int accentColor = ThemeManager.getCurrentTheme().accent;
        int defaultTextColor = accentColor;
        int defaultBarColor = accentColor;
        int defaultBgColor = ctx.theme.panel;

        int currentTextColor, currentBarColor, currentBgColor;
        if (customColors.get()) {
            currentTextColor = textColor.getCurrentColor().getARGB();
            currentBarColor = barColor.getCurrentColor().getARGB();
            currentBgColor = backgroundColor.getCurrentColor().getARGB();
        } else {
            currentTextColor = defaultTextColor;
            currentBarColor = defaultBarColor;
            currentBgColor = defaultBgColor;
        }

        int barX = barOnLeft ? renderX : renderX + maxBoxWidth;
        ctx.fillRounded(barX, baseY, barWidthScaled, totalHeight, radiusScaled, currentBarColor);

        for (int i = 0; i < list.size(); i++) {
            int yy = baseY + i * itemHeight;
            int boxWidth = boxWidths[i];
            int boxX;

            if (barOnLeft) {
                boxX = renderX + barWidthScaled;
                ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radiusScaled, currentBgColor,
                        false, true, true, false);
            } else {
                boxX = barX - boxWidth;
                ctx.fillRoundedCustom(boxX, yy, boxWidth, itemHeight, radiusScaled, currentBgColor,
                        true, false, false, true);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            int yy = baseY + i * itemHeight;
            int boxWidth = boxWidths[i];
            int textY = yy + (itemHeight - (int) (FONT_HEIGHT * scale)) / 2;
            int textWidth = (int) (name.length() * CHAR_UNIT * scale);
            int textX;

            if (barOnLeft) {
                textX = renderX + barWidthScaled + paddingScaled;
            } else {
                textX = barX - textWidth - paddingScaled;
            }

            ctx.drawScaledText(name, textX, textY, currentTextColor, false, scale);
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}