package silversword.axiom.client.modules.misc;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public final class PingSpoof extends AxiomMod implements KeybindConfigurable {

    public static PingSpoof INSTANCE;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber fakePing;

    public PingSpoof() {
        super("PingSpoof", "Spoofs your displayed ping", ModuleCategory.MISC);
        INSTANCE = this;

        fakePing = new SettingNumber("Ping", 1, 999, 1, 69);

        addHiddenSetting(toggleKey);
        addSetting(fakePing);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onTick() {}

    public int getFakePing() {
        return (int) fakePing.getValue();
    }
}