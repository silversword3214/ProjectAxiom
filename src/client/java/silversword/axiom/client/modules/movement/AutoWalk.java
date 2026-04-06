package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public final class AutoWalk extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AutoWalk() {
        super("Auto Walk", "Automatically walks forward", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        // Jos ollaan jossain valikossa (esim. chat tai inventory), ei pakoteta kävelyä
        if (mc.screen != null) {
            return;
        }

        // Pakotetaan "eteenpäin" näppäin pohjaan
        mc.options.keyUp.setDown(true);
    }

    @Override
    protected void onDisable() {
        // TÄRKEÄ: Kun moduuli sammutetaan, vapautetaan näppäin heti.
        // Muuten hahmo jatkaa juoksemista ikuisesti.
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.keyUp != null) {
            // Tarkistetaan onko näppäin OIKEASTI pohjassa (fyysisesti)
            // Jos ei ole, asetetaan se falseksi.
            mc.options.keyUp.setDown(false);
        }
    }
}