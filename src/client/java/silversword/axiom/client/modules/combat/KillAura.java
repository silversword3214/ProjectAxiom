package silversword.axiom.client.modules.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.*;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class KillAura extends AxiomMod implements KeybindConfigurable {

    private static KillAura instance;

    private final TargetManager targetManager = new TargetManager();
    private final AttackController attackController = new AttackController();
    private final RotationManager rotationManager = new RotationManager();

    private final SilentRotationController silentRotation = new SilentRotationController();

    private LivingEntity currentTarget = null;

    // Hyökkäysjonotus
    private int attackDelay = 0;
    private LivingEntity pendingAttack = null;

    // ---------- Asetukset ----------
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingMode mode = new SettingMode(
            "Mode", new String[]{"Silent", "Legit"}, "Silent"
    );

    private final SettingMode targetMode = new SettingMode(
            "Target Mode", new String[]{"Players", "Mobs", "Both"}, "Players"
    );

    private final SettingMode priorityMode = new SettingMode(
            "Priority", new String[]{"Distance", "Health", "Armor", "Hybrid"}, "Distance"
    );

    private final SettingSlider attackRange = new SettingSlider(
            "Attack Range", new double[]{3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0}, 4.0
    );

    private final SettingSlider minCps = new SettingSlider(
            "Min CPS", new double[]{4, 5, 6, 7, 8, 9, 10, 12, 15}, 8
    );

    private final SettingSlider maxCps = new SettingSlider(
            "Max CPS", new double[]{6, 7, 8, 9, 10, 12, 15, 20}, 12
    );

    private final SettingBoolean checkWalls = new SettingBoolean("Check Walls", true);
    private final SettingBoolean predictMovement = new SettingBoolean("Predict Movement", true);
    private final SettingBoolean ignoreBots = new SettingBoolean("Ignore Bots", true);

    private final SettingSlider maxTurnSpeed = new SettingSlider(
            "Turn Speed (deg/tick)", new double[]{5, 10, 15, 20, 25, 30, 35, 40}, 20
    );

    private final SettingBoolean simulateJitter = new SettingBoolean("Simulate Jitter", true);
    private final SettingSlider jitterAmount = new SettingSlider(
            "Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0}, 2.0
    );

    // Attack jitter (satunnainen poikkeama hyökkäyshetkellä)
    private final SettingBoolean attackJitter = new SettingBoolean("Attack Jitter", true);
    private final SettingSlider attackJitterAmount = new SettingSlider(
            "Attack Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0}, 1.5
    );

    private final SettingBoolean renderTargetBox = new SettingBoolean("Draw Box", true);
    private final SettingColor boxColor = new SettingColor("Box Color", Color.GREEN);

    public KillAura() {
        super("Kill Aura", "Kill Aura with multiple modes", ModuleCategory.COMBAT);

        addSetting(mode);
        addSetting(targetMode);
        addSetting(priorityMode);
        addSetting(attackRange);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(checkWalls);
        addSetting(predictMovement);
        addSetting(ignoreBots);
        addSetting(maxTurnSpeed);
        addSetting(simulateJitter);
        addSetting(jitterAmount);
        addSetting(attackJitter);
        addSetting(attackJitterAmount);
        addSetting(renderTargetBox);
        addHiddenSetting(boxColor.getSetting());
        addHiddenSetting(toggleKey);

        instance = this;
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        targetManager.reset();
        attackController.reset();
        rotationManager.reset();
        currentTarget = null;
        pendingAttack = null;
        attackDelay = 0;
        if (mc.player != null) {
            silentRotation.init(mc.player.getYRot(), mc.player.getXRot());
        }
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        silentRotation.reset();
        pendingAttack = null;
        attackDelay = 0;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        // Käsitellään jonotettu hyökkäys
        if (attackDelay > 0) {
            attackDelay--;
            if (attackDelay == 0 && pendingAttack != null) {
                performAttack(pendingAttack);
                pendingAttack = null;
            }
        }

        LivingEntity target = targetManager.selectTarget(
                mc.player, mc.level,
                priorityMode.getMode(), ignoreBots.get(), targetMode.getMode()
        );
        currentTarget = target;

        if (target == null) {
            silentRotation.reset();
            return;
        }

        double range = attackRange.getValue();
        if (mc.player.distanceTo(target) > range) {
            silentRotation.reset();
            return;
        }

        if (checkWalls.get() && !isTargetVisible(target)) {
            silentRotation.reset();
            return;
        }

        String currentMode = mode.getMode();

        if (currentMode.equals("Silent")) {
            // Laske tavoiterotaatio
            float targetYaw = (float) getYawToTarget(target);
            float targetPitch = (float) getPitchToTarget(target);
            targetYaw = Mth.wrapDegrees(targetYaw);
            targetPitch = Mth.clamp(targetPitch, -90f, 90f);

            float speed = (float) maxTurnSpeed.getValue();
            float jitter = simulateJitter.get() ? (float) jitterAmount.getValue() : 0f;
            silentRotation.setTarget(targetYaw, targetPitch, speed, jitter);
            silentRotation.update();

            // Pelaajan pää kääntyy visuaalisesti
            mc.player.setYHeadRot(silentRotation.getCurrentYaw());
            mc.player.yHeadRotO = silentRotation.getCurrentYaw();

            // Jonotetaan hyökkäys seuraavalle tickille, jotta liikepaketti ehtii mennä ensin
            if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue()) && pendingAttack == null) {
                pendingAttack = target;
                // Satunnainen viive 0–2 tickiä (ihmismäinen)
                attackDelay = 1 + (int) (Math.random() * 2);
            }
        } else if (currentMode.equals("Legit")) {
            RotationManager.Rotation targetRotation = rotationManager.calculateRotation(
                    mc.player, target, predictMovement.get()
            );
            rotationManager.rotateSmoothly(
                    mc.player, targetRotation, (float) maxTurnSpeed.getValue(),
                    simulateJitter.get() ? (float) jitterAmount.getValue() : 0f
            );

            if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue()) && pendingAttack == null) {
                pendingAttack = target;
                attackDelay = 1;
            }
        }
    }

    private void performAttack(LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        // Attack jitter: pieni satunnainen poikkeama rotaatioon hyökkäyshetkellä
        if (attackJitter.get()) {
            float jitter = (float) attackJitterAmount.getValue();
            float yawOffset = (float) ((Math.random() - 0.5) * jitter);
            float pitchOffset = (float) ((Math.random() - 0.5) * jitter * 0.5);

            // Väliaikainen rotaation muutos (vain client-puoli)
            float oldYaw = mc.player.getYRot();
            float oldPitch = mc.player.getXRot();
            mc.player.setYRot(oldYaw + yawOffset);
            mc.player.setXRot(oldPitch + pitchOffset);

            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());

            // Palautetaan alkuperäinen rotaatio
            mc.player.setYRot(oldYaw);
            mc.player.setXRot(oldPitch);
        } else {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
        }

        attackController.recordAttack();
    }

    private double getYawToTarget(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        return Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
    }

    private double getPitchToTarget(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffY = target.getY() + target.getBbHeight() / 2 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = target.getZ() - mc.player.getZ();
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        return -Math.toDegrees(Math.atan2(diffY, distance));
    }

    private boolean isTargetVisible(LivingEntity target) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        HitResult result = mc.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player
        ));
        if (result.getType() == HitResult.Type.MISS) return true;
        if (result instanceof EntityHitResult entityHit) {
            return entityHit.getEntity() == target;
        }
        return false;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || currentTarget == null || !renderTargetBox.get()) return;
        Renderer3D renderer = event.getRenderer();
        AABB box = currentTarget.getBoundingBox();
        int color = boxColor.getCurrentColor().getARGB();
        renderer.boxOutline(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, 0);
    }

    // ---------- Staattiset metodit mixinille ----------
    public static KillAura getInstance() {
        return instance;
    }

    public boolean isSilentModeActive() {
        return isEnabled() && mode.getMode().equals("Silent");
    }

    public SilentRotationController getSilentRotationController() {
        return silentRotation;
    }
}