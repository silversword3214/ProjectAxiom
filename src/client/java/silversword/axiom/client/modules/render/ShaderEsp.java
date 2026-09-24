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

    private final SettingSlider  renderDistance;
    private final SettingSlider  downscale;
    private final SettingSlider  outlineColor;
    private final SettingBoolean drawPlayers, drawHostile, drawPassive,
            drawNeutral, drawWater, drawBoss;

    private static final Set<TargetGroup> HANDLED = EnumSet.of(
            TargetGroup.PLAYER, TargetGroup.HOSTILE, TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL, TargetGroup.WATER, TargetGroup.BOSS);

    public ShaderEsp() {
        super("ShaderEsp", "Outline entities with edge-detection shader",
                ModuleCategory.RENDER);

        renderDistance = new SettingSlider("Render Distance",
                new double[]{16, 32, 64, 96, 128, 256, 512}, 128);
        downscale = new SettingSlider("Mask Resolution",
                new double[]{1, 2, 3, 4}, 2);
        outlineColor = new SettingSlider("Outline Color",
                new double[]{0xFFFFFFFFL, 0xFF00FFFFL, 0xFFFF00FFL,
                        0xFFFF0000L, 0xFF00FF00L, 0xFFFFFF00L}, 0);

        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", false);
        drawNeutral = new SettingBoolean("Draw Neutral", false);
        drawWater   = new SettingBoolean("Draw Water",   false);
        drawBoss    = new SettingBoolean("Draw Boss",    true);

        addSetting(renderDistance);
        addSetting(downscale);
        addSetting(outlineColor);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
    }

    @Override
    protected void onEnable() {
        try {
            ShaderEspRenderer.init();
        } catch (Throwable t) {
            // Moduuli voi yrittää initin uudelleen myöhemmin — älä kaada tähän
            org.slf4j.LoggerFactory.getLogger("Axiom/ShaderESP")
                    .warn("ShaderEsp init deferred: {}", t.toString());
        }
        ShaderEspRenderer.setEnabled(true);   // ← TÄMÄ AJETAAN AINA
        applySettings();
    }

    @Override
    protected void onDisable() {
        ShaderEspRenderer.setEnabled(false);
    }

    @Override
    protected void onTick() { applySettings(); }

    private void applySettings() {
        ShaderEspRenderer.setDownscale((int) downscale.getValue());
        ShaderEspRenderer.setOutlineColor((int) outlineColor.getValue());

        final double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        ShaderEspRenderer.setFilter(entity -> {
            TargetGroup g = TargetGroup.getGroup(entity);
            if (!HANDLED.contains(g)) return false;
            if (!shouldDraw(g)) return false;
            Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
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