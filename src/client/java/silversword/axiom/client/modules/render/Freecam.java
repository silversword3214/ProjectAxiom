package silversword.axiom.client.modules.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import silversword.axiom.client.event.*;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.eventbus.Priority;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.mixin.client.accessors.KeyBindingAccessor;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Freecam extends AxiomMod implements KeybindConfigurable {
    // Settings
    private final SettingNumber speed = new SettingNumber("Speed", 0.1, 10.0, 0.1, 1.0);
    private final SettingNumber speedScrollSensitivity = new SettingNumber("Speed Scroll Sensitivity", 0.0, 2.0, 0.1, 0.0);
    private final SettingBoolean staySneaking = new SettingBoolean("Stay Sneaking", true);
    private final SettingBoolean toggleOnDamage = new SettingBoolean("Toggle on Damage", false);
    private final SettingBoolean toggleOnDeath = new SettingBoolean("Toggle on Death", false);
    private final SettingBoolean toggleOnLog = new SettingBoolean("Toggle on Logout", true);
    private final SettingBoolean reloadChunks = new SettingBoolean("Reload Chunks", true);
    private final SettingBoolean renderHands = new SettingBoolean("Show Hands", true);
    private final SettingBoolean rotate = new SettingBoolean("Rotate", false);
    private final SettingBoolean staticView = new SettingBoolean("Static View", true);
    private final SettingBoolean baritoneClick = new SettingBoolean("Click to Path", false);
    private final SettingBoolean requireDoubleClick = new SettingBoolean("Double Click", false);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // State
    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();
    public float yaw, pitch;
    public float lastYaw, lastPitch;
    private Perspective prevPerspective;
    private double fovScale;
    private boolean bobView;
    private boolean forward, backward, right, left, up, down, isSneaking;
    private long clickTs = 0;

    public Freecam() {
        super("Freecam", "Move camera freely without moving server-side", ModuleCategory.RENDER);
        addSetting(speed);
        addSetting(speedScrollSensitivity);
        addSetting(staySneaking);
        addSetting(toggleOnDamage);
        addSetting(toggleOnDeath);
        addSetting(toggleOnLog);
        addSetting(reloadChunks);
        addSetting(renderHands);
        addSetting(rotate);
        addSetting(staticView);
        addSetting(baritoneClick);
        addSetting(requireDoubleClick);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (mc.world == null || mc.player == null) {
            toggle();
            return;
        }

        fovScale = mc.options.getFovEffectScale().getValue();
        bobView = mc.options.getBobView().getValue();
        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue(0.0);
            mc.options.getBobView().setValue(false);
        }

        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();

        prevPerspective = mc.options.getPerspective();
        if (!prevPerspective.isFirstPerson()) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        pos.set(camPos.x, camPos.y, camPos.z);
        prevPos.set(camPos.x, camPos.y, camPos.z);

        if (prevPerspective == Perspective.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.sneakKey.isPressed();

        // Reset movement flags
        forward = backward = right = left = up = down = false;

        unpress();

        if (reloadChunks.get()) mc.worldRenderer.reload();
    }

    @Override
    protected void onDisable() {
        if (reloadChunks.get()) {
            mc.execute(() -> mc.worldRenderer.reload());
        }

        mc.options.setPerspective(prevPerspective);

        if (staticView.get()) {
            mc.options.getFovEffectScale().setValue(fovScale);
            mc.options.getBobView().setValue(bobView);
        }

        isSneaking = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) return;

        // Hardware key detection – bypasses game's key binding system
        long handle = mc.getWindow().getHandle();
        forward = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.forwardKey).getBoundKey().getCode());
        backward = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.backKey).getBoundKey().getCode());
        right = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.rightKey).getBoundKey().getCode());
        left = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.leftKey).getBoundKey().getCode());
        up = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.jumpKey).getBoundKey().getCode());
        down = InputUtil.isKeyPressed(mc.getWindow(), ((KeyBindingAccessor) mc.options.sneakKey).getBoundKey().getCode());

        // Unpress the actual keys so the player doesn't move
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);

        if (mc.getCameraEntity().isInsideWall()) mc.getCameraEntity().noClip = true;
        if (prevPerspective != null && !prevPerspective.isFirstPerson()) {
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }

        Vec3d forwardVec = Vec3d.fromPolar(0, yaw);
        Vec3d rightVec = Vec3d.fromPolar(0, yaw + 90);
        double spd = speed.getValue() * 0.5; // base speed factor

        double velX = 0, velY = 0, velZ = 0;

        if (forward) {
            velX += forwardVec.x * spd;
            velZ += forwardVec.z * spd;
        }
        if (backward) {
            velX -= forwardVec.x * spd;
            velZ -= forwardVec.z * spd;
        }
        if (right) {
            velX += rightVec.x * spd;
            velZ += rightVec.z * spd;
        }
        if (left) {
            velX -= rightVec.x * spd;
            velZ -= rightVec.z * spd;
        }

        if ((forward || backward) && (right || left)) {
            double diag = 1 / Math.sqrt(2);
            velX *= diag;
            velZ *= diag;
        }

        if (up) velY += spd;
        if (down) velY -= spd;

        prevPos.set(pos);
        pos.add(velX, velY, velZ);
    }

    // Optional: keep mouse scroll for speed adjustment (still works via mixin)
    @AxiomEvent(priority = Priority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (!isEnabled()) return;
        if (speedScrollSensitivity.getValue() > 0 && mc.currentScreen == null) {
            double newSpeed = speed.getValue() + event.value * 0.25 * speedScrollSensitivity.getValue() * speed.getValue();
            if (newSpeed < 0.1) newSpeed = 0.1;
            speed.setValue(newSpeed);
            event.cancel();
        }
    }

    // The following event handlers are no longer needed for movement
    // but can be kept for other features (like toggling on damage, etc.)
    @AxiomEvent
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get() && isEnabled()) toggle();
    }

    @AxiomEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled()) return;
        if (event.packet instanceof DeathMessageS2CPacket pkt) {
            Entity e = mc.world.getEntityById(pkt.playerId());
            if (e == mc.player && toggleOnDeath.get()) {
                toggle();
                System.out.println("Toggled off because you died.");
            }
        } else if (event.packet instanceof HealthUpdateS2CPacket pkt) {
            if (mc.player.getHealth() - pkt.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                System.out.println("Toggled off because you took damage.");
            }
        } else if (event.packet instanceof PlayerRespawnS2CPacket) {
            if (isEnabled()) {
                toggle();
                System.out.println("Toggled off because you changed dimensions.");
            }
        }
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaX;
        pitch += (float) deltaY;
        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    // For CameraMixin
    public double getX(float tickDelta) { return MathHelper.lerp(tickDelta, prevPos.x, pos.x); }
    public double getY(float tickDelta) { return MathHelper.lerp(tickDelta, prevPos.y, pos.y); }
    public double getZ(float tickDelta) { return MathHelper.lerp(tickDelta, prevPos.z, pos.z); }
    public double getYaw(float tickDelta) { return MathHelper.lerp(tickDelta, lastYaw, yaw); }
    public double getPitch(float tickDelta) { return MathHelper.lerp(tickDelta, lastPitch, pitch); }

    public boolean renderHands() { return !isEnabled() || renderHands.get(); }
    public boolean staySneaking() { return isEnabled() && staySneaking.get() && isSneaking; }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }
}