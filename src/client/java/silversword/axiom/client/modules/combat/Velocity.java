package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.packets.PacketReceiveEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomInitialize; // Tarvitaan EVENT_BUS:ia varten
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.*;

public final class Velocity extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingMode   mode;
    private final SettingNumber horizontal;
    private final SettingNumber vertical;

    public Velocity() {
        super("Velocity", "Reduces or cancels knockback", ModuleCategory.COMBAT);

        mode       = new SettingMode("Mode", new String[]{"Cancel", "Reduce"}, "Reduce");
        horizontal = new SettingNumber("Horizontal %", 0.0, 100.0, 1.0, 50.0);
        vertical   = new SettingNumber("Vertical %",   0.0, 100.0, 1.0, 50.0);

        addHiddenSetting(toggleKey);
        addSetting(mode);
        addSetting(horizontal);
        addSetting(vertical);
    }


    @Override
    protected void onEnable() {
    }


    @Override
    protected void onDisable() {
    }

    @Override
    protected void onTick() {

    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Subscribe
    private void onPacket(PacketReceiveEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null) return;
        if (!(event.getPacket() instanceof ClientboundSetEntityMotionPacket pkt)) return;
        if (pkt.getId() != mc.player.getId()) return;

        if (mode.getMode().equals("Cancel")) {
            event.setCancelled(true);
        } else if (mode.getMode().equals("Reduce")) {
            double h = horizontal.getValue() / 100.0;
            double v = vertical.getValue() / 100.0;

            Vec3 original = pkt.getMovement();
            Vec3 modified = new Vec3(
                    original.x * h,
                    original.y * v,
                    original.z * h
            );

            // Käytetään event.setPacket(), ei mc.player.setDeltaMovement()
            event.setPacket(new ClientboundSetEntityMotionPacket(pkt.getId(), modified));
        }
    }
}