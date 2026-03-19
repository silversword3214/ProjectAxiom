package silversword.axiom.client.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SkeletonESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Colors for each entity group
    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    // Filter settings
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    private final SettingSlider renderDistance;
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Set of groups we actually handle (skip unknown ones like items)
    private static final Set<TargetGroup> HANDLED_GROUPS = EnumSet.of(
            TargetGroup.PLAYER,
            TargetGroup.HOSTILE,
            TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL,
            TargetGroup.WATER,
            TargetGroup.BOSS
    );

    public SkeletonESP() {
        super("SkeletonESP", "Draws a stick‑figure skeleton in entities", ModuleCategory.RENDER);

        playerColor   = new SettingColor("Player Color",   new Color(0, 255, 200, 255));
        hostileColor  = new SettingColor("Hostile Color",  new Color(255, 50, 50, 255));
        passiveColor  = new SettingColor("Passive Color",  new Color(50, 255, 50, 255));
        neutralColor  = new SettingColor("Neutral Color",  new Color(255, 255, 0, 255));
        waterColor    = new SettingColor("Water Color",    new Color(50, 150, 255, 255));
        bossColor     = new SettingColor("Boss Color",     new Color(200, 0, 200, 255));

        drawPlayers   = new SettingBoolean("Draw Players", true);
        drawHostile   = new SettingBoolean("Draw Hostile", true);
        drawPassive   = new SettingBoolean("Draw Passive", true);
        drawNeutral   = new SettingBoolean("Draw Neutral", true);
        drawWater     = new SettingBoolean("Draw Water", true);
        drawBoss      = new SettingBoolean("Draw Boss", true);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);

        // Hidden settings
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());
        addHiddenSetting(toggleKey);

        // Visible settings
        addSetting(renderDistance);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        // Nothing needed
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            TargetGroup group = TargetGroup.getGroup(entity);
            if (!HANDLED_GROUPS.contains(group)) continue;          // skip items, XP orbs, etc.
            if (!shouldDrawGroup(group)) continue;

            Vec3d entityPos = entity.getEntityPos();
            if (entityPos.squaredDistanceTo(cameraPos) > maxDistSq) continue;

            // Interpolated position for smooth rendering
            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());

            Color color = getColorForGroup(group).getCurrentColor();

            drawSkeleton(event, x, y, z, entity, color);
        }
    }

    /**
     * Draws a stick‑figure skeleton at the entity's position.
     * All coordinates are computed as doubles and passed directly to the renderer.
     */
    private void drawSkeleton(Render3DEvent event, double x, double y, double z, Entity entity, Color color) {
        float width = entity.getWidth();
        float height = entity.getHeight();
        float halfWidth = width / 2.0f;

        // Head size
        float headHeight = Math.max(0.2f, height * 0.2f);
        float headTopY = height;
        float headBottomY = height - headHeight;

        // Torso
        float shouldersY = headBottomY;
        float pelvisY = height * 0.4f;

        // Arms: shoulder → elbow (horizontal) → hand (vertical)
        float elbowX = halfWidth * 1.5f;
        float handY = pelvisY * 0.8f;
        Vec3d leftShoulder = new Vec3d(-halfWidth, shouldersY, 0);
        Vec3d rightShoulder = new Vec3d( halfWidth, shouldersY, 0);
        Vec3d leftElbow    = new Vec3d(-elbowX, shouldersY, 0);
        Vec3d rightElbow   = new Vec3d( elbowX, shouldersY, 0);
        Vec3d leftHand     = new Vec3d(-elbowX, handY, 0);
        Vec3d rightHand    = new Vec3d( elbowX, handY, 0);

        // Legs
        float feetX = halfWidth * 0.8f;
        Vec3d leftPelvis  = new Vec3d(-halfWidth * 0.6f, pelvisY, 0);
        Vec3d rightPelvis = new Vec3d( halfWidth * 0.6f, pelvisY, 0);
        Vec3d leftFoot    = new Vec3d(-feetX, 0, 0);
        Vec3d rightFoot   = new Vec3d( feetX, 0, 0);

        // Head (cross)
        float headRadius = halfWidth * 0.6f;
        Vec3d headCenter = new Vec3d(0, headTopY - headHeight/2, 0);
        Vec3d headFront  = new Vec3d(0, headTopY - headHeight/2,  headRadius);
        Vec3d headBack   = new Vec3d(0, headTopY - headHeight/2, -headRadius);
        Vec3d headLeft   = new Vec3d(-headRadius, headTopY - headHeight/2, 0);
        Vec3d headRight  = new Vec3d( headRadius, headTopY - headHeight/2, 0);
        Vec3d headTop    = new Vec3d(0, headTopY, 0);
        Vec3d headBottom = new Vec3d(0, headBottomY, 0);

        // Rotation
        float yawRad = entity.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double cosYaw = MathHelper.cos(yawRad);
        double sinYaw = MathHelper.sin(yawRad);

        java.util.function.Function<Vec3d, double[]> toWorld = local -> {
            double worldX = x + local.x * cosYaw - local.z * sinYaw;
            double worldY = y + local.y;
            double worldZ = z + local.x * sinYaw + local.z * cosYaw;
            return new double[]{worldX, worldY, worldZ};
        };

        // Torso line (pelvis → shoulders)
        drawLine(event, toWorld.apply(new Vec3d(0, pelvisY, 0)), toWorld.apply(new Vec3d(0, shouldersY, 0)), color);

        // Shoulder line (left shoulder ↔ right shoulder)
        drawLine(event, toWorld.apply(leftShoulder), toWorld.apply(rightShoulder), color);

        // Hip line (left pelvis ↔ right pelvis)
        drawLine(event, toWorld.apply(leftPelvis), toWorld.apply(rightPelvis), color);

        // Legs
        drawLine(event, toWorld.apply(leftPelvis), toWorld.apply(leftFoot), color);
        drawLine(event, toWorld.apply(rightPelvis), toWorld.apply(rightFoot), color);

        // Arms
        drawLine(event, toWorld.apply(leftShoulder), toWorld.apply(leftElbow), color);
        drawLine(event, toWorld.apply(leftElbow), toWorld.apply(leftHand), color);
        drawLine(event, toWorld.apply(rightShoulder), toWorld.apply(rightElbow), color);
        drawLine(event, toWorld.apply(rightElbow), toWorld.apply(rightHand), color);

        // Head cross
        drawLine(event, toWorld.apply(headFront), toWorld.apply(headBack), color);
        drawLine(event, toWorld.apply(headLeft), toWorld.apply(headRight), color);
        drawLine(event, toWorld.apply(headTop), toWorld.apply(headBottom), color);
    }

    // Helper to call the correct line method with double coordinates
    private void drawLine(Render3DEvent event, double[] from, double[] to, Color color) {
        event.render.drawLine(from[0], from[1], from[2], to[0], to[1], to[2], color);
    }

    private boolean shouldDrawGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER   -> drawWater.get();
            case BOSS    -> drawBoss.get();
            default      -> true; // should not happen because we filter unknown groups
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

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Player", playerColor),
                new NamedColor("Hostile", hostileColor),
                new NamedColor("Passive", passiveColor),
                new NamedColor("Neutral", neutralColor),
                new NamedColor("Water", waterColor),
                new NamedColor("Boss", bossColor)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("skeletonesp_color", "SkeletonESP Color Customizer", sw, sh, content);
    }
}