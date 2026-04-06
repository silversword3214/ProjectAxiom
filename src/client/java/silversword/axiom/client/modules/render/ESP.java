package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    // ── Keybind ──────────────────────────────────────────────────────────────
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // ── ESP-tyypit (parent-booleanit) ─────────────────────────────────────────
    private final SettingBoolean enableBox;
    private final SettingBoolean enableSphere;
    private final SettingBoolean enableSkeleton;

    // ── Box-asetukset (subsettings) ──────────────────────────────────────────
    private final SettingMode    boxMode;

    // ── Sphere-asetukset (subsettings) ───────────────────────────────────────
    private final SettingMode    sphereMode;
    private final SettingSlider  lonSegments;
    private final SettingSlider  latSegments;
    private final SettingMode    radiusMode;
    private final SettingNumber  customRadius;

    // ── Yhteiset asetukset ───────────────────────────────────────────────────
    private final SettingSlider  renderDistance;
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    // ── Värit (piilotetut, avautuvat Color Editor -napista) ──────────────────
    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    private static final Set<TargetGroup> HANDLED_GROUPS = EnumSet.of(
            TargetGroup.PLAYER, TargetGroup.HOSTILE, TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL, TargetGroup.WATER,  TargetGroup.BOSS);

    public ESP() {
        super("ESP", "Render entities through walls", ModuleCategory.RENDER);

        // ── ESP-tyyppien togglet ─────────────────────────────────────────────
        enableBox      = new SettingBoolean("Box ESP",      true);
        enableSphere   = new SettingBoolean("Sphere ESP",   false);
        enableSkeleton = new SettingBoolean("Skeleton ESP", false);

        // ── Box subsettings ──────────────────────────────────────────────────
        boxMode = new SettingMode("Box Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");
        boxMode.setParent(enableBox);

        // ── Sphere subsettings ───────────────────────────────────────────────
        sphereMode   = new SettingMode("Sphere Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");
        lonSegments  = new SettingSlider("Lon Segments",  new double[]{8,12,16,24,32,48,64}, 24);
        latSegments  = new SettingSlider("Lat Segments",  new double[]{6,8,12,16,24,32,48},  16);
        radiusMode   = new SettingMode("Radius Mode", new String[]{"Width","Height","Max","Min","Custom"}, "Width");
        customRadius = new SettingNumber("Custom Radius", 0.1, 5.0, 0.1, 1.0);
        sphereMode  .setParent(enableSphere);
        lonSegments .setParent(enableSphere);
        latSegments .setParent(enableSphere);
        radiusMode  .setParent(enableSphere);
        customRadius.setParent(enableSphere);

        // ── Yhteiset ─────────────────────────────────────────────────────────
        renderDistance = new SettingSlider("Render Distance",
                new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", true);
        drawNeutral = new SettingBoolean("Draw Neutral", true);
        drawWater   = new SettingBoolean("Draw Water",   true);
        drawBoss    = new SettingBoolean("Draw Boss",    true);

        // ── Värit ─────────────────────────────────────────────────────────────
        playerColor  = new SettingColor("Player Color",  new Color(0,   255, 200, 180));
        hostileColor = new SettingColor("Hostile Color", new Color(255,  50,  50, 180));
        passiveColor = new SettingColor("Passive Color", new Color(50,  255,  50, 180));
        neutralColor = new SettingColor("Neutral Color", new Color(255, 255,   0, 180));
        waterColor   = new SettingColor("Water Color",   new Color(50,  150, 255, 180));
        bossColor    = new SettingColor("Boss Color",    new Color(200,   0, 200, 180));

        // ── Rekisteröinti ─────────────────────────────────────────────────────

        // Näkyvät asetukset — järjestys määrää UI-järjestyksen
        addSetting(enableBox);
        addSetting(boxMode);          // subsetting (parent = enableBox)

        addSetting(enableSphere);
        addSetting(sphereMode);       // subsettings (parent = enableSphere)
        addSetting(lonSegments);
        addSetting(latSegments);
        addSetting(radiusMode);
        addSetting(customRadius);

        addSetting(enableSkeleton);   // skeleton ei tarvitse lisäasetuksia

        addSetting(renderDistance);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);

        // Piilotetut (tallennetaan, eivät UI:ssa)
        addHiddenSetting(playerColor .getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor  .getSetting());
        addHiddenSetting(bossColor   .getSetting());
        addHiddenSetting(toggleKey);
    }

    // ── Keybind ───────────────────────────────────────────────────────────────

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onTick() {}

    // ── Pää-render ────────────────────────────────────────────────────────────

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 camera = mc.gameRenderer.getMainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            TargetGroup group = TargetGroup.getGroup(entity);
            if (!HANDLED_GROUPS.contains(group)) continue;
            if (!shouldDrawGroup(group)) continue;

            if (entity.position().distanceToSqr(camera) > maxDistSq) continue;

            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());

            Color base = getColorForGroup(group).getCurrentColor();

            if (enableBox.get())      renderBox     (event, x, y, z, entity, base);
            if (enableSphere.get())   renderSphere  (event, x, y, z, entity, base);
            if (enableSkeleton.get()) renderSkeleton(event, x, y, z, entity, base);
        }
    }

    // ── Box ───────────────────────────────────────────────────────────────────

    private void renderBox(Render3DEvent event,
                           double x, double y, double z,
                           Entity entity, Color base) {
        double hw = entity.getBbWidth()  / 2.0;
        double h  = entity.getBbHeight();

        ShapeModeEnum mode = resolveShapeMode(boxMode.getMode());
        int side = new Color(base.r, base.g, base.b, 20).getARGB();
        int line = base.getARGB();

        event.getRenderer().drawBox(
                x - hw, y, z - hw,
                x + hw, y + h, z + hw,
                side, line, mode, 0);
    }

    // ── Sphere ────────────────────────────────────────────────────────────────

    private void renderSphere(Render3DEvent event,
                              double x, double y, double z,
                              Entity entity, Color base) {
        double centerY = y + entity.getBbHeight() / 2.0;
        double radius  = calcRadius(entity);

        ShapeModeEnum mode  = resolveShapeMode(sphereMode.getMode());
        int fill    = new Color(base.r, base.g, base.b, 25).getARGB();
        int outline = base.getARGB();
        int lon = (int) lonSegments.getValue();
        int lat = (int) latSegments.getValue();

        event.getRenderer().drawSphere(x, centerY, z, radius, fill, outline, mode, lon, lat);
    }

    private double calcRadius(Entity entity) {
        double w = entity.getBbWidth();
        double h = entity.getBbHeight();
        return switch (radiusMode.getMode()) {
            case "Height" -> h / 2.0;
            case "Max"    -> Math.max(w, h) / 2.0;
            case "Min"    -> Math.min(w, h) / 2.0;
            case "Custom" -> customRadius.getValue();
            default       -> w / 2.0;   // "Width"
        };
    }

    // ── Skeleton ──────────────────────────────────────────────────────────────

    private void renderSkeleton(Render3DEvent event,
                                double x, double y, double z,
                                Entity entity, Color color) {
        float width     = entity.getBbWidth();
        float height    = entity.getBbHeight();
        float halfWidth = width / 2.0f;

        float headHeight  = Math.max(0.2f, height * 0.2f);
        float headTopY    = height;
        float headBottomY = height - headHeight;
        float shouldersY  = headBottomY;
        float pelvisY     = height * 0.4f;
        float elbowX      = halfWidth * 1.5f;
        float handY       = pelvisY  * 0.8f;
        float feetX       = halfWidth * 0.8f;
        float headRadius  = halfWidth * 0.6f;
        float headMidY    = headTopY - headHeight / 2;

        float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;
        double cosY = Mth.cos(yawRad);
        double sinY = Mth.sin(yawRad);

        // Converts local (lx, ly, lz) offset to world double[3]
        // (rotation only around Y-axis)
        var w = (java.util.function.Function<Vec3, double[]>) local -> new double[]{
                x + local.x * cosY - local.z * sinY,
                y + local.y,
                z + local.x * sinY + local.z * cosY
        };

        // Torso
        line(event, w.apply(new Vec3(0, pelvisY,   0)), w.apply(new Vec3(0, shouldersY, 0)), color);
        // Shoulders
        line(event, w.apply(new Vec3(-halfWidth, shouldersY, 0)), w.apply(new Vec3(halfWidth, shouldersY, 0)), color);
        // Hips
        line(event, w.apply(new Vec3(-halfWidth * 0.6f, pelvisY, 0)), w.apply(new Vec3(halfWidth * 0.6f, pelvisY, 0)), color);
        // Legs
        line(event, w.apply(new Vec3(-halfWidth * 0.6f, pelvisY, 0)), w.apply(new Vec3(-feetX, 0, 0)), color);
        line(event, w.apply(new Vec3( halfWidth * 0.6f, pelvisY, 0)), w.apply(new Vec3( feetX, 0, 0)), color);
        // Arms
        line(event, w.apply(new Vec3(-halfWidth, shouldersY, 0)), w.apply(new Vec3(-elbowX, shouldersY, 0)), color);
        line(event, w.apply(new Vec3(-elbowX,    shouldersY, 0)), w.apply(new Vec3(-elbowX, handY,      0)), color);
        line(event, w.apply(new Vec3( halfWidth, shouldersY, 0)), w.apply(new Vec3( elbowX, shouldersY, 0)), color);
        line(event, w.apply(new Vec3( elbowX,    shouldersY, 0)), w.apply(new Vec3( elbowX, handY,      0)), color);
        // Head cross
        line(event, w.apply(new Vec3(0, headMidY,  headRadius)), w.apply(new Vec3(0, headMidY, -headRadius)), color);
        line(event, w.apply(new Vec3(-headRadius, headMidY, 0)), w.apply(new Vec3( headRadius, headMidY, 0)), color);
        line(event, w.apply(new Vec3(0, headTopY,         0)), w.apply(new Vec3(0, headBottomY, 0)), color);
    }

    private void line(Render3DEvent event, double[] a, double[] b, Color c) {
        event.getRenderer().drawLine(a[0], a[1], a[2], b[0], b[1], b[2], c);
    }

    // ── Apumetodit ────────────────────────────────────────────────────────────

    private static ShapeModeEnum resolveShapeMode(String mode) {
        return switch (mode) {
            case "Filled" -> ShapeModeEnum.SIDES;
            case "Both"   -> ShapeModeEnum.BOTH;
            default       -> ShapeModeEnum.LINES;
        };
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
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER   -> waterColor;
            case BOSS    -> bossColor;
            default      -> playerColor;
        };
    }

    // ── ColorConfigurable ────────────────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player",  playerColor),
                new NamedColor("Hostile", hostileColor),
                new NamedColor("Passive", passiveColor),
                new NamedColor("Neutral", neutralColor),
                new NamedColor("Water",   waterColor),
                new NamedColor("Boss",    bossColor));
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        factory.openCustomWindow("esp_color", "ESP Color Customizer",
                sw, sh, new ColorCustomizerView(this));
    }
}
