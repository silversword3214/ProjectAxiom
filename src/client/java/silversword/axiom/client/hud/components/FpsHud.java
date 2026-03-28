package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingNumber;

public final class FpsHud extends BaseHudElement {
    private final SettingNumber textScale;
    private final SettingNumber backgroundPadding;
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor backgroundColor;
    private final SettingColor borderColor;
    private final SettingColor textColor;
    private final SettingColor valueColor;

    public FpsHud() {
        super("FPS", 10, 10);

        textScale = new SettingNumber("Text Scale", 0.5, 3.0, 0.1, 1.0);
        backgroundPadding = new SettingNumber("Background Padding", 0, 20, 1, 4);
        backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 4);
        outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 1.0);
        backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        borderColor = new SettingColor("Border Color", new Color(0xFFAAAAAA));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        valueColor = new SettingColor("Value Color", new Color(0xFF00FF00));

        settings.addSetting(textScale);
        settings.addSetting(backgroundPadding);
        settings.addSetting(backgroundRadius);
        settings.addSetting(outlineThickness);
        settings.addNamedColor(new NamedColor("Background", backgroundColor));
        settings.addNamedColor(new NamedColor("Border", borderColor));
        settings.addNamedColor(new NamedColor("Text", textColor));
        settings.addNamedColor(new NamedColor("Value", valueColor));
    }

    @Override
    public int width(Minecraft mc) {
        float scale = (float) textScale.getValue();
        String text = "FPS: 999"; // maksimileveys
        int textW = (int) (TextRenderer.get().getWidth(text) * scale);
        return textW + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public int height(Minecraft mc) {
        float scale = (float) textScale.getValue();
        int lineH = (int) (TextRenderer.get().getHeight() * scale);
        return lineH + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Color bgCol = backgroundColor.getCurrentColor();
        Color borderCol = borderColor.getCurrentColor();
        Color txtCol = textColor.getCurrentColor();
        Color valCol = valueColor.getCurrentColor();

        float scale = (float) textScale.getValue();
        int padding = (int) backgroundPadding.getValue();
        int radius = (int) backgroundRadius.getValue();
        double thickness = outlineThickness.getValue();

        int fps = ctx.mc.getFps(); // Minecraftin oma fps-laskuri
        String textLabel = "FPS: ";
        String valueStr = String.valueOf(fps);

        // Tekstin leveydet
        float labelWidth = (float) (TextRenderer.get().getWidth(textLabel) * scale);
        float valueWidth = (float) (TextRenderer.get().getWidth(valueStr) * scale);
        int totalWidth = (int) (labelWidth + valueWidth + 2 * scale); // pieni väli
        int lineH = (int) (TextRenderer.get().getHeight() * scale);

        int bgW = totalWidth + padding * 2;
        int bgH = lineH + padding * 2;
        int bgX = x;
        int bgY = y;

        // Tausta
        if (radius > 0) {
            ctx.fillRounded(bgX, bgY, bgW, bgH, radius, bgCol.getARGB());
        } else {
            ctx.fill(bgX, bgY, bgW, bgH, bgCol.getARGB());
        }

        // Reunus
        if (borderCol.getAlpha() != 0 && thickness > 0) {
            ctx.drawRoundedOutline(bgX, bgY, bgW, bgH, radius, borderCol.getARGB(), thickness);
        }

        // Teksti
        int textX = bgX + padding;
        int textY = bgY + padding;
        ctx.drawScaledText(textLabel, textX, textY, txtCol.getARGB(), true, scale);
        ctx.drawScaledText(valueStr, textX + (int) labelWidth + (int) (2 * scale), textY, valCol.getARGB(), true, scale);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}