package silversword.axiom.client.modules.world;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public final class TimeChanger extends AxiomMod implements KeybindConfigurable {
    public final SettingMode timeMode;
    public final SettingNumber customTime;
    public final SettingKeybind toggleKey;

    public TimeChanger() {
        super("Time Changer", "Changes the world time client-side", ModuleCategory.WORLD);

        timeMode = new SettingMode("Time Mode", new String[]{"Day", "Night", "Custom"}, "Day");
        customTime = new SettingNumber("Custom Time", 0, 24000, 100, 6000);
        toggleKey = new SettingKeybind("Toggle Key", 0);

        addSetting(timeMode);
        addSetting(customTime);
        addHiddenSetting(toggleKey);
    }

    public long getForcedTime() {
        switch (timeMode.getMode()) {
            case "Day":
                return 6000; // keskipäivä
            case "Night":
                return 18000; // keskiyö
            case "Custom":
                return (long) customTime.getValue();
            default:
                return -1;
        }
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}
}