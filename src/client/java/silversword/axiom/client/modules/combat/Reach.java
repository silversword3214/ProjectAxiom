package silversword.axiom.client.modules.combat;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public class Reach extends AxiomMod implements KeybindConfigurable {

    private final SettingSlider reachSlider;

    private static double ACTIVE_REACH = 3.0;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Reach() {
        super("Reach", "Extends your interaction and attacking distance", ModuleCategory.COMBAT);

        double[] presets = new double[31];
        for (int i = 0; i < presets.length; i++) {
            presets[i] = 3.0 + i * 0.1;
        }

        this.reachSlider = new SettingSlider("Reach Distance", presets, 3.0);
        addSetting(reachSlider);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    public double getReachDistance() {
        return reachSlider.getValue();
    }

    @Override
    protected void onEnable() {

        ACTIVE_REACH = getReachDistance();
    }

    @Override
    protected void onDisable() {
        ACTIVE_REACH = 3.0;
    }

    @Override
    public void onTick() {

        if (!isEnabled()) return;
        ACTIVE_REACH = getReachDistance();
    }
}
