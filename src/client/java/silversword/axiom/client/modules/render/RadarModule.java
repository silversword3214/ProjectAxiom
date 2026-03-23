package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.render.RadarHud;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class RadarModule extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String HUD_ID = "Radar";
    private RadarHud hud;

    private final Minecraft mc = Minecraft.getInstance();

    // ------------------- Värit -------------------
    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    // ------------------- Suodatus -------------------
    public final SettingBoolean drawPlayers;
    public final SettingBoolean drawHostile;
    public final SettingBoolean drawPassive;
    public final SettingBoolean drawNeutral;
    public final SettingBoolean drawWater;
    public final SettingBoolean drawBoss;

    // ------------------- Perusasetukset -------------------
    public final SettingNumber radarSize;
    public final SettingNumber renderDistance;
    public final SettingNumber dotSize;
    public final SettingNumber radarScale;
    public final SettingNumber textScale;
    public final SettingNumber dotScale;

    // ------------------- Muoto -------------------
    public final SettingMode radarShape;           // SQUARE / CIRCLE
    public final SettingBoolean showEntityCircles; // piirretäänkö ympyrä entiteettien ympärille
    public final SettingNumber entityCircleSize;   // ympyrän koko (oletus 8.0)

    // ------------------- Korkeusindikaattori -------------------
    public final SettingMode heightIndicator;      // OFF / COLOR / OPACITY / LINE
    public final SettingNumber heightRange;

    // ------------------- Värit korkeusindikaattorille -------------------
    final SettingColor aboveColor;
    final SettingColor belowColor;
    final SettingColor sameLevelColor;

    // ------------------- Kompassi -------------------
    public final SettingBoolean showCompass;       // UUSI

    // ------------------- Keybind -------------------
    public final SettingKeybind toggleKey;

    public RadarModule() {
        super("Radar", "Shows nearby entities on a 2D radar", ModuleCategory.RENDER);

        // Värit ryhmille
        playerColor   = new SettingColor("Player Color",   new Color(0, 255, 200, 255));
        hostileColor  = new SettingColor("Hostile Color",  new Color(255, 50, 50, 255));
        passiveColor  = new SettingColor("Passive Color",  new Color(50, 255, 50, 255));
        neutralColor  = new SettingColor("Neutral Color",  new Color(255, 255, 0, 255));
        waterColor    = new SettingColor("Water Color",    new Color(50, 150, 255, 255));
        bossColor     = new SettingColor("Boss Color",     new Color(200, 0, 200, 255));

        // Suodatus
        drawPlayers   = new SettingBoolean("Draw Players", true);
        drawHostile   = new SettingBoolean("Draw Hostile", true);
        drawPassive   = new SettingBoolean("Draw Passive", true);
        drawNeutral   = new SettingBoolean("Draw Neutral", true);
        drawWater     = new SettingBoolean("Draw Water", true);
        drawBoss      = new SettingBoolean("Draw Boss", true);

        // Perusasetukset
        radarSize     = new SettingNumber("Radar Size", 40, 400, 1, 120);
        renderDistance = new SettingNumber("Render Distance", 16, 512, 8, 64);
        dotSize       = new SettingNumber("Dot Size", 1.0, 8.0, 0.5, 2.0);
        radarScale    = new SettingNumber("Radar Scale", 0.5, 2.0, 0.1, 1.0);
        textScale     = new SettingNumber("Text Scale", 0.5, 2.0, 0.1, 1.0);
        dotScale      = new SettingNumber("Dot Scale", 0.5, 2.0, 0.1, 1.0);

        // Muoto
        radarShape    = new SettingMode("Radar Shape", new String[]{"SQUARE", "CIRCLE"}, "SQUARE");
        showEntityCircles = new SettingBoolean("Show Entity Circles", false);
        entityCircleSize = new SettingNumber("Entity Circle Size", 3.0, 20.0, 0.5, 8.0); // pienempi oletus

        // Korkeusindikaattori
        heightIndicator = new SettingMode("Height Indicator", new String[]{"OFF", "OPACITY", "LINE"}, "OPACITY");
        heightRange    = new SettingNumber("Height Range", 1, 128, 1, 32);
        aboveColor     = new SettingColor("Above Color", new Color(255, 100, 100, 255));
        belowColor     = new SettingColor("Below Color", new Color(100, 100, 255, 255));
        sameLevelColor = new SettingColor("Same Level Color", new Color(255, 255, 255, 255));

        // Kompassi
        showCompass    = new SettingBoolean("Show Compass", true); // UUSI

        toggleKey = new SettingKeybind("Toggle Key", 0);

        // Piilotetut väriasetukset
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());
        addHiddenSetting(aboveColor.getSetting());
        addHiddenSetting(belowColor.getSetting());
        addHiddenSetting(sameLevelColor.getSetting());
        addHiddenSetting(toggleKey);

        // Näkyvät asetukset
        addSetting(radarSize);
        addSetting(renderDistance);
        addSetting(dotSize);
        addSetting(radarScale);
        addSetting(textScale);
        addSetting(dotScale);
        addSetting(radarShape);
        addSetting(showEntityCircles);
        addSetting(entityCircleSize);
        addSetting(heightIndicator);
        addSetting(heightRange);
        addSetting(showCompass); // UUSI
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);

        ensureHudRegistered();
        if (hud != null) {
            hud.setEnabled(this.isEnabled());
        }
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() { if (hud != null) hud.setEnabled(true); }

    @Override
    protected void onDisable() { if (hud != null) hud.setEnabled(false); }

    @Override
    protected void onTick() {
        if (hud == null) return;

        // Perusasetukset
        hud.setSize((int) radarSize.getValue());
        hud.setRenderDistance(renderDistance.getValue());
        hud.setDotSize(dotSize.getValue());
        hud.setRadarScale((float) radarScale.getValue());
        hud.setTextScale((float) textScale.getValue());
        hud.setDotScale((float) dotScale.getValue());

        // Suodatus
        hud.setDrawPlayers(drawPlayers.get());
        hud.setDrawHostile(drawHostile.get());
        hud.setDrawPassive(drawPassive.get());
        hud.setDrawNeutral(drawNeutral.get());
        hud.setDrawWater(drawWater.get());
        hud.setDrawBoss(drawBoss.get());

        // Muoto
        hud.setRadarShape(radarShape.getMode());
        hud.setShowEntityCircles(showEntityCircles.get());
        hud.setEntityCircleSize(entityCircleSize.getValue());

        // Korkeusindikaattori
        hud.setHeightIndicator(heightIndicator.getMode());
        hud.setHeightRange(heightRange.getValue());
        hud.setAboveColor(aboveColor.getCurrentColor().getARGB());
        hud.setBelowColor(belowColor.getCurrentColor().getARGB());
        hud.setSameLevelColor(sameLevelColor.getCurrentColor().getARGB());

        // Kompassi
        hud.setShowCompass(showCompass.get()); // UUSI

        // Värit ryhmille
        hud.setPlayerColor(playerColor.getCurrentColor().getARGB());
        hud.setHostileColor(hostileColor.getCurrentColor().getARGB());
        hud.setPassiveColor(passiveColor.getCurrentColor().getARGB());
        hud.setNeutralColor(neutralColor.getCurrentColor().getARGB());
        hud.setWaterColor(waterColor.getCurrentColor().getARGB());
        hud.setBossColor(bossColor.getCurrentColor().getARGB());
    }

    private void ensureHudRegistered() {
        if (hud == null) {
            for (HudElement e : HudManager.get().elements()) {
                if (HUD_ID.equals(e.id()) && e instanceof RadarHud) {
                    hud = (RadarHud) e;
                    return;
                }
            }
            hud = new RadarHud(this);
            HudManager.get().register(hud);
        }
    }

    public int getColorForGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> playerColor.getCurrentColor().getARGB();
            case HOSTILE -> hostileColor.getCurrentColor().getARGB();
            case PASSIVE -> passiveColor.getCurrentColor().getARGB();
            case NEUTRAL -> neutralColor.getCurrentColor().getARGB();
            case WATER   -> waterColor.getCurrentColor().getARGB();
            case BOSS    -> bossColor.getCurrentColor().getARGB();
            default      -> playerColor.getCurrentColor().getARGB();
        };
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player", playerColor),
                new NamedColor("Hostile", hostileColor),
                new NamedColor("Passive", passiveColor),
                new NamedColor("Neutral", neutralColor),
                new NamedColor("Water", waterColor),
                new NamedColor("Boss", bossColor),
                new NamedColor("Above", aboveColor),
                new NamedColor("Below", belowColor),
                new NamedColor("Same Level", sameLevelColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("radarmodule_color", "Radar Color Customizer", sw, sh, content);
    }
}