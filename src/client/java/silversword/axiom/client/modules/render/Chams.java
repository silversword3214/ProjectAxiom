package silversword.axiom.client.modules.render;

import net.minecraft.entity.Entity;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Chams extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public enum RenderMode {
        Solid,
        Textured
    }

    private final SettingMode renderMode = new SettingMode(
            "Render Mode",
            new String[]{"Solid", "Textured"},
            "Textured" // oletus teksturoitu
    );

    private final SettingColor color = new SettingColor("Color", new Color(255, 255, 255, 255)); // valkoinen
    private final SettingSlider alpha = new SettingSlider(
            "Alpha",
            new double[]{0, 25, 50, 75, 100, 125, 150, 175, 200, 225, 255},
            180
    );

    private final SettingBoolean throughWalls = new SettingBoolean("Through Walls", true);

    private final SettingBoolean drawPlayers = new SettingBoolean("Draw Players", true);
    private final SettingBoolean drawHostile = new SettingBoolean("Draw Hostile", true);
    private final SettingBoolean drawPassive = new SettingBoolean("Draw Passive", true);
    private final SettingBoolean drawNeutral = new SettingBoolean("Draw Neutral", true);
    private final SettingBoolean drawWater = new SettingBoolean("Draw Water", true);
    private final SettingBoolean drawBoss = new SettingBoolean("Draw Boss", true);

    private final SettingSlider renderDistance = new SettingSlider(
            "Render Distance",
            new double[]{16, 32, 64, 96, 128, 256, 512},
            96
    );

    public Chams() {
        super("Chams", "See entities through walls (use ShaderESP instead of this)", ModuleCategory.RENDER);
        addSetting(renderMode);
        addHiddenSetting(color.getSetting());
        addSetting(alpha);
        addSetting(throughWalls);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
        addSetting(renderDistance);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}

    public boolean shouldDraw(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == mc.player) return false;
        if (entity.distanceTo(mc.player) > renderDistance.getValue()) return false;

        TargetGroup group = TargetGroup.getGroup(entity);
        return switch (group) {
            case PLAYER  -> drawPlayers.get();
            case HOSTILE -> drawHostile.get();
            case PASSIVE -> drawPassive.get();
            case NEUTRAL -> drawNeutral.get();
            case WATER   -> drawWater.get();
            case BOSS    -> drawBoss.get();
            default      -> false;
        };
    }

    // UUSI METODI: palauttaa ARGB-pakatun värin (alpha huomioitu)
    public int getPackedColor() {
        Color col = color.getCurrentColor().copy();
        col.a((int) alpha.getValue()); // asetetaan alpha sliderista
        return col.getPacked();         // palauttaa int muodossa 0xAARRGGBB
    }

    public int getRenderModeIndex() {
        return (int) renderMode.getValue();
    }

    public boolean isThroughWalls() {
        return throughWalls.get();
    }
}