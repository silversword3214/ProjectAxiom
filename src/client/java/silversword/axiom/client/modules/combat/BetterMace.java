package silversword.axiom.client.modules.combat;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.mouse.MouseUpdateEvent;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.TargetManager;
import silversword.axiom.client.modules.moduleutils.bettermace.MaceElytraMode;
import silversword.axiom.client.modules.moduleutils.bettermace.MlgHandler;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.client.setting.SettingMode;

import java.util.Comparator;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class BetterMace extends AxiomMod {

    private final SettingBoolean enableAutoSwap = new SettingBoolean("AutoSwap Logic", true);
    private final SettingBoolean infiniteWindBurst = new SettingBoolean("Infinite WindBurst", true);
    private final SettingBoolean moveToTarget = new SettingBoolean("Move to Target", true);

    private final SettingBoolean enableAimAssist = new SettingBoolean("Aim Assist", true);
    private final SettingSlider  aimSpeed        = new SettingSlider("Aim Speed", new double[]{1, 2, 3, 5, 10}, 3);
    private final SettingNumber  maxAngle        = new SettingNumber("Max Angle (deg)", 1, 180, 5, 60);

    private final SettingBoolean respectCooldown = new SettingBoolean("Respect Cooldown", true);
    private final SettingNumber  range           = new SettingNumber("Range", 1, 6, 0.1, 4.0);
    private final SettingNumber  targetRange     = new SettingNumber("Target Range", 1, 100, 0.5, 50.0);
    private final SettingMode    targetMode      = new SettingMode("Target Mode", new String[]{"Players", "Mobs", "Both"}, "Both");

    private final SettingNumber maxHorizontalDist = new SettingNumber("Max Horizontal Dist", 1, 20, 0.5, 7.0);


    private final MaceElytraMode maceElytraMode = new MaceElytraMode();

    private long lastAttackTime = 0;
    private LivingEntity currentTarget = null;
    private boolean isSlamming = false;
    private boolean wasMoving = false;
    private boolean hasAttackedThisFall = false;

    private final MlgHandler mlgHandler = new MlgHandler();

    public BetterMace() {
        super("BetterMace", "Get good with mace", ModuleCategory.COMBAT);
        infiniteWindBurst.setParent(enableAutoSwap);
        moveToTarget.setParent(enableAutoSwap);
        aimSpeed.setParent(enableAimAssist);
        maxAngle.setParent(enableAimAssist);


        addSetting(maceElytraMode.enabled);
        addSetting(maceElytraMode.minHeight);
        addSetting(maceElytraMode.targetRange);
        addSetting(maceElytraMode.attackRange);
        addSetting(maceElytraMode.aimSpeed);
        addSetting(maceElytraMode.respectCooldown);
        addSetting(maceElytraMode.autoRocket);
        addSetting(maceElytraMode.upwardBoost);
        addSetting(maceElytraMode.diveBoostDist);
        addSetting(maceElytraMode.minFallDist);
        addSetting(maceElytraMode.targetMode);

        addSetting(enableAutoSwap);
        addSetting(infiniteWindBurst);
        addSetting(moveToTarget);
        addSetting(enableAimAssist);
        addSetting(aimSpeed);
        addSetting(maxAngle);
        addSetting(respectCooldown);
        addSetting(range);
        addSetting(targetRange);
        addSetting(targetMode);
        addSetting(maxHorizontalDist);


    }

    @Override
    protected void onEnable() {
        resetState();
        mlgHandler.reset();
        maceElytraMode.resetState();
    }

    @Override
    protected void onDisable() {
        releaseModMovement();
        resetState();
        maceElytraMode.resetState();
    }

    private void resetState() {
        lastAttackTime = 0;
        currentTarget = null;
        isSlamming = false;
        wasMoving = false;
        hasAttackedThisFall = false;
        releaseModMovement();
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        if (maceElytraMode.enabled.get()) {
            maceElytraMode.enabled.set(true);
            maceElytraMode.onPreMotion();
            return;
        } else {
            maceElytraMode.enabled.set(false);
        }

        if (!mc.player.onGround()) {
            boolean isPowerfulJump = mc.player.getDeltaMovement().y > 0.45;
            boolean isHighEnough = isMinimumHeight(2.0);
            if (isPowerfulJump || isHighEnough || (isSlamming && mc.player.getDeltaMovement().y > -0.5)) {
                isSlamming = true;
            }
        } else {
            isSlamming = false;
            hasAttackedThisFall = false;
        }

        boolean shouldFindTarget = enableAutoSwap.get() && !maceElytraMode.enabled.get() && !mc.player.onGround()
                && (isSlamming || mc.player.getDeltaMovement().y < -0.3);
        if (shouldFindTarget) {
            currentTarget = findTargetWithHorizontalLimit();
        } else {
            currentTarget = null;
        }


        if (currentTarget == null || mc.player.onGround()) {
            releaseModMovement();
        }


        boolean isMaceAttackImminent = (currentTarget != null && mc.player.getDeltaMovement().y < -0.1);
        if (!isMaceAttackImminent) {
            if (mlgHandler.tick() || mlgHandler.isMlgActive()) {
                return;
            }
        }


        if (!enableAutoSwap.get()) return;

        if (mc.player.onGround()) {
            isSlamming = false;
            hasAttackedThisFall = false;
        }

        boolean isPowerfulJump = mc.player.getDeltaMovement().y > 0.45;
        boolean isHighEnough = isMinimumHeight(2.0);
        boolean isFalling = mc.player.getDeltaMovement().y < -0.1;

        if (!mc.player.onGround()) {
            if (isPowerfulJump || isHighEnough || (isSlamming && mc.player.getDeltaMovement().y > -0.5)) {
                isSlamming = true;
            }
        }

        if (moveToTarget.get() && isSlamming && !mc.player.onGround() && currentTarget != null) {
            handleMoveToTarget();
        } else {
            releaseModMovement();
        }

        if (!isSlamming || mc.player.onGround()) return;

        int maceSlot = findItemSlot(Items.MACE);
        int swordSlot = findSwordSlot();
        if (maceSlot == -1) return;

        double dist = currentTarget != null ? mc.player.distanceTo(currentTarget) : 999;

        if (infiniteWindBurst.get()) {
            if (dist > range.getValue() - 0.2 && swordSlot != -1) {
                if (mc.player.getInventory().selected != swordSlot) {
                    mc.player.getInventory().selected = swordSlot;
                }
            } else if (dist <= range.getValue() && isFalling) {
                if (mc.player.getInventory().selected != maceSlot) {
                    mc.player.getInventory().selected = maceSlot;
                }
                if (canAttack(currentTarget)) {
                    performAttack(currentTarget);
                }
            }
        } else {
            if (!hasAttackedThisFall && dist <= range.getValue() && isFalling) {
                if (mc.player.getInventory().selected != maceSlot) {
                    mc.player.getInventory().selected = maceSlot;
                }
                if (canAttack(currentTarget)) {
                    performAttack(currentTarget);
                    hasAttackedThisFall = true;
                }
            } else if (dist > range.getValue() - 0.2 && swordSlot != -1) {
                if (mc.player.getInventory().selected != swordSlot) {
                    mc.player.getInventory().selected = swordSlot;
                }
            }
        }
    }

    private LivingEntity findTargetWithHorizontalLimit() {
        if (mc.player == null || mc.level == null) return null;
        double maxRange = targetRange.getValue();
        double maxHor = maxHorizontalDist.getValue();
        return mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(maxRange),
                entity -> entity != mc.player && entity.isAlive() &&
                        TargetManager.isValidTarget(entity, targetMode.getMode()) &&
                        mc.player.distanceTo(entity) <= maxRange &&
                        Math.hypot(entity.getX() - mc.player.getX(), entity.getZ() - mc.player.getZ()) <= maxHor &&
                        hasLineOfSight(entity)
        ).stream().min(Comparator.comparingDouble(e -> mc.player.distanceTo(e))).orElse(null);
    }

    private boolean hasLineOfSight(LivingEntity target) {
        if (mc.player == null || mc.level == null || target == null) return false;
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = target.getEyePosition(1.0f);
        BlockHitResult result = mc.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (result.getType() == HitResult.Type.MISS) return true;
        Vec3 hitVec = result.getLocation();
        double distToHit = start.distanceTo(hitVec);
        double distToTarget = start.distanceTo(end);
        return distToHit >= distToTarget - 0.5;
    }

    private void handleMoveToTarget() {
        if (mc.player == null || currentTarget == null) return;
        double dist = mc.player.distanceTo(currentTarget);
        if (dist > 1.5) {
            Vec3 toTarget = currentTarget.position().subtract(mc.player.position());
            toTarget = new Vec3(toTarget.x, 0, toTarget.z).normalize();
            Vec3 lookVec = mc.player.getLookAngle().normalize();
            double dot = lookVec.dot(toTarget);
            if (dot > 0) {
                if (!wasMoving) {
                    mc.options.keyUp.setDown(true);
                    wasMoving = true;
                }
                mc.player.setSprinting(true);
            } else {
                releaseModMovement();
            }
        } else {
            releaseModMovement();
        }
    }

    private void releaseModMovement() {
        if (wasMoving) {
            if (mc.options != null) {
                mc.options.keyUp.setDown(false);
            }
            wasMoving = false;
        }
    }

    private void performAttack(LivingEntity target) {
        if (mc.gameMode == null || mc.level == null) return;
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
        if (infiniteWindBurst.get()) {
            int swordSlot = findSwordSlot();
            if (swordSlot != -1) mc.player.getInventory().selected = swordSlot;
        } else {
            int windChargeSlot = findWindChargeSlot();
            if (windChargeSlot != -1) mc.player.getInventory().selected = windChargeSlot;
        }
    }

    private boolean isMinimumHeight(double height) {
        if (mc.player == null || mc.level == null) return false;
        Vec3 start = mc.player.position();
        Vec3 end = start.add(0, -height, 0);
        BlockHitResult result = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private boolean canAttack(LivingEntity target) {
        if (respectCooldown.get() && mc.player.getAttackStrengthScale(0.5f) < 0.92f) return false;
        return (System.currentTimeMillis() - lastAttackTime) >= 150;
    }

    private int findItemSlot(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(item)) return i;
        return -1;
    }

    private int findSwordSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemTags.SWORDS)) return i;
        }
        return -1;
    }

    private int findWindChargeSlot() {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.WIND_CHARGE)) return i;
        return -1;
    }

    @Subscribe
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (!isEnabled()) return;
        if (maceElytraMode.enabled.get()) return;
        if (currentTarget == null || !enableAimAssist.get() || mc.player == null) return;
        if (mc.player.distanceTo(currentTarget) > targetRange.getValue()) return;

        boolean isHighFall = mc.player.fallDistance > 2.5f;
        boolean isSlammingAndFalling = isSlamming && !mc.player.onGround() && mc.player.getDeltaMovement().y < -0.1;

        if (!isHighFall && !isSlammingAndFalling) return;
        if (mc.player.onGround() || mc.player.getDeltaMovement().y >= 0) return;

        Vec3 targetPos = currentTarget.getEyePosition(1.0f);
        Vec3 playerPos = mc.player.getEyePosition(1.0f);

        Vec3 delta = targetPos.subtract(playerPos);

        double yawTarget = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        double pitchTarget = -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        double diffYaw = wrapAngle(yawTarget - mc.player.getYRot());
        double diffPitch = pitchTarget - mc.player.getXRot();

        if (Math.abs(diffYaw) <= maxAngle.getValue()) {
            double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
            double mult = (sensitivity * sensitivity * sensitivity * 8.0) * 0.15;

            event.setDeltaX((diffYaw / mult) * (aimSpeed.getValue() / 10.0));
            event.setDeltaY((diffPitch / mult) * (aimSpeed.getValue() / 10.0));
        }
    }

    private double wrapAngle(double angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    @Override
    protected void onTick() {}
}