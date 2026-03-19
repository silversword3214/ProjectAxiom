package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

/**
 * Disables camera view-bobbing transform (the "tilt/rock" in world render).
 * This does NOT touch your tracer math at all -> it removes the source of wobble.
 */
public final class NoViewBobbingTilt extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);


    // Optional: allow also disabling hurt tilt if you want later
    public final SettingBoolean disableHurtTilt = new SettingBoolean("Disable Hurt Tilt", false);

    public NoViewBobbingTilt() {
        super("No ViewBobbing Tilt", "Disables camera view bobbing transform (tilt).", ModuleCategory.RENDER);
        addSetting(disableHurtTilt);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        // nothing
    }
}