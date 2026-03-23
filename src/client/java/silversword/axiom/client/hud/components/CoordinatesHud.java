package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.Direction;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;

public final class CoordinatesHud extends BaseHudElement {
    private final SettingNumber textScale;
    private final SettingNumber backgroundPadding;
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor backgroundColor;
    private final SettingColor borderColor;
    private final SettingColor textColor;
    private final SettingBoolean showFacing;

    public CoordinatesHud() {
        super("Coordinates", 10, 10);

        textScale = new SettingNumber("Text Scale", 0.5, 3.0, 0.1, 1.0);
        backgroundPadding = new SettingNumber("Background Padding", 0, 20, 1, 4);
        backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 4);
        outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 1.0);
        backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        borderColor = new SettingColor("Border Color", new Color(0xFFAAAAAA));
        textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        showFacing = new SettingBoolean("Show Facing", true);

        settings.addSetting(textScale);
        settings.addSetting(backgroundPadding);
        settings.addSetting(backgroundRadius);
        settings.addSetting(outlineThickness);
        settings.addNamedColor(new NamedColor("Background", backgroundColor));
        settings.addNamedColor(new NamedColor("Border", borderColor));
        settings.addNamedColor(new NamedColor("Text", textColor));
        settings.addSetting(showFacing);
    }

    @Override
    public int width(Minecraft mc) {
        if (mc.player == null) return 0;
        float scale = (float) textScale.getValue();
        String text = getDisplayText(mc);
        return (int) (TextRenderer.get().getWidth(text) * scale) + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public int height(Minecraft mc) {
        if (mc.player == null) return 0;
        float scale = (float) textScale.getValue();
        int textH = (int) (TextRenderer.get().getHeight() * scale);
        return textH + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Haetaan nykyiset värit (rainbow-tuki)
        Color bgCol = backgroundColor.getCurrentColor();
        Color borderCol = borderColor.getCurrentColor();
        Color txtCol = textColor.getCurrentColor();

        float scale = (float) textScale.getValue();
        int padding = (int) backgroundPadding.getValue();
        int radius = (int) backgroundRadius.getValue();
        double thickness = outlineThickness.getValue();

        String text = getDisplayText(mc);
        int textW = (int) (TextRenderer.get().getWidth(text) * scale);
        int textH = (int) (TextRenderer.get().getHeight() * scale);

        int bgW = textW + padding * 2;
        int bgH = textH + padding * 2;
        int bgX = x;
        int bgY = y;
        int textX = x + padding;
        int textY = y + padding + (textH - (int) (TextRenderer.get().getHeight() * scale)) / 2;

        // Tausta
        if (radius > 0) {
            Renderer2D.COLOR.drawRoundedRect(bgX, bgY, bgW, bgH, radius, bgCol);
        } else {
            Renderer2D.COLOR.quad(bgX, bgY, bgW, bgH, bgCol);
        }

        // Reunus
        if (borderCol.getAlpha() != 0 && thickness > 0) {
            Renderer2D.COLOR.drawRoundedRectOutline(bgX, bgY, bgW, bgH, radius, borderCol, thickness);
        }

        // Teksti
        ctx.drawScaledText(text, textX, textY, txtCol.getPacked(), true, scale);
    }

    private String getDisplayText(Minecraft mc) {
        int x = mc.player.getBlockX();
        int y = mc.player.getBlockY();
        int z = mc.player.getBlockZ();
        String facing = "";
        if (showFacing.get()) {
            Direction dir = mc.player.getDirection();
            facing = " " + dir.name().substring(0, 1).toUpperCase(); // N, S, W, E
        }
        return String.format("X: %d Y: %d Z: %d%s", x, y, z, facing);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}