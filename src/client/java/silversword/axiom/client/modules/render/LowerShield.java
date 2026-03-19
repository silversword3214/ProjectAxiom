package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public final class LowerShield extends AxiomMod implements KeybindConfigurable {

    // True = laskee kilpeä aina (myös idle). False = vain blockatessa.
    private final SettingBoolean alwaysLower =
            new SettingBoolean("Always Lower", true);

    // Idle offset (kun ei blockata). Step 0.01, default 0.25
    private final SettingNumber idleOffset =
            new SettingNumber("Idle Offset", 0.0, 1.0, 0.01, 0.25);

    // Blocking offset (kun blockataan). Step 0.01, default 0.40
    private final SettingNumber blockingOffset =
            new SettingNumber("Blocking Offset", 0.0, 1.0, 0.01, 0.40);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public LowerShield() {
        super("Lower Shield", "Lowers the shield.", ModuleCategory.RENDER);

        addSetting(alwaysLower);
        addSetting(idleOffset);
        addSetting(blockingOffset);

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

    public boolean isAlwaysLower() {
        return alwaysLower.get();
    }

    public float getIdleOffset() {
        return (float) idleOffset.getValue();
    }

    public float getBlockingOffset() {
        return (float) blockingOffset.getValue();
    }

    /**
     * Mixin kutsuu tätä.
     * - blockatessa: blockingOffset
     * - muuten: idleOffset jos Always Lower, muuten 0
     */
    public float getOffsetY(boolean isBlocking) {
        if (isBlocking) return getBlockingOffset();
        return isAlwaysLower() ? getIdleOffset() : 0.0f;
    }
}
