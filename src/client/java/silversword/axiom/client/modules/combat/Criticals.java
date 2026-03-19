package silversword.axiom.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (!(target instanceof LivingEntity)) return;

        if (!mc.player.isOnGround()) return;

        if (mc.player.isClimbing()) return;
        if (mc.player.isTouchingWater()) return;
        if (mc.player.isInLava()) return;
        if (mc.player.hasVehicle()) return;
        if (mc.player.getAbilities().flying) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        sendFull(x, y + 0.0625D, z, yaw, pitch, false);
        sendFull(x, y,           z, yaw, pitch, false);
        sendFull(x, y + 1.0E-5D, z, yaw, pitch, false);
        sendFull(x, y,           z, yaw, pitch, false);
    }

    private static void sendFull(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.Full(
                        x, y, z,
                        yaw, pitch,
                        onGround,
                        mc.player.horizontalCollision
                )
        );
    }
}
