package silversword.axiom.client.modules.misc;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public class InvWalk extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingBoolean minecraftGui = new SettingBoolean("Minecraft Gui", true);
    public final SettingBoolean ownClickGui = new SettingBoolean("Click Gui", true);

    public InvWalk() {
        super("InvWalk", "Allows you to walk while GUI/Inv is open", ModuleCategory.MISC);

        addHiddenSetting(toggleKey);
        addSetting(minecraftGui);
        addSetting(ownClickGui);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {}
}