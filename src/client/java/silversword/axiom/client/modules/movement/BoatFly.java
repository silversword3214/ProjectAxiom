package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public final class BoatFly extends AxiomMod {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingNumber speed = new SettingNumber("Speed", 0.1, 10, 0.1, 2);


    public BoatFly() {
        super("Boat Fly", "Allows you to fly in a boat.", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addSetting(speed);
    }

    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        // All logic is in the mixin
    }
}