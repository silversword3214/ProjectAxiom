package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public class IceSpeed extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber frictionBoost = new SettingNumber("Friction Boost", 0.98f, 1.08f, 0.01f, 0.99f);

    private static boolean moduleEnabled = false;
    private static float currentFriction = 0.99f;

    public IceSpeed() {
        super("Ice Speed", "Faster sliding on ice", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addSetting(frictionBoost);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        moduleEnabled = true;
        currentFriction = (float) frictionBoost.getValue();
    }

    @Override
    protected void onDisable() {
        moduleEnabled = false;
    }

    @Override
    protected void onTick() {
        if (moduleEnabled) {
            currentFriction = (float) frictionBoost.getValue();
        }
    }

    public static boolean isModuleEnabled() {
        return moduleEnabled;
    }

    public static float getCustomFriction() {
        return currentFriction;
    }
}