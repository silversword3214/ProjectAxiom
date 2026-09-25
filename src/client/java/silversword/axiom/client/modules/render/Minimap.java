package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.render.MinimapHud;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.axiomrenderer.minimap.MinimapRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;

public final class Minimap extends AxiomMod {

    private final SettingNumber  hudSize;
    private final SettingNumber  resolution;
    private final SettingNumber  viewRadius;
    private final SettingNumber  zoom;
    private final SettingMode    shape;
    private final SettingSlider  updateHz;
    private final SettingBoolean rotateWithPlayer;
    private final SettingBoolean heightShading;
    private final SettingBoolean showHud;
    private final SettingNumber  borderThickness;
    private final SettingColor   borderColor;

    // Mob
    private final SettingNumber  dotScale;
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    public Minimap() {
        super("Minimap", "Xaero-style top-down minimap", ModuleCategory.RENDER);

        hudSize          = new SettingNumber("HUD Size", 64, 512, 16, 128);
        resolution       = new SettingNumber("Resolution", 128, 1024, 32, 512);
        viewRadius       = new SettingNumber("View Radius", 16, 256, 4, 64);
        zoom             = new SettingNumber("Zoom", 0.25, 4.0, 0.05, 1.0);
        shape            = new SettingMode("Shape", new String[]{"SQUARE", "CIRCLE"}, "CIRCLE");
        updateHz         = new SettingSlider("Update Rate (Hz)",
                new double[]{5, 10, 15, 20, 30, 60}, 60);
        rotateWithPlayer = new SettingBoolean("Rotate With Player", true);
        heightShading    = new SettingBoolean("Height Shading", true);
        showHud          = new SettingBoolean("Show HUD Element", true);

        // Mob
        dotScale     = new SettingNumber("Dot Scale", 0.5, 6.0, 0.1, 3.0);
        drawPlayers  = new SettingBoolean("Draw Players", true);
        drawHostile  = new SettingBoolean("Draw Hostile", true);
        drawPassive  = new SettingBoolean("Draw Passive", true);
        drawNeutral  = new SettingBoolean("Draw Neutral", true);
        drawWater    = new SettingBoolean("Draw Water",   true);
        drawBoss     = new SettingBoolean("Draw Boss",    true);

        playerColor  = new SettingColor("Player Color",  new Color(  0, 255, 200, 255));
        hostileColor = new SettingColor("Hostile Color", new Color(255,  50,  50, 255));
        passiveColor = new SettingColor("Passive Color", new Color( 50, 255,  50, 255));
        neutralColor = new SettingColor("Neutral Color", new Color(255, 255,   0, 255));
        waterColor   = new SettingColor("Water Color",   new Color( 50, 150, 255, 255));
        bossColor    = new SettingColor("Boss Color",    new Color(200,   0, 200, 255));

        borderThickness = new SettingNumber("Border Thickness", 0.0, 6.0, 0.1, 1.5);
        borderColor     = new SettingColor("Border Color", new Color(170, 170, 170, 255));

        addHiddenSetting(borderColor.getSetting());

        addSetting(borderThickness);

        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());


        addSetting(dotScale);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);

        addSetting(hudSize);
        addSetting(resolution);
        addSetting(viewRadius);
        addSetting(zoom);
        addSetting(shape);
        addSetting(updateHz);
        addSetting(rotateWithPlayer);
        addSetting(heightShading);
        addSetting(showHud);
    }

    @Override
    protected void onEnable() {
        MinimapRenderer.init();
        MinimapRenderer.setEnabled(true);
        applyHudState(true);
        applySettings();
    }

    @Override
    protected void onDisable() {
        MinimapRenderer.setEnabled(false);
        applyHudState(false);
    }

    @Override
    protected void onTick() {
        MinimapRenderer.renderMinimapView();
        applyHudState(showHud.get());
        applySettings();
    }

    private void applyHudState(boolean on) {
        MinimapHud hud = findHud();
        if (hud == null) return;
        hud.setEnabled(on);
        hud.setViewportSize((int) hudSize.getValue());
    }

    private void applySettings() {
        MinimapRenderer.setRtSize((int) resolution.getValue());
        MinimapRenderer.setViewRadius((float) viewRadius.getValue());
        MinimapRenderer.setZoom((float) zoom.getValue());
        MinimapRenderer.setUpdateHz((int) updateHz.getValue());
        MinimapRenderer.setRotateWithPlayer(rotateWithPlayer.get());
        MinimapRenderer.setCircular("CIRCLE".equals(shape.getMode()));
        MinimapRenderer.setHeightShading(heightShading.get());
        MinimapRenderer.setBorderThickness((float) borderThickness.getValue());
        MinimapRenderer.setBorderColor(borderColor.getCurrentColor().getARGB());

        // Mob
        MinimapRenderer.setDotScale((float) dotScale.getValue());
        MinimapRenderer.setDrawPlayers(drawPlayers.get());
        MinimapRenderer.setDrawHostile(drawHostile.get());
        MinimapRenderer.setDrawPassive(drawPassive.get());
        MinimapRenderer.setDrawNeutral(drawNeutral.get());
        MinimapRenderer.setDrawWater(drawWater.get());
        MinimapRenderer.setDrawBoss(drawBoss.get());

        MinimapRenderer.setPlayerColor(playerColor.getCurrentColor().getARGB());
        MinimapRenderer.setHostileColor(hostileColor.getCurrentColor().getARGB());
        MinimapRenderer.setPassiveColor(passiveColor.getCurrentColor().getARGB());
        MinimapRenderer.setNeutralColor(neutralColor.getCurrentColor().getARGB());
        MinimapRenderer.setWaterColor(waterColor.getCurrentColor().getARGB());
        MinimapRenderer.setBossColor(bossColor.getCurrentColor().getARGB());
    }

    private MinimapHud findHud() {
        for (HudElement e : HudManager.get().elements()) {
            if (e instanceof MinimapHud hud) return hud;
        }
        return null;
    }
}