package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public final class Step extends AxiomMod implements KeybindConfigurable {

    private final SettingSlider stepHeight;

    private static volatile boolean ENABLED = false;
    private static volatile double HEIGHT = 1.0;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Step() {
        super("Step", "Change the step height", ModuleCategory.MOVEMENT);

        this.stepHeight = new SettingSlider("Step Height", new double[]{
                1,2,3,4,5,6,7,8,9,10
        }, 4);
        addSetting(stepHeight);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    public static boolean isEnabledGlobal() {
        return ENABLED;
    }

    public static double getStepHeight() {
        return HEIGHT;
    }

    @Override
    protected void onEnable() {

        ENABLED = true;
        sync();
    }

    @Override
    protected void onDisable() {

        ENABLED = false;
        HEIGHT = 1.0;
    }

    @Override
    protected void onTick() {
        if (!ENABLED) return;
        sync();
    }

    private void sync() {
        HEIGHT = stepHeight.getValue();
        if (HEIGHT < 1.0) HEIGHT = 1.0;
        if (HEIGHT > 10.0) HEIGHT = 10.0;
    }
}
