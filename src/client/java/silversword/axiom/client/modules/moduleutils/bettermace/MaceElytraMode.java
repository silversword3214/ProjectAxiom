package silversword.axiom.client.modules.moduleutils.bettermace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.modules.moduleutils.TargetManager;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class MaceElytraMode {

    public final SettingBoolean enabled = new SettingBoolean("Elytra Mode", false);
    public final SettingNumber minHeight = new SettingNumber("Min Height", 5, 100, 1, 20);
    public final SettingNumber targetRange = new SettingNumber("Target Range", 10, 150, 5, 100);
    public final SettingNumber attackRange = new SettingNumber("Attack Range", 1, 8, 0.5, 4.0);
    public final SettingSlider aimSpeed = new SettingSlider("Aim Speed", new double[]{1, 2, 3, 5, 10}, 3);
    public final SettingBoolean respectCooldown = new SettingBoolean("Respect Cooldown", true);
    public final SettingBoolean autoRocket = new SettingBoolean("Auto Rocket", true);
    public final SettingBoolean upwardBoost = new SettingBoolean("Loop Mode", true);
    public final SettingNumber diveBoostDist = new SettingNumber("Dive Boost Dist", 5, 50, 1, 20);
    public final SettingNumber minFallDist = new SettingNumber("Min Fall Distance", 3, 20, 1, 5);
    public final SettingMode targetMode = new SettingMode("Target Mode", new String[]{"Players", "Mobs", "Both"}, "Both");

    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private long lastActionTime = 0;
    private boolean isBoostingUp = false;
    private boolean isDiving = false;
    private boolean hasLockedAim = false;
    private boolean waitingForBoost = false;

    private final MlgHandler mlgHandler = new MlgHandler();
    private double customFallDistance = 0.0;

    public MaceElytraMode() {
        minHeight.setParent(enabled);
        targetRange.setParent(enabled);
        attackRange.setParent(enabled);
        aimSpeed.setParent(enabled);
        respectCooldown.setParent(enabled);
        autoRocket.setParent(enabled);
        upwardBoost.setParent(enabled);
        diveBoostDist.setParent(enabled);
        minFallDist.setParent(enabled);
    }

    public void onPreMotion() {
        if (!enabled.get() || mc.player == null || mc.level == null) return;

        if (waitingForBoost) {
            if (System.currentTimeMillis() - lastAttackTime > 150) {
                waitingForBoost = false;
                startAscent();
            }
            return;
        }

        if (!mc.player.isFallFlying()) {
            resetState();
            return;
        }

        currentTarget = TargetManager.getClosest(mc.level, targetRange.getValue(), targetMode.getMode());
        boolean isDivingTowardTarget = (currentTarget != null && mc.player.getDeltaMovement().y < -0.1);

        if (!isDivingTowardTarget) {
            if (mlgHandler.tick() || mlgHandler.isMlgActive()) {
                return;
            }
        }

        if (currentTarget == null) {
            resetState();
            return;
        }

        double dist = mc.player.distanceTo(currentTarget);
        double deltaY = mc.player.getDeltaMovement().y;

        if (isDiving && deltaY < -0.05) {
            customFallDistance += Math.abs(deltaY);
            mc.player.fallDistance = (float) customFallDistance;
        }

        if (!isBoostingUp && !isDiving) {
            startAscent();
            return;
        }

        if (isBoostingUp) {
            ensureSwordEquipped();
            double heightDiff = mc.player.getY() - currentTarget.getY();

            // Reach desired height -> start dive
            if (heightDiff >= minHeight.getValue() || (heightDiff > 2 && deltaY < -0.2)) {
                isBoostingUp = false;
                isDiving = true;
                hasLockedAim = false;
                // Do NOT switch to mace here – keep sword for the dive
            }
            return;
        }

        if (isDiving) {
            // Use rocket once to dive toward target (still holding sword)
            if (!hasLockedAim && autoRocket.get()) {
                useRocketTowardTarget();
                hasLockedAim = true;
            }

            // Attack only when in range and enough fall distance
            if (dist <= attackRange.getValue() && deltaY < -0.1 && customFallDistance >= minFallDist.getValue()) {
                if (canAttack()) {
                    performAttack(); // switches to mace just before hitting
                }
            }
        }
    }

    private void performAttack() {
        // 1. Switch to mace right before the attack
        int maceSlot = findMaceSlot();
        if (maceSlot == -1) return;
        setHotbarSlot(maceSlot);

        // 2. Set fall distance for mace smash damage
        mc.player.fallDistance = (float) customFallDistance;

        // 3. Attack
        if (mc.getConnection() != null && mc.player.isFallFlying()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        mc.gameMode.attack(mc.player, currentTarget);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();

        // 4. Optionally switch back to sword after attack (for next loop)
        int swordSlot = findSwordSlot();
        if (swordSlot != -1) {
            setHotbarSlot(swordSlot);
        }

        if (upwardBoost.get()) {
            waitingForBoost = true;
        } else {
            resetState();
        }
    }

    private void startAscent() {
        isBoostingUp = true;
        isDiving = false;
        hasLockedAim = false;
        customFallDistance = 0;
        mc.player.fallDistance = 0;

        ensureSwordEquipped();

        if (!mc.player.isFallFlying() && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }

        if (autoRocket.get()) {
            useRocketUpward();
        }
    }

    private void useRocketUpward() {
        int rocketSlot = findRocketSlot();
        if (rocketSlot == -1) return;

        mc.player.setXRot(-90f);
        int prev = mc.player.getInventory().selected;
        setHotbarSlot(rocketSlot);
        mc.gameMode.useItem(mc.player, mc.player.getUsedItemHand());
        setHotbarSlot(prev);
    }

    private void useRocketTowardTarget() {
        if (currentTarget == null) return;
        int rocketSlot = findRocketSlot();
        if (rocketSlot == -1) return;

        Vec3 toTarget = currentTarget.getEyePosition(1.0f).subtract(mc.player.getEyePosition(1.0f));
        float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z)));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);

        int prev = mc.player.getInventory().selected;
        setHotbarSlot(rocketSlot);
        mc.gameMode.useItem(mc.player, mc.player.getUsedItemHand());
        setHotbarSlot(prev);
    }

    private boolean canAttack() {
        if (respectCooldown.get()) {
            return mc.player.getAttackStrengthScale(0.5f) >= 0.85f;
        }
        return (System.currentTimeMillis() - lastAttackTime) >= 25;
    }

    private void setHotbarSlot(int slot) {
        if (mc.player == null || mc.player.getInventory().selected == slot) return;
        mc.player.getInventory().selected = slot;
    }

    private void ensureSwordEquipped() {
        int swordSlot = findSwordSlot();
        if (swordSlot != -1) setHotbarSlot(swordSlot);
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.MACE)) return i;
        }
        return -1;
    }

    private int findSwordSlot() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem().toString().contains("sword")) return i;
        }
        return -1;
    }

    private int findRocketSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) return i;
        }
        return -1;
    }

    public void resetState() {
        currentTarget = null;
        isBoostingUp = false;
        isDiving = false;
        hasLockedAim = false;
        waitingForBoost = false;
        customFallDistance = 0;
        if (mc.player != null) mc.player.fallDistance = 0;
    }
}