package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
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
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import java.util.Arrays;
import java.util.List;

public final class BoxESP extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Värit (package-private, jotta näkyvät customizerissa)
    final SettingColor playerColor;
    final SettingColor hostileColor;
    final SettingColor passiveColor;
    final SettingColor neutralColor;
    final SettingColor waterColor;
    final SettingColor bossColor;

    // Suodatusasetukset
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    private final SettingSlider renderDistance;
    private final SettingMode boxMode; // "Outline", "Filled", "Both"
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);


    public BoxESP() {
        super("BoxESP", "Draws boxes around entities", ModuleCategory.RENDER);

        playerColor   = new SettingColor("Player Color",   new Color(0, 255, 200, 180));
        hostileColor  = new SettingColor("Hostile Color",  new Color(255, 50, 50, 180));
        passiveColor  = new SettingColor("Passive Color",  new Color(50, 255, 50, 180));
        neutralColor  = new SettingColor("Neutral Color",  new Color(255, 255, 0, 180));
        waterColor    = new SettingColor("Water Color",    new Color(50, 150, 255, 180));
        bossColor     = new SettingColor("Boss Color",     new Color(200, 0, 200, 180));

        drawPlayers   = new SettingBoolean("Draw Players", true);
        drawHostile   = new SettingBoolean("Draw Hostile", true);
        drawPassive   = new SettingBoolean("Draw Passive", true);
        drawNeutral   = new SettingBoolean("Draw Neutral", true);
        drawWater     = new SettingBoolean("Draw Water", true);
        drawBoss      = new SettingBoolean("Draw Boss", true);

        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        boxMode = new SettingMode("Box Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");

        // Piilotetut väriasetukset (tallentuvat, eivät näy asetuslistassa)
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());

        addHiddenSetting(toggleKey);

        // Näkyvät asetukset (ei enää rainbowSpeed)
        addSetting(boxMode);
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
        // Ei tarvita erillistä rainbow-päivitystä – värit lasketaan lennossa getCurrentColor():ssa
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
            if (!shouldDrawGroup(group)) continue;

            Vec3 entityPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            if (entityPos.distanceToSqr(cameraPos) > maxDistSq) continue;

            double x = entity.xOld + (entity.getX() - entity.xOld) * event.tickDelta;
            double y = entity.yOld + (entity.getY() - entity.yOld) * event.tickDelta;
            double z = entity.zOld + (entity.getZ() - entity.zOld) * event.tickDelta;

            double halfWidth = entity.getBbWidth() / 2.0;
            double height = entity.getBbHeight();

            Color baseColor = getColorForGroup(group).getCurrentColor(); // <-- suoraan getCurrentColor()

            Color sideColor = new Color(baseColor.r, baseColor.g, baseColor.b, 30);
            Color lineColor = baseColor;

            ShapeMode mode = switch (boxMode.getMode()) {
                case "Filled" -> ShapeMode.Sides;
                case "Both"   -> ShapeMode.Both;
                default       -> ShapeMode.Lines;
            };

            event.render.drawBox(
                    x - halfWidth, y, z - halfWidth,
                    x + halfWidth, y + height, z + halfWidth,
                    sideColor, lineColor, mode, 0
            );
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
        factory.openCustomWindow("boxesp_color", "BoxESP Color Customizer", sw, sh, content);
    }
}