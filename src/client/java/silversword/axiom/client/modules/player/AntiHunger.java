package silversword.axiom.client.modules.player;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public final class AntiHunger extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public AntiHunger() {
        super("Anti Hunger", "Prevents hunger loss by canceling sprint packets", ModuleCategory.PLAYER);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled()) return;

        if (event.getPacket() instanceof ServerboundPlayerCommandPacket command) {
            if (command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
                event.setCancelled(true);
            }

        }
    }

    @Override
    protected void onTick() {
    }
}