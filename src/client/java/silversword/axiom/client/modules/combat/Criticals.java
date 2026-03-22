package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;

public final class Criticals extends AxiomMod implements KeybindConfigurable {

    public Criticals() {
        super("Criticals", "Forces critical hits using packets.", ModuleCategory.COMBAT);
        addHiddenSetting(toggleKey);
    }

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        // Ei tarvita tick-logiikkaa.
    }

    /** Kutsutaan mixinistä juuri ennen attackEntityä. */
    public void tryDoPacketCrit(Entity target) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        if (!(target instanceof LivingEntity)) return;

        if (!mc.player.onGround()) return;

        if (mc.player.onClimbable()) return;
        if (mc.player.isInWater()) return;
        if (mc.player.isInLava()) return;
        if (mc.player.isPassenger()) return;
        if (mc.player.getAbilities().flying) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        sendFull(x, y + 0.0625D, z, yaw, pitch, false);
        sendFull(x, y,           z, yaw, pitch, false);
        sendFull(x, y + 1.0E-5D, z, yaw, pitch, false);
        sendFull(x, y,           z, yaw, pitch, false);
    }

    private static void sendFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        mc.getConnection().send(
                new ServerboundMovePlayerPacket.PosRot(
                        x, y, z,
                        yaw, pitch,
                        onGround,
                        mc.player.horizontalCollision
                )
        );
    }
}
