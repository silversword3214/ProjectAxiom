package silversword.axiom.client.hud.components.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import silversword.axiom.ProjectAxiom;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;
import silversword.axiom.client.setting.SettingNumber;

public final class WatermarkHud extends BaseHudElement {
    private static final Identifier LOGO_ID = Identifier.fromNamespaceAndPath("projectaxiom", "textures/logo.png");
    private static final String VERSION_TEXT = "v" + ProjectAxiom.VERSION;

    private final SettingNumber logoScale;
    private final SettingNumber textScale;
    private final SettingNumber backgroundPadding;
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor textColor;
    private final SettingColor backgroundColor;
    private final SettingColor outlineColor;

    public WatermarkHud() {
        super("Watermark", 6, 6);
        this.logoScale = new SettingNumber("Logo Scale", 0.01, 0.1, 0.01, 0.05);
        this.textScale = new SettingNumber("Text Scale", 0.2, 3.0, 0.1, 0.80);
        this.backgroundPadding = new SettingNumber("Background Padding", 0, 20, 1, 3);
        this.backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 6);
        this.outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 2.0);
        this.textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        this.backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        this.outlineColor = new SettingColor("Outline Color", new Color(0x00000000));

        settings.addSetting(logoScale);
        settings.addSetting(textScale);
        settings.addSetting(backgroundPadding);
        settings.addSetting(backgroundRadius);
        settings.addSetting(outlineThickness);
        settings.addNamedColor(new NamedColor("Text Color", textColor));
        settings.addNamedColor(new NamedColor("Background Color", backgroundColor));
        settings.addNamedColor(new NamedColor("Outline Color", outlineColor));
    }

    @Override
    public int width(Minecraft mc) {
        Texture tex = TextureManager.getTexture(LOGO_ID);
        if (tex == null) return 0;

        float lScale = (float) logoScale.getValue();
        int logoW = (int) (tex.getWidth() * lScale);

        float tScale = (float) textScale.getValue();
        int textW = (int) (mc.font.width(VERSION_TEXT) * tScale);

        int padding = (int) backgroundPadding.getValue();

        return logoW + padding + textW;
    }

    @Override
    public int height(Minecraft mc) {
        Texture tex = TextureManager.getTexture(LOGO_ID);
        if (tex == null) return 0;

        float lScale = (float) logoScale.getValue();
        int logoH = (int) (tex.getHeight() * lScale);

        float tScale = (float) textScale.getValue();
        int textH = (int) (mc.font.lineHeight * tScale);

        return Math.max(logoH, textH);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Texture tex = TextureManager.getTexture(LOGO_ID);
        if (tex == null) return;

        Color textCol = textColor.getCurrentColor();
        Color bgCol = backgroundColor.getCurrentColor();
        Color outlineCol = outlineColor.getCurrentColor();

        float lScale = (float) logoScale.getValue();
        int logoW = (int) (tex.getWidth() * lScale);
        int logoH = (int) (tex.getHeight() * lScale);

        // Piirrä logo Renderer2D:llä
        ctx.renderer.drawTexture(LOGO_ID, x, y, logoW, logoH);

        float tScale = (float) textScale.getValue();
        String text = VERSION_TEXT;
        int textW = (int) (ctx.textWidth(text) * tScale);
        int textH = (int) (ctx.fontHeight() * tScale);

        int padding = (int) backgroundPadding.getValue();
        int radius = (int) backgroundRadius.getValue();
        double thickness = outlineThickness.getValue();

        int textX = x + logoW + padding - 20;
        int textY = y + (logoH - textH) / 2;

        int bgW = textW + padding * 2;
        int bgH = textH + padding * 2;
        int bgX = textX - padding;
        int bgY = textY - padding;

        // Reunus (jos näkyvä)
        if (outlineCol.getAlpha() != 0 && thickness > 0) {
            ctx.drawRoundedOutline(bgX, bgY, bgW, bgH, radius, outlineCol.getARGB(), thickness);
        }

        // Tausta
        if (radius > 0) {
            ctx.fillRounded(bgX, bgY, bgW, bgH, radius, bgCol.getARGB());
        } else {
            ctx.fill(bgX, bgY, bgW, bgH, bgCol.getARGB());
        }

        // Teksti
        ctx.drawScaledText(text, textX, textY, textCol.getARGB(), true, tScale);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}