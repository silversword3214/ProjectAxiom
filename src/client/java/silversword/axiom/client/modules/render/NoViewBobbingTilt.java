package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public final class NoViewBobbingTilt extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public NoViewBobbingTilt() {
        super("NoBob", "Disables camera view bobbing tilt", ModuleCategory.RENDER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {

    }
}