package silversword.axiom.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

public class Aimbot extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

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
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }
    }

    @Override
    protected void onDisable() {
        target = null;
        active = false;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        boolean shouldAim = false;
        if (hold.get()) {
            shouldAim = isEnabled();
        } else {
            int key = holdKey.get();
            if (key != 0) {
                long handle = mc.getWindow().handle();
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
        float yawDiff = Math.abs(mc.player.getYRot() - lastYaw);
        float pitchDiff = Math.abs(mc.player.getXRot() - lastPitch);
        float threshold = (float) manualOverrideThreshold.getValue(); // <-- KORJATTU
        if (yawDiff > threshold || pitchDiff > threshold) {
            active = false;
            target = null;
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
            return;
        }

        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > range.getValue()) {
            target = findTarget();
            if (target == null) {
                active = false;
                return;
            }
        }

        Vec3 targetPos = getTargetPosition(target);
        Vec3 playerPos = mc.player.getEyePosition();
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

        float smoothFactor = (float) smooth.getValue() / 100f; // <-- KORJATTU
        if (smoothFactor > 0) {
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            float newYaw = currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * smoothFactor;
            float newPitch = currentPitch + (targetPitch - currentPitch) * smoothFactor;
            mc.player.setYRot(newYaw);
            mc.player.setXRot(newPitch);
        } else {
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
        }

        active = true;
        lastYaw = mc.player.getYRot();
        lastPitch = mc.player.getXRot();
    }

    private Entity findTarget() {
        double maxDist = range.getValue();
        Entity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (!isValidTarget(entity)) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > maxDist) continue;

            Vec3 playerPos = mc.player.getEyePosition();
            Vec3 targetPos = getTargetPosition(entity);
            Vec3 toTarget = targetPos.subtract(playerPos).normalize();
            Vec3 lookVec = mc.player.getViewVector(1.0f);
            double angle = Math.acos(lookVec.dot(toTarget)) * (180 / Math.PI);

            if (angle < bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity entity) {
        String mode = targetMode.getMode();
        if (mode.equals("Players")) return entity instanceof Player;
        if (mode.equals("Mobs")) return entity instanceof LivingEntity && !(entity instanceof Player);
        return entity instanceof LivingEntity;
    }

    private Vec3 getTargetPosition(Entity entity) {
        String part = bodyPart.getMode();
        double yOffset = 0;
        switch (part) {
            case "Head":
                yOffset = entity.getBbHeight() * 0.9;
                break;
            case "Body":
                yOffset = entity.getBbHeight() * 0.5;
                break;
            case "Feet":
                yOffset = 0;
                break;
        }
        return entity.position().add(0, yOffset, 0);
    }
}