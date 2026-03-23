package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
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
import silversword.axiom.client.eventbus.Subscribe;          // korjattu: @Subscribe
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;  // korjattu: import
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import java.util.Arrays;
import java.util.List;

public final class Tracers extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Värit (SettingColor)
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
    private final SettingMode targetPoint; // "Head" tai "Body"
    private final SettingBoolean drawPillar;
    private final SettingBoolean drawBehind; // UUSI: piirretäänkö myös kameran takana olevat

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Tracers() {
        super("Tracers", "Draws lines towards entities", ModuleCategory.RENDER);

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
        targetPoint    = new SettingMode("Target Point", new String[]{"Body", "Head"}, "Body");
        drawPillar     = new SettingBoolean("Draw Stem", true);
        drawBehind     = new SettingBoolean("Draw Behind", true); // Oletus true

        // Piilotetut väriasetukset
        addHiddenSetting(playerColor.getSetting());
        addHiddenSetting(hostileColor.getSetting());
        addHiddenSetting(passiveColor.getSetting());
        addHiddenSetting(neutralColor.getSetting());
        addHiddenSetting(waterColor.getSetting());
        addHiddenSetting(bossColor.getSetting());

        addHiddenSetting(toggleKey);

        // Näkyvät asetukset
        addSetting(renderDistance);
        addSetting(targetPoint);
        addSetting(drawPillar);
        addSetting(drawBehind);
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
        // Ei tarvita
    }

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Vec3 cameraDir = mc.player.getViewVector(event.getTickDelta());
        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();

        // Käytä renderöijää tapahtumasta
        Renderer3D renderer = event.getRenderer();

        // Lähtöpiste (ruudun keskipiste maailmassa) – päivittyy Renderer3D:ssä
        Vec3 start = RenderUtils.center;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            TargetGroup group = TargetGroup.getGroup(entity);
            if (!shouldDrawGroup(group)) continue;

            Vec3 entityPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());

            if (entityPos.distanceToSqr(cameraPos) > maxDistSq) continue;

            // Tarkista katsesuunta vain, jos drawBehind on false
            if (!drawBehind.get()) {
                Vec3 toEntity = entityPos.subtract(cameraPos).normalize();
                if (toEntity.dot(cameraDir) <= 0) continue;
            }

            double x = entity.xOld + (entity.getX() - entity.xOld) * event.getTickDelta();
            double y = entity.yOld + (entity.getY() - entity.yOld) * event.getTickDelta();
            double z = entity.zOld + (entity.getZ() - entity.zOld) * event.getTickDelta();

            if ("Head".equals(targetPoint.getMode())) {
                y += entity.getBbHeight() * 0.9;
            } else {
                y += entity.getBbHeight() * 0.5;
            }

            // Haetaan väri ARGB-int
            int color = getColorForGroup(group).getCurrentColor().getARGB();

            // Piirrä tracer-viiva (oletuspaksuus 1.0f)
            renderer.drawLine(start.x, start.y, start.z, x, y, z, color, 1.0f);

            if (drawPillar.get()) {
                double groundY = entity.getY();
                double topY = groundY + entity.getBbHeight();
                renderer.drawLine(x, groundY, z, x, topY, z, color, 1.0f);
            }
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

    // Palautetaan SettingColor, jotta voidaan kutsua getCurrentColor()
    private SettingColor getColorForGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER  -> playerColor;
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER   -> waterColor;
            case BOSS    -> bossColor;
            default      -> playerColor;
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
        factory.openCustomWindow("tracers_color", "Tracers Color Customizer", sw, sh, content);
    }
}