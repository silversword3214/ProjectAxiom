package silversword.axiom.client.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public class Aimbot extends AxiomMod implements KeybindConfigurable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Asetukset
    public final SettingMode targetMode = new SettingMode("Target", new String[]{"Players", "Mobs", "Both"}, "Both");
    public final SettingNumber range = new SettingNumber("Range", 1, 100, 1, 64);
    public final SettingMode bodyPart = new SettingMode("Body Part", new String[]{"Head", "Body", "Feet"}, "Body");
    public final SettingNumber smooth = new SettingNumber("Smooth", 1, 100, 1, 25);
    public final SettingBoolean hold = new SettingBoolean("Hold", true);
    public final SettingNumber manualOverrideThreshold = new SettingNumber("Manual Override", 0, 10, 0.5, 2.0);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0, true);
    public final SettingKeybind holdKey = new SettingKeybind("Hold Key", 0, false);

    private Entity target = null;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private boolean active = false;

    public Aimbot() {
        super("Aimbot", "Automatically aims at nearby entities", ModuleCategory.COMBAT);
        addSetting(targetMode);
        addSetting(range);
        addSetting(bodyPart);
        addSetting(smooth);
        addSetting(hold);
        addSetting(manualOverrideThreshold);
        addHiddenSetting(toggleKey);
        addHiddenSetting(holdKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        target = null;
        active = false;
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    protected void onDisable() {
        target = null;
        active = false;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.world == null) return;

        boolean shouldAim = false;
        if (hold.get()) {
            shouldAim = isEnabled();
        } else {
            int key = holdKey.get();
            if (key != 0) {
                long handle = mc.getWindow().getHandle();
                boolean pressed = org.lwjgl.glfw.GLFW.glfwGetKey(handle, key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                shouldAim = pressed && isEnabled();
            }
        }

        if (!shouldAim) {
            active = false;
            target = null;
            return;
        }

        // Tarkistetaan manuaalinen ohitus (hiiren liike)
        float yawDiff = Math.abs(mc.player.getYaw() - lastYaw);
        float pitchDiff = Math.abs(mc.player.getPitch() - lastPitch);
        float threshold = (float) manualOverrideThreshold.getValue(); // <-- KORJATTU
        if (yawDiff > threshold || pitchDiff > threshold) {
            active = false;
            target = null;
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            return;
        }

        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > range.getValue()) {
            target = findTarget();
            if (target == null) {
                active = false;
                return;
            }
        }

        Vec3d targetPos = getTargetPosition(target);
        Vec3d playerPos = mc.player.getEyePos();
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        float smoothFactor = (float) smooth.getValue() / 100f; // <-- KORJATTU
        if (smoothFactor > 0) {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            float newYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) * smoothFactor;
            float newPitch = currentPitch + (targetPitch - currentPitch) * smoothFactor;
            mc.player.setYaw(newYaw);
            mc.player.setPitch(newPitch);
        } else {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(targetPitch);
        }

        active = true;
        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
    }

    private Entity findTarget() {
        double maxDist = range.getValue();
        Entity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (!isValidTarget(entity)) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > maxDist) continue;

            Vec3d playerPos = mc.player.getEyePos();
            Vec3d targetPos = getTargetPosition(entity);
            Vec3d toTarget = targetPos.subtract(playerPos).normalize();
            Vec3d lookVec = mc.player.getRotationVec(1.0f);
            double angle = Math.acos(lookVec.dotProduct(toTarget)) * (180 / Math.PI);

            if (angle < bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity entity) {
        String mode = targetMode.getMode();
        if (mode.equals("Players")) return entity instanceof PlayerEntity;
        if (mode.equals("Mobs")) return entity instanceof LivingEntity && !(entity instanceof PlayerEntity);
        return entity instanceof LivingEntity;
    }

    private Vec3d getTargetPosition(Entity entity) {
        String part = bodyPart.getMode();
        double yOffset = 0;
        switch (part) {
            case "Head":
                yOffset = entity.getHeight() * 0.9;
                break;
            case "Body":
                yOffset = entity.getHeight() * 0.5;
                break;
            case "Feet":
                yOffset = 0;
                break;
        }
        return entity.getEntityPos().add(0, yOffset, 0);
    }
}