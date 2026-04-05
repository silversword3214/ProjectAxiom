package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public final class Criticals extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Uudet asetukset
    private final SettingMode mode = new SettingMode("Mode", new String[]{"Packet", "MiniJump"}, "Packet");
    private final SettingNumber jumpHeight = new SettingNumber("Jump Height", 0.1, 2.0, 0.1, 0.5);

    public Criticals() {
        super("Criticals", "Forces critical hits using packets or mini-jump", ModuleCategory.COMBAT);
        addSetting(mode);
        addSetting(jumpHeight);
        addHiddenSetting(toggleKey);
    }

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

        // Vältetään turhat tilanteet
        if (mc.player.onClimbable() || mc.player.isInWater() || mc.player.isInLava()
                || mc.player.isPassenger() || mc.player.getAbilities().flying) {
            return;
        }

        String currentMode = mode.getMode();
        if (currentMode.equals("Packet")) {
            doPacketCrit(mc);
        } else if (currentMode.equals("MiniJump")) {
            doMiniJump(mc);
        }
    }

    /** Alkuperäinen packet-kriittinen (0.0625 nousu) */
    private void doPacketCrit(Minecraft mc) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        sendFull(x, y + 0.0625, z, yaw, pitch, false);
        sendFull(x, y,         z, yaw, pitch, false);
        sendFull(x, y + 1.0E-5, z, yaw, pitch, false);
        sendFull(x, y,         z, yaw, pitch, false);
    }

    /** Mini-hyppy: nousee halutun korkeuden ja laskeutuu heti */
    private void doMiniJump(Minecraft mc) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        double height = jumpHeight.getValue();

        // Nouse ylös
        sendFull(x, y + height, z, yaw, pitch, false);
        // Laske takaisin (palvelin tulkitsee pienenä putoamisena)
        sendFull(x, y, z, yaw, pitch, false);
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