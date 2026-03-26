package silversword.axiom.client.modules.render;

import net.minecraft.client.CameraType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import silversword.axiom.client.event.*;
import silversword.axiom.client.eventbus.EventPriority;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.FreecamCameraEntity;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.mixin.client.accessors.KeyMappingAccessor;

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
    private CameraType prevPerspective;
    private double fovScale;
    private boolean bobView;
    private boolean forward, backward, right, left, up, down, isSneaking;
    private long clickTs = 0;
    private FreecamCameraEntity cameraEntity;

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
        if (mc.level == null || mc.player == null) {
            toggle();
            return;
        }

        // Create and set the dummy camera
        cameraEntity = new FreecamCameraEntity(mc.level);
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        cameraEntity.setPos(camPos.x, camPos.y, camPos.z);
        cameraEntity.setYRot(yaw);
        cameraEntity.setXRot(pitch);
        Entity originalCamera = mc.getCameraEntity();
        mc.setCameraEntity(cameraEntity);

        fovScale = mc.options.fovEffectScale().get();
        bobView = mc.options.bobView().get();
        if (staticView.get()) {
            mc.options.fovEffectScale().set(0.0);
            mc.options.bobView().set(false);
        }

        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();

        prevPerspective = mc.options.getCameraType();
        if (!prevPerspective.isFirstPerson()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }


        pos.set(camPos.x, camPos.y, camPos.z);
        prevPos.set(camPos.x, camPos.y, camPos.z);

        if (prevPerspective == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        lastYaw = yaw;
        lastPitch = pitch;

        isSneaking = mc.options.keyShift.isDown();

        // Reset movement flags
        forward = backward = right = left = up = down = false;

        unpress();

        if (mc.level != null) {
            mc.level.addFreshEntity(cameraEntity);
        }

        if (reloadChunks.get()) mc.levelRenderer.allChanged();
    }

    @Override
    protected void onDisable() {

        if (cameraEntity != null) {
            mc.setCameraEntity(mc.player);
            // Remove the dummy entity from the level to avoid clutter
            if (cameraEntity.isAlive()) {
                cameraEntity.discard();
            }
            cameraEntity = null;
        }

        if (reloadChunks.get()) {
            mc.execute(() -> mc.levelRenderer.allChanged());
        }

        mc.options.setCameraType(prevPerspective);

        if (staticView.get()) {
            mc.options.fovEffectScale().set(fovScale);
            mc.options.bobView().set(bobView);
        }

        isSneaking = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null) return;

        if (cameraEntity != null) {
            cameraEntity.setPos(pos.x, pos.y, pos.z);
            cameraEntity.setYRot(yaw);
            cameraEntity.setXRot(pitch);
        }

        // Hardware key detection - bypasses game's key binding system
        long handle = mc.getWindow().handle();
        forward = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyUp).getKey().getValue());
        backward = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyDown).getKey().getValue());
        right = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyRight).getKey().getValue());
        left = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyLeft).getKey().getValue());
        up = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyJump).getKey().getValue());
        down = InputConstants.isKeyDown(mc.getWindow(), ((KeyMappingAccessor) mc.options.keyShift).getKey().getValue());

        // Unpress the actual keys so the player doesn't move
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);

        if (mc.getCameraEntity().isInWall()) mc.getCameraEntity().noPhysics = true;
        if (prevPerspective != null && !prevPerspective.isFirstPerson()) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }

        Vec3 forwardVec = Vec3.directionFromRotation(0, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90);
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


    @Subscribe(priority = EventPriority.LOW)
    private void onMouseScroll(MouseScrollEvent event) {
        if (!isEnabled()) return;
        if (speedScrollSensitivity.getValue() > 0 && mc.screen == null) {
            double newSpeed = speed.getValue() + event.value * 0.25 * speedScrollSensitivity.getValue() * speed.getValue();
            if (newSpeed < 0.1) newSpeed = 0.1;
            speed.setValue(newSpeed);
            event.setCancelled(true);
        }
    }

    @Subscribe
    private void onGameLeft(GameLeftEvent event) {
        if (toggleOnLog.get() && isEnabled()) toggle();
    }

    @Subscribe
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled()) return;
        if (event.packet instanceof ClientboundPlayerCombatKillPacket pkt) {
            Entity e = mc.level.getEntity(pkt.playerId());
            if (e == mc.player && toggleOnDeath.get()) {
                toggle();
                System.out.println("Toggled off because you died.");
            }
        } else if (event.packet instanceof ClientboundSetHealthPacket pkt) {
            if (mc.player.getHealth() - pkt.getHealth() > 0 && toggleOnDamage.get()) {
                toggle();
                System.out.println("Toggled off because you took damage.");
            }
        } else if (event.packet instanceof ClientboundRespawnPacket) {
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
        pitch = Mth.clamp(pitch, -90, 90);
    }

    // For CameraMixin
    public double getX(float tickDelta) { return Mth.lerp(tickDelta, prevPos.x, pos.x); }
    public double getY(float tickDelta) { return Mth.lerp(tickDelta, prevPos.y, pos.y); }
    public double getZ(float tickDelta) { return Mth.lerp(tickDelta, prevPos.z, pos.z); }
    public double getYaw(float tickDelta) { return Mth.lerp(tickDelta, lastYaw, yaw); }
    public double getPitch(float tickDelta) { return Mth.lerp(tickDelta, lastPitch, pitch); }

    public boolean renderHands() { return !isEnabled() || renderHands.get(); }
    public boolean staySneaking() { return isEnabled() && staySneaking.get() && isSneaking; }

    private void unpress() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
    }
}