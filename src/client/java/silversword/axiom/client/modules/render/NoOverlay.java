package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public final class NoOverlay extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingBoolean noFire = new SettingBoolean("No Fire Overlay", true);
    public final SettingBoolean noFreezing = new SettingBoolean("No Freezing Overlay", true);
    public final SettingBoolean noPumpkin = new SettingBoolean("No Pumpkin Overlay", true);
    public final SettingBoolean noTotem = new SettingBoolean("No Totem Overlay", true);

    public NoOverlay() {
        super("No Overlay", "Removes various screen overlays.", ModuleCategory.RENDER);
        addHiddenSetting(toggleKey);
        addSetting(noFire);
        addSetting(noFreezing);
        addSetting(noPumpkin);
        addSetting(noTotem);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        // All logic is in mixins
    }
}