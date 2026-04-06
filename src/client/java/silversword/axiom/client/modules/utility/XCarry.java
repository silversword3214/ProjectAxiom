package silversword.axiom.client.modules.utility;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public final class XCarry extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public XCarry() {
        super("XCarry", "Keep items in your crafting slots after closing inventory", ModuleCategory.UTILITY);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled()) return;

        if (event.getPacket() instanceof ServerboundContainerClosePacket closePacket) {
            if (closePacket.getContainerId() == 0) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void onTick() {

    }
}