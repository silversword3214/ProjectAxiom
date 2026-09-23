package silversword.axiom.client.modules.hidden;

import com.mojang.blaze3d.platform.InputConstants;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class Keybinds extends AxiomMod {
    public final SettingKeybind clickGuiKey = new SettingKeybind("ClickGUI Key", InputConstants.KEY_TAB);

    public Keybinds() {
        super("Keybinds", "Configure keybindings", ModuleCategory.HIDDEN);
        addSetting(clickGuiKey);
    }

    @Override
    protected void onTick() {
        // ei tarvita
    }
}