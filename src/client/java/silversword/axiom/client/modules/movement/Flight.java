package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.packets.PacketEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.mixin.client.accessors.ServerboundMovePlayerPacketAccessor;

import java.util.Random;

public class Flight extends AxiomMod implements KeybindConfigurable {

    public static Flight INSTANCE;

    // Moodit
    private final SettingMode flyMode = new SettingMode("Mode", new String[]{"Vanilla", "Velocity", "Packet"}, "Packet");
    private final SettingSlider speed = new SettingSlider("Speed", new double[]{0.5, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0}, 1.0);
    private final SettingSlider vSpeed = new SettingSlider("Vertical Speed", new double[]{0.4, 0.6, 0.8, 1.0, 1.2}, 0.7);
    private final SettingSlider hoverOffset = new SettingSlider("Hover Offset", new double[]{1e-10, 3e-10, 5e-10, 1e-9}, 5e-10);

    // Anti‑kick asetukset – nyt SettingMode (ei enää enum)
    private final SettingMode antiKickMode = new SettingMode("Anti-Kick Mode", new String[]{"Normal", "Packet", "None"}, "Packet");
    private final SettingSlider antiKickInterval = new SettingSlider("Anti‑Kick Interval", new double[]{20, 40, 60, 80, 100, 120}, 60);
    private final SettingSlider antiKickAmount = new SettingSlider("Anti‑Kick Amount", new double[]{0.03, 0.04, 0.05, 0.06, 0.07, 0.08}, 0.05);

    // Packet‑moodin laskurit
    private int packetCounter = 0;
    private int antiKickTimer = 0;
    private Random random = new Random();
    private Double oldFovEffectScale = null;

    // Anti‑kick (Packet) -laskurit
    private int delayLeft = 20;
    private int offLeft = 1;
    private double lastPacketY = Double.MAX_VALUE;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public Flight() {
        super("Flight", "Vanilla, Packet and Velocity modes", ModuleCategory.MOVEMENT);
        INSTANCE = this;

        addSetting(flyMode);
        addSetting(speed);
        addSetting(vSpeed);
        addSetting(hoverOffset);
        addSetting(antiKickMode);
        addSetting(antiKickInterval);
        addSetting(antiKickAmount);

        addHiddenSetting(toggleKey);

        // Rekisteröidään event-kuuntelija
        AxiomInitialize.EVENT_BUS.register(this);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    // Tämä metodi kutsutaan aina, kun paketti lähetetään (PacketEvent.Send)
    public void onSendPacket(PacketEvent.Send event) {
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundMovePlayerPacket movePacket)) return;
        // Vain Packet-moodissa tehdään manipulaatio
        if (!antiKickMode.getMode().equals("Packet")) return;

        double currentY = getYFromPacket(movePacket);
        if (currentY != Double.MAX_VALUE) {
            antiKickPacket(movePacket, currentY);
        }
    }

    private double getYFromPacket(ServerboundMovePlayerPacket packet) {
        if (packet instanceof ServerboundMovePlayerPacket.Pos p) {
            return p.getY(lastPacketY);
        } else if (packet instanceof ServerboundMovePlayerPacket.PosRot p) {
            return p.getY(lastPacketY);
        }
        return Double.MAX_VALUE;
    }

    private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (delayLeft <= 0 && lastPacketY != Double.MAX_VALUE &&
                shouldFlyDown(currentY, lastPacketY) && !mc.player.onGround()) {

            ((ServerboundMovePlayerPacketAccessor) packet).axiom$setY(lastPacketY - 0.03130);
        } else {
            lastPacketY = currentY;
        }
    }

    private boolean shouldFlyDown(double currentY, double lastY) {
        if (currentY >= lastY) return true;
        return (lastY - currentY) < 0.03130;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        // ---- Packet-moodin laskurien päivitys (anti‑kick) ----
        if (antiKickMode.getMode().equals("Packet")) {
            if (delayLeft > 0) delayLeft--;
            if (offLeft <= 0 && delayLeft <= 0) {
                delayLeft = (int) antiKickInterval.getValue();
                offLeft = 1;
            } else if (delayLeft <= 0) {
                offLeft--;
            }
        }

        String mode = flyMode.getMode();

        mc.player.setNoGravity(true);
        mc.player.fallDistance = 0.0f;

        if (mode.equals("Vanilla")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed((float) (speed.getValue() * 0.05));
            mc.player.getAbilities().setWalkingSpeed((float) (speed.getValue() * 0.1));
            mc.player.onUpdateAbilities();
            return;
        }

        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().mayfly = false;

        Vec2 movementInput = mc.player.input.getMoveVector();
        double forward = movementInput.y;
        double strafe = movementInput.x;
        float yaw = mc.player.getYRot();

        double speedVal = speed.getValue();
        double vSpeedVal = vSpeed.getValue();

        double motionX = 0, motionY = 0, motionZ = 0;

        if (forward != 0 || strafe != 0) {
            double rad = Math.toRadians(yaw);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);
            motionX = -sin * forward * speedVal + cos * strafe * speedVal;
            motionZ = cos * forward * speedVal + sin * strafe * speedVal;
        }

        if (mc.options.keyJump.isDown()) motionY += vSpeedVal;
        if (mc.options.keyShift.isDown()) motionY -= vSpeedVal;

        // Vanha anti-kick (vain jos ei Packet-moodi)
        if (!antiKickMode.getMode().equals("Packet")) {
            antiKickTimer--;
            if (antiKickTimer <= 0) {
                antiKickTimer = (int) antiKickInterval.getValue();
                if (motionY >= 0) motionY -= antiKickAmount.getValue();
                else motionY -= antiKickAmount.getValue() * 0.5;
            }
        }

        if (mode.equals("Velocity")) {
            mc.player.setDeltaMovement(motionX, motionY, motionZ);
        } else if (mode.equals("Packet")) {
            mc.player.setDeltaMovement(motionX, motionY, motionZ);

            Vec3 pos = mc.player.position();
            double newX = pos.x + motionX;
            double newY = pos.y + motionY;
            double newZ = pos.z + motionZ;

            double randX = (random.nextDouble() - 0.5) * 1e-4;
            double randY = (random.nextDouble() - 0.5) * 1e-4;
            double randZ = (random.nextDouble() - 0.5) * 1e-4;

            int packetsPerTick = 2 + (int) (speedVal * 1);
            for (int i = 0; i < packetsPerTick; i++) {
                double yOffset = (packetCounter + i) % 2 == 0 ? hoverOffset.getValue() : -hoverOffset.getValue();
                sendPos(newX + randX * (i + 1), newY + yOffset + randY * (i + 1), newZ + randZ * (i + 1), false);
            }

            boolean groundSpoof = true;
            sendPos(newX, newY, newZ, groundSpoof);
            packetCounter++;
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        Minecraft mc = Minecraft.getInstance();
        mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, mc.player.horizontalCollision)
        );
    }

    public boolean isActive() {
        return this.isEnabled();
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            oldFovEffectScale = mc.options.fovEffectScale().get();
            mc.options.fovEffectScale().set(0.0);
        }
        packetCounter = 0;
        antiKickTimer = (int) antiKickInterval.getValue();
        delayLeft = (int) antiKickInterval.getValue();
        offLeft = 1;
        lastPacketY = Double.MAX_VALUE;

    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && oldFovEffectScale != null) {
            mc.options.fovEffectScale().set(oldFovEffectScale);
            oldFovEffectScale = null;
        }
        if (mc != null && mc.player != null) {
            mc.player.setNoGravity(false);
            mc.player.fallDistance = 0.0f;
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().setFlyingSpeed(0.05f);
            mc.player.getAbilities().setWalkingSpeed(0.1f);
            mc.player.onUpdateAbilities();
        }

        packetCounter = 0;
    }
}