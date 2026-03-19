package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public class AntiBlind extends AxiomMod implements KeybindConfigurable {

    public final SettingBoolean noBlindness = new SettingBoolean("No Blindness", false);
    public final SettingBoolean noDarkness = new SettingBoolean("No Darkness", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AntiBlind() {
        super("AntiBlind", "Removes blindness and darkness effects", ModuleCategory.RENDER);
        addSetting(noBlindness);
        addSetting(noDarkness);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void onTick() {
        // ei tarvita
    }
}