package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public class Slippy extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber slipperiness = new SettingNumber("Slipperiness", 0.6f, 0.98f, 0.01f, 0.98f);

    private static boolean moduleEnabled = false;
    private static float currentSlipperiness = 0.98f;

    public Slippy() {
        super("Slippy", "Slide on all blocks as if on ice", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addSetting(slipperiness);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        moduleEnabled = true;
        currentSlipperiness = (float) slipperiness.getValue();
    }

    @Override
    protected void onDisable() {
        moduleEnabled = false;
    }

    @Override
    protected void onTick() {
        if (moduleEnabled) {
            currentSlipperiness = (float) slipperiness.getValue();
        }
    }

    public static boolean isModuleEnabled() {
        return moduleEnabled;
    }

    public static float getCurrentSlipperiness() {
        return currentSlipperiness;
    }
}