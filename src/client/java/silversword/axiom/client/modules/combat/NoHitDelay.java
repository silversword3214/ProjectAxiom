package silversword.axiom.client.modules.combat;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;

public class NoHitDelay extends AxiomMod implements KeybindConfigurable {

    public static boolean enabled = false; // Mixin tarkistaa tämän

    private final SettingBoolean onlyPlayers = new SettingBoolean("Only Players", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public NoHitDelay() {
        super("No Hit Delay", "Removes the delay between attacks. Tip: Use with KillAura", ModuleCategory.COMBAT);
        addSetting(onlyPlayers);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        enabled = true;
    }

    @Override
    protected void onDisable() {
        enabled = false;
    }

    @Override
    protected void onTick() {
        // Ei tarvetta tick-logiikalle, mixin hoitaa
    }

    public static boolean shouldDisableForPlayersOnly() {
        // Tätä voidaan käyttää mixinissä jos halutaan tarkistaa onlyPlayers-asetus
        // (mutta se on monimutkaisempaa, koska asetus ei ole staattinen)
        return false; // Yksinkertaistetaan nyt
    }
}