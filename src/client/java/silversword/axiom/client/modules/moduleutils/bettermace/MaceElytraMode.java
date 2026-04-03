package silversword.axiom.client.modules.moduleutils.bettermace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.modules.moduleutils.TargetManager;
import silversword.axiom.client.setting.SettingBoolean;
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

    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private long lastActionTime = 0; // Viiveitä varten
    private boolean isBoostingUp = false;
    private boolean isDiving = false;
    private boolean hasLockedAim = false;
    private boolean hasSwitchedToMace = false;
    private boolean waitingForBoost = false;
    private boolean mlgTriggered = false;
    private int mlgDelayTicks = 0;


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

        if (currentTarget == null && !mc.player.onGround()) {
            handleMlg();
        }

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

        currentTarget = TargetManager.getClosest(mc.level, targetRange.getValue(), "Both");
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

            // Vaihdetaan syöksyyn jos ollaan tarpeeksi korkealla
            if (heightDiff >= minHeight.getValue() || (heightDiff > 2 && deltaY < -0.2)) {
                isBoostingUp = false;
                isDiving = true;
                hasLockedAim = false;

                int maceSlot = findMaceSlot();
                if (maceSlot != -1) {
                    setHotbarSlot(maceSlot);
                    hasSwitchedToMace = true;
                }
            }
            return;
        }

        if (isDiving) {
            if (!hasLockedAim && autoRocket.get()) {
                useRocketTowardTarget();
                hasLockedAim = true;
            }

            if (!hasSwitchedToMace) {
                int maceSlot = findMaceSlot();
                if (maceSlot != -1) {
                    setHotbarSlot(maceSlot);
                    hasSwitchedToMace = true;
                }
            }

            if (dist <= attackRange.getValue() && deltaY < -0.1 && customFallDistance >= minFallDist.getValue()) {
                if (canAttack()) {
                    performAttack();
                }
            }
        }
    }

    private void handleMlg() {
        if (mlgDelayTicks > 0) {
            mlgDelayTicks--;
            return;
        }

        if (mc.player == null || mc.level == null) return;
        if (currentTarget != null) return;
        if (mc.player.onGround()) {
            mlgTriggered = false;
            return;
        }

        double fallSpeed = -mc.player.getDeltaMovement().y;
        double distanceToGround = getDistanceToGround();


        boolean dangerousFall = (fallSpeed > 1.0 && distanceToGround < 5.0 && distanceToGround > 0.5);
        boolean tooHigh = (distanceToGround > 200.0);
        if (!dangerousFall || tooHigh) return;

        int mlgSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            var item = stack.getItem();

            if (item == Items.WATER_BUCKET) {
                mlgSlot = i;
                break;
            }
            if (item == Items.POWDER_SNOW_BUCKET) {
                mlgSlot = i;
                break;
            }
            if (item == Items.TWISTING_VINES || item == Items.WEEPING_VINES) {
                mlgSlot = i;
                break;
            }
        }

        if (mlgSlot == -1 || mlgTriggered) return;

        int prevSlot = mc.player.getInventory().selected;
        setHotbarSlot(mlgSlot);

        mc.gameMode.useItem(mc.player, mc.player.getUsedItemHand());

        setHotbarSlot(prevSlot);

        mlgTriggered = true;
        mlgDelayTicks = 20;
    }

    private double getDistanceToGround() {
        if (mc.player == null || mc.level == null) return 0;
        Vec3 pos = mc.player.position();
        double startY = pos.y;

        for (double y = startY; y > startY - 100; y -= 0.5) {
            if (mc.level.getBlockState(new BlockPos((int)pos.x, (int)y, (int)pos.z)).isSolid()) {
                return startY - y;
            }
        }
        return 100.0;
    }

    private void performAttack() {
        int maceSlot = findMaceSlot();
        if (maceSlot == -1) return;

        setHotbarSlot(maceSlot);
        mc.player.fallDistance = (float) customFallDistance;

        if (mc.getConnection() != null && mc.player.isFallFlying()) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }

        mc.gameMode.attack(mc.player, currentTarget);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();

        int swordSlot = findSwordSlot();
        if (swordSlot != -1) {
            setHotbarSlot(swordSlot);
            hasSwitchedToMace = false;
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
        hasSwitchedToMace = false;
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
        return (System.currentTimeMillis() - lastAttackTime) >= 150;
    }

    private void setHotbarSlot(int slot) {
        if (mc.player == null || mc.player.getInventory().selected == slot) return;
        mc.player.getInventory().selected = slot;
    }

    private void ensureSwordEquipped() {
        if (hasSwitchedToMace) return;
        int swordSlot = findSwordSlot();
        if (swordSlot != -1) setHotbarSlot(swordSlot);
    }

    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.MACE)) return i;
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
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) return i;
        return -1;
    }

    public void resetState() {
        currentTarget = null;
        isBoostingUp = false;
        isDiving = false;
        hasLockedAim = false;
        hasSwitchedToMace = false;
        waitingForBoost = false;
        customFallDistance = 0;
        if (mc.player != null) mc.player.fallDistance = 0;
    }
}