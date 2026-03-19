package silversword.axiom.client.modules.render;

import org.lwjgl.glfw.GLFW;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class NoVignette extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey;

    public NoVignette() {
        super("No Vignette", "Removes the dark vignette effect from the screen", ModuleCategory.RENDER);
        toggleKey = new SettingKeybind("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);
        addHiddenSetting(toggleKey);
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }
}