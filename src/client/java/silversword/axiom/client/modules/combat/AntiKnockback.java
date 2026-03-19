package silversword.axiom.client.modules.combat;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;

public final class AntiKnockback extends AxiomMod implements KeybindConfigurable {
    public final SettingSlider knockbackPercent;
    public final SettingKeybind toggleKey;

    public AntiKnockback() {
        super("AntiKnockback", "Reduces or removes knockback", ModuleCategory.COMBAT);

        knockbackPercent = new SettingSlider("Knockback %", new double[]{0, 25, 50, 75, 100}, 0);
        toggleKey = new SettingKeybind("Toggle Key", 0);

        addSetting(knockbackPercent);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}
}