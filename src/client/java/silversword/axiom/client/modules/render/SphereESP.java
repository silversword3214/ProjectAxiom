package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.List;

public final class SphereESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Colors per entity type
    private final SettingColor playerColor;
    private final SettingColor hostileColor;
    private final SettingColor passiveColor;
    private final SettingColor neutralColor;
    private final SettingColor waterColor;
    private final SettingColor bossColor;

    // Filter settings
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    // General settings
    private final SettingSlider renderDistance;
    private final SettingMode sphereMode;        // "Outline", "Filled", "Both"
    private final SettingSlider lonSegments;     // number of longitudinal segments (default 24)
    private final SettingSlider latSegments;     // number of latitudinal segments (default 16)
    private final SettingMode radiusMode;        // "Width", "Height", "Max", "Min", "Custom"
    private final SettingNumber customRadius;    // custom radius in blocks (only if radiusMode == "Custom")

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public SphereESP() {
        super("SphereESP", "Draws spheres around entities", ModuleCategory.RENDER);

        // Colors
        playerColor  = new SettingColor("Player Color",  new Color(0, 255, 200, 180));
        hostileColor = new SettingColor("Hostile Color", new Color(255, 50, 50, 180));
        passiveColor = new SettingColor("Passive Color", new Color(50, 255, 50, 180));
        neutralColor = new SettingColor("Neutral Color", new Color(255, 255, 0, 180));
        waterColor   = new SettingColor("Water Color",   new Color(50, 150, 255, 180));
        bossColor    = new SettingColor("Boss Color",    new Color(200, 0, 200, 180));

        // Filters
        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", true);
        drawNeutral = new SettingBoolean("Draw Neutral", true);
        drawWater   = new SettingBoolean("Draw Water",   true);
        drawBoss    = new SettingBoolean("Draw Boss",    true);

        // Appearance settings
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        sphereMode = new SettingMode("Sphere Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");
        lonSegments = new SettingSlider("Longitude Segments", new double[]{8, 12, 16, 24, 32, 48, 64}, 24);
        latSegments = new SettingSlider("Latitude Segments",  new double[]{6, 8, 12, 16, 24, 32, 48}, 16);
        radiusMode = new SettingMode("Radius Mode", new String[]{"Width", "Height", "Max", "Min", "Custom"}, "Width");
        customRadius = new SettingNumber("Custom Radius", 0.1, 5.0, 0.1, 1);

        // Hidden settings (for persistence)
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());
        addHiddenSetting(toggleKey);

        // Visible settings
        addSetting(sphereMode);
        addSetting(renderDistance);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
        addSetting(lonSegments);
        addSetting(latSegments);
        addSetting(radiusMode);
        addSetting(customRadius);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            TargetGroup group = TargetGroup.getGroup(entity);
            if (!shouldDrawGroup(group)) continue;

            Vec3 entityPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            if (entityPos.distanceToSqr(cameraPos) > maxDistSq) continue;

            // Interpolated base position (foot)
            double x = entity.xOld + (entity.getX() - entity.xOld) * event.tickDelta;
            double y = entity.yOld + (entity.getY() - entity.yOld) * event.tickDelta;
            double z = entity.zOld + (entity.getZ() - entity.zOld) * event.tickDelta;

            // Keskikohta (puoliväli korkeudesta)
            double centerY = y + entity.getBbHeight() / 2.0;

            double radius = getRadius(entity);

            Color baseColor = getColorForGroup(group).getCurrentColor();
            int argbColor = baseColor.getARGB();

            ShapeModeEnum mode = switch (sphereMode.getMode()) {
                case "Filled" -> ShapeModeEnum.SIDES;
                case "Both"   -> ShapeModeEnum.BOTH;
                default       -> ShapeModeEnum.LINES;
            };

            int lon = (int) lonSegments.getValue();
            int lat = (int) latSegments.getValue();

            int fillColor = new Color(baseColor.r, baseColor.g, baseColor.b, 25).getARGB();
            int outlineColor = baseColor.getARGB();
            event.getRenderer().drawSphere(x, centerY, z, radius, fillColor, outlineColor, mode, lon, lat);
        }
    }

    private boolean shouldDrawGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER   -> drawWater.get();
            case BOSS    -> drawBoss.get();
            default      -> true;
        };
    }

    private SettingColor getColorForGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> playerColor;
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER   -> waterColor;
            case BOSS    -> bossColor;
            default      -> playerColor; // fallback
        };
    }

    /**
     * Calculates the sphere radius based on the selected radius mode.
     */
    private double getRadius(Entity entity) {
        double width  = entity.getBbWidth();
        double height = entity.getBbHeight();

        return switch (radiusMode.getMode()) {
            case "Width"  -> width / 2.0;
            case "Height" -> height / 2.0;
            case "Max"    -> Math.max(width, height) / 2.0;
            case "Min"    -> Math.min(width, height) / 2.0;
            case "Custom" -> customRadius.getValue();
            default       -> width / 2.0;
        };
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player",  playerColor),
                new NamedColor("Hostile", hostileColor),
                new NamedColor("Passive", passiveColor),
                new NamedColor("Neutral", neutralColor),
                new NamedColor("Water",   waterColor),
                new NamedColor("Boss",    bossColor)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("sphereesp_color", "SphereESP Color Customizer", sw, sh, content);
    }

    @Override
    protected void onTick() {

    }
}