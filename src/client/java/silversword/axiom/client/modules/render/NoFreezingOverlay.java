package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public final class NoFreezingOverlay extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public NoFreezingOverlay() {
        super("No Freezing Overlay", "Removes the freezing / powder snow screen overlay.", ModuleCategory.RENDER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        // Mixin hoitaa kaiken
    }
}
