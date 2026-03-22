package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public final class Jesus extends AxiomMod implements KeybindConfigurable {

    // Kun sneakkaat, uppoat normaalisti (käytännöllinen)
    public final SettingBoolean sneakToSink = new SettingBoolean("Sneak To Sink", true);

    // Halutessasi myöhemmin lava
    public final SettingBoolean includeLava = new SettingBoolean("Include Lava", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Jesus() {
        super("Jesus", "Walk on water", ModuleCategory.MOVEMENT);
        addSetting(sneakToSink);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!includeLava.get()) return;

        // Jos seisot laavan pinnalla, poista lava- ja fire-tila
        if (mc.player.onGround() && mc.player.isInLava()) {
            mc.player.setRemainingFireTicks(0);
            mc.player.clearFire();
        }
    }

}
