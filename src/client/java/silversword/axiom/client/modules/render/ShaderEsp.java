package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.axiomrenderer.shaderesp.ShaderEspRenderer;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingSlider;

import java.util.EnumSet;
import java.util.Set;

public final class ShaderEsp extends AxiomMod {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Set<TargetGroup> HANDLED = EnumSet.of(
            TargetGroup.PLAYER, TargetGroup.HOSTILE, TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL, TargetGroup.WATER, TargetGroup.BOSS);

    // ─── Chams ─────────────────────────────────────────────────────────────
    private final SettingBoolean enableChams;
    private final SettingSlider  chamsTint;

    // ─── Outline ───────────────────────────────────────────────────────────
    private final SettingBoolean enableOutline;
    private final SettingSlider  outlineColor;

    // ─── Yhteiset ──────────────────────────────────────────────────────────
    private final SettingSlider  downscale;
    private final SettingSlider  renderDistance;

    // ─── Entiteettityypit ──────────────────────────────────────────────────
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;

    public ShaderEsp() {
        super("ShaderEsp",
                "Chams (see-through entities) and Outline (edge-detection)",
                ModuleCategory.RENDER);

        // ─── Chams ─────────────────────────────────────────────────────────
        enableChams = new SettingBoolean("Chams", true);
        chamsTint = new SettingSlider("Chams Tint",
                new double[]{
                        0xFFFFFFFFL,  // valkoinen (alkuperäinen)
                        0xCCFF0000L,  // punainen 80 %
                        0xCC00FF00L,  // vihreä 80 %
                        0xCC0000FFL,  // sininen 80 %
                        0xCCFF00FFL,  // magenta 80 %
                        0xCC00FFFFL,  // cyan 80 %
                        0xCCFFFF00L,  // keltainen 80 %
                        0x80000000L   // musta 50 %
                }, 0);

        // ─── Outline ───────────────────────────────────────────────────────
        enableOutline = new SettingBoolean("Outline", true);
        outlineColor = new SettingSlider("Outline Color",
                new double[]{
                        0xFFFFFFFFL,  // valkoinen
                        0xFF00FFFFL,  // cyan
                        0xFFFF00FFL,  // magenta
                        0xFFFF0000L,  // punainen
                        0xFF00FF00L,  // vihreä
                        0xFF0000FFL,  // sininen
                        0xFFFFFF00L   // keltainen
                }, 0);

        // ─── Yhteiset ──────────────────────────────────────────────────────
        downscale = new SettingSlider("Mask Resolution",
                new double[]{1, 2, 3, 4}, 2);
        renderDistance = new SettingSlider("Render Distance",
                new double[]{16, 32, 64, 96, 128, 256, 512}, 128);

        // ─── Entiteettityypit ──────────────────────────────────────────────
        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", false);
        drawNeutral = new SettingBoolean("Draw Neutral", false);
        drawWater   = new SettingBoolean("Draw Water",   false);
        drawBoss    = new SettingBoolean("Draw Boss",    true);

        // ─── Rekisteröinti (UI-järjestys) ─────────────────────────────────
        addSetting(enableChams);
        addSetting(chamsTint);

        addSetting(enableOutline);
        addSetting(outlineColor);

        addSetting(downscale);
        addSetting(renderDistance);

        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
    }

    // ─── Elinkaari ─────────────────────────────────────────────────────────

    @Override
    protected void onEnable() {
        try {
            ShaderEspRenderer.init();
        } catch (Throwable ignored) {
        }
        ShaderEspRenderer.setEnabled(true);
        applySettings();
    }

    @Override
    protected void onDisable() {
        ShaderEspRenderer.setEnabled(false);
    }

    @Override
    protected void onTick() {
        applySettings();
    }

    // ─── Asetusten sovellus ────────────────────────────────────────────────

    private void applySettings() {
        ShaderEspRenderer.setDownscale((int) downscale.getValue());

        ShaderEspRenderer.setRenderChams(enableChams.get());
        ShaderEspRenderer.setChamsTint((int) chamsTint.getValue());

        ShaderEspRenderer.setRenderOutline(enableOutline.get());
        ShaderEspRenderer.setOutlineColor((int) outlineColor.getValue());

        final double maxDistSq = renderDistance.getValue() * renderDistance.getValue();

        ShaderEspRenderer.setFilter(entity -> {
            if (entity == null) return false;

            TargetGroup g = TargetGroup.getGroup(entity);
            if (!HANDLED.contains(g)) return false;
            if (!shouldDraw(g)) return false;

            Vec3 cam = mc.gameRenderer.mainCamera().position();
            return entity.position().distanceToSqr(cam) <= maxDistSq;
        });
    }

    private boolean shouldDraw(TargetGroup g) {
        return switch (g) {
            case PLAYER  -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER   -> drawWater.get();
            case BOSS    -> drawBoss.get();
            default      -> false;
        };
    }
}