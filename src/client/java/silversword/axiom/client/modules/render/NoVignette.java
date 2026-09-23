package silversword.axiom.client.modules.render;

import com.mojang.blaze3d.platform.InputConstants;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public class NoVignette extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey;

    public NoVignette() {
        super("No Vignette", "Removes the dark vignette effect from the screen", ModuleCategory.RENDER);
        // 26.3: GLFW_KEY_UNKNOWN → InputConstants.UNKNOWN.getValue() (-1)
        toggleKey = new SettingKeybind("Toggle Key", InputConstants.UNKNOWN.getValue());
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