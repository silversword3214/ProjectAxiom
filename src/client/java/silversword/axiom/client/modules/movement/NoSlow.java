package silversword.axiom.client.modules.movement;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public final class NoSlow extends AxiomMod implements KeybindConfigurable {
    public final SettingBoolean noItemUseSlow;
    public final SettingBoolean noSoulSandSlow;
    public final SettingBoolean noSlimeBlockSlow;
    public final SettingBoolean noBerryBushSlow;
    public final SettingBoolean noCobwebSlow;
    public final SettingBoolean noWaterSlow;
    public final SettingBoolean noLavaSlow;
    public final SettingBoolean noSneakingSlow;
    public final SettingBoolean noSlownessPotion;
    public final SettingBoolean noHoneyBlockSlow;

    public final SettingKeybind toggleKey;

    public NoSlow() {
        super("No Slow", "Prevents various slowing effects", ModuleCategory.MOVEMENT);

        noItemUseSlow     = new SettingBoolean("No Item Use Slow", true);
        noSoulSandSlow    = new SettingBoolean("No Soul Sand Slow", true);
        noSlimeBlockSlow  = new SettingBoolean("No Slime Block Slow", true);
        noBerryBushSlow   = new SettingBoolean("No Berry Bush Slow", true);
        noCobwebSlow      = new SettingBoolean("No Cobweb Slow", true);
        noWaterSlow       = new SettingBoolean("No Water Slow", false);
        noLavaSlow        = new SettingBoolean("No Lava Slow", false);
        noSneakingSlow    = new SettingBoolean("No Sneaking Slow", true);
        noSlownessPotion  = new SettingBoolean("No Slowness Potion", true);
        noHoneyBlockSlow  = new SettingBoolean("No Honey Block Slow", true);

        toggleKey = new SettingKeybind("Toggle Key", 0);

        addSetting(noItemUseSlow);
        addSetting(noSoulSandSlow);
        addSetting(noSlimeBlockSlow);
        addSetting(noHoneyBlockSlow);
        addSetting(noBerryBushSlow);
        addSetting(noCobwebSlow);
        addSetting(noWaterSlow);
        addSetting(noLavaSlow);
        addSetting(noSneakingSlow);
        addSetting(noSlownessPotion);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}
}