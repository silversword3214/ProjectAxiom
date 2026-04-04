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
    private final SettingNumber boxOffsetX; // UUSI: Etäisyys logosta
    private final SettingNumber boxOffsetY; // UUSI: Pystysuuntainen siirtymä
    private final SettingNumber paddingX;   // UUSI: Laatikon leveyssuuntainen koko
    private final SettingNumber paddingY;   // UUSI: Laatikon korkeussuuntainen koko
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor textColor;
    private final SettingColor backgroundColor;
    private final SettingColor outlineColor;

    public WatermarkHud() {
        super("Watermark", 6, 6);
        this.logoScale = new SettingNumber("Logo Scale", 0.01, 1, 0.01, 0.2);
        this.textScale = new SettingNumber("Text Scale", 0.2, 3.0, 0.1, 0.80);

        // Uudet asetukset offsetille ja koolle
        this.boxOffsetX = new SettingNumber("Box Offset X", -50, 100, 1, 5);
        this.boxOffsetY = new SettingNumber("Box Offset Y", -50, 50, 1, 0);
        this.paddingX = new SettingNumber("Padding X", 0, 30, 1, 6);
        this.paddingY = new SettingNumber("Padding Y", 0, 20, 1, 3);

        this.backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 6);
        this.outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 2.0);
        this.textColor = new SettingColor("Text Color", new Color(0xFFFFFFFF));
        this.backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        this.outlineColor = new SettingColor("Outline Color", new Color(0x00000000));

        settings.addSetting(logoScale);
        settings.addSetting(textScale);
        settings.addSetting(boxOffsetX);
        settings.addSetting(boxOffsetY);
        settings.addSetting(paddingX);
        settings.addSetting(paddingY);
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

        int logoW = (int) (tex.getWidth() * (float) logoScale.getValue());
        int textW = (int) (mc.font.width(VERSION_TEXT) * (float) textScale.getValue());

        // Kokonaisleveys: Logo + Offset + Laatikon leveys (teksti + paddingit)
        return logoW + (int) boxOffsetX.getValue() + textW + ((int) paddingX.getValue() * 2);
    }

    @Override
    public int height(Minecraft mc) {
        Texture tex = TextureManager.getTexture(LOGO_ID);
        if (tex == null) return 0;

        int logoH = (int) (tex.getHeight() * (float) logoScale.getValue());
        int textH = (int) (mc.font.lineHeight * (float) textScale.getValue());
        int boxH = textH + ((int) paddingY.getValue() * 2);

        // Korkeus on joko logon tai laatikon korkeus (huomioiden offsetin)
        return Math.max(logoH, boxH + Math.abs((int) boxOffsetY.getValue()));
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Texture tex = TextureManager.getTexture(LOGO_ID);
        if (tex == null) return;

        // Arvot asetuksista
        float lScale = (float) logoScale.getValue();
        float tScale = (float) textScale.getValue();
        int offX = (int) boxOffsetX.getValue();
        int offY = (int) boxOffsetY.getValue();
        int pX = (int) paddingX.getValue();
        int pY = (int) paddingY.getValue();
        int radius = (int) backgroundRadius.getValue();
        double thickness = outlineThickness.getValue();

        // Logon mitat
        int logoW = (int) (tex.getWidth() * lScale);
        int logoH = (int) (tex.getHeight() * lScale);

        // Tekstin mitat
        int textW = (int) (ctx.textWidth(VERSION_TEXT) * tScale);
        int textH = (int) (ctx.fontHeight() * tScale);

        // Laatikon mitat
        int bgW = textW + pX * 2;
        int bgH = textH + pY * 2;

        // Sijoittelu
        int boxX = x + logoW + offX;
        int boxY = y + (logoH - bgH) / 2 + offY; // Keskistetty logoon nähden + offset

        int textX = boxX + pX;
        int textY = boxY + pY;

        // 1. Piirretään logo
        ctx.renderer.drawTexture(LOGO_ID, x, y, logoW, logoH);

        // 2. Piirretään laatikon tausta ja reunus
        Color bgCol = backgroundColor.getCurrentColor();
        Color outlineCol = outlineColor.getCurrentColor();

        if (outlineCol.getAlpha() != 0 && thickness > 0) {
            ctx.drawRoundedOutline(boxX, boxY, bgW, bgH, radius, outlineCol.getARGB(), thickness);
        }

        if (radius > 0) {
            ctx.fillRounded(boxX, boxY, bgW, bgH, radius, bgCol.getARGB());
        } else {
            ctx.fill(boxX, boxY, bgW, bgH, bgCol.getARGB());
        }

        // 3. Piirretään teksti
        ctx.drawScaledText(VERSION_TEXT, textX, textY, textColor.getCurrentColor().getARGB(), true, tScale);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}