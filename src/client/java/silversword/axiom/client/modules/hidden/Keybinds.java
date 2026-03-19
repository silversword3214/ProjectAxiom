package silversword.axiom.client.modules.hidden;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import org.lwjgl.glfw.GLFW;

public class Keybinds extends AxiomMod {
    public final SettingKeybind clickGuiKey = new SettingKeybind("ClickGUI Key", GLFW.GLFW_KEY_TAB);

    public Keybinds() {
        super("Keybinds", "Configure keybindings", ModuleCategory.HIDDEN);
        addSetting(clickGuiKey);
    }

    @Override
    protected void onTick() {
        // ei tarvita
    }
}