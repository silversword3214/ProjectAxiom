package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    private final Minecraft mc = Minecraft.getInstance();

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
        if (mc.player == null || mc.level == null) return;

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            TargetGroup group = TargetGroup.getGroup(entity);
            if (!HANDLED_GROUPS.contains(group)) continue;          // skip items, XP orbs, etc.
            if (!shouldDrawGroup(group)) continue;

            Vec3 entityPos = entity.position();
            if (entityPos.distanceToSqr(cameraPos) > maxDistSq) continue;

            // Interpolated position for smooth rendering
            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());

            Color color = getColorForGroup(group).getCurrentColor();

            drawSkeleton(event, x, y, z, entity, color);
        }
    }

    /**
     * Draws a stick‑figure skeleton at the entity's position.
     * All coordinates are computed as doubles and passed directly to the renderer.
     */
    private void drawSkeleton(Render3DEvent event, double x, double y, double z, Entity entity, Color color) {
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
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
        Vec3 leftShoulder = new Vec3(-halfWidth, shouldersY, 0);
        Vec3 rightShoulder = new Vec3( halfWidth, shouldersY, 0);
        Vec3 leftElbow    = new Vec3(-elbowX, shouldersY, 0);
        Vec3 rightElbow   = new Vec3( elbowX, shouldersY, 0);
        Vec3 leftHand     = new Vec3(-elbowX, handY, 0);
        Vec3 rightHand    = new Vec3( elbowX, handY, 0);

        // Legs
        float feetX = halfWidth * 0.8f;
        Vec3 leftPelvis  = new Vec3(-halfWidth * 0.6f, pelvisY, 0);
        Vec3 rightPelvis = new Vec3( halfWidth * 0.6f, pelvisY, 0);
        Vec3 leftFoot    = new Vec3(-feetX, 0, 0);
        Vec3 rightFoot   = new Vec3( feetX, 0, 0);

        // Head (cross)
        float headRadius = halfWidth * 0.6f;
        Vec3 headCenter = new Vec3(0, headTopY - headHeight/2, 0);
        Vec3 headFront  = new Vec3(0, headTopY - headHeight/2,  headRadius);
        Vec3 headBack   = new Vec3(0, headTopY - headHeight/2, -headRadius);
        Vec3 headLeft   = new Vec3(-headRadius, headTopY - headHeight/2, 0);
        Vec3 headRight  = new Vec3( headRadius, headTopY - headHeight/2, 0);
        Vec3 headTop    = new Vec3(0, headTopY, 0);
        Vec3 headBottom = new Vec3(0, headBottomY, 0);

        // Rotation
        float yawRad = entity.getYRot() * Mth.DEG_TO_RAD;
        double cosYaw = Mth.cos(yawRad);
        double sinYaw = Mth.sin(yawRad);

        java.util.function.Function<Vec3, double[]> toWorld = local -> {
            double worldX = x + local.x * cosYaw - local.z * sinYaw;
            double worldY = y + local.y;
            double worldZ = z + local.x * sinYaw + local.z * cosYaw;
            return new double[]{worldX, worldY, worldZ};
        };

        // Torso line (pelvis → shoulders)
        drawLine(event, toWorld.apply(new Vec3(0, pelvisY, 0)), toWorld.apply(new Vec3(0, shouldersY, 0)), color);

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
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("skeletonesp_color", "SkeletonESP Color Customizer", sw, sh, content);
    }
}