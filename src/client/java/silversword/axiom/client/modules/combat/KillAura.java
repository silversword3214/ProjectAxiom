package silversword.axiom.client.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;          // korjattu
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.AttackController;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.RotationHandler;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.RotationManager;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.TargetManager;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;   // lisätty
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;


import static silversword.axiom.client.main.AxiomInitialize.mc;

public class KillAura extends AxiomMod implements KeybindConfigurable {

    private final TargetManager targetManager = new TargetManager();
    private final AttackController attackController = new AttackController();
    private final RotationManager rotationManager = new RotationManager();


    private LivingEntity currentTarget = null;

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

    // Yhteiset rotaatioasetukset
    private final SettingSlider maxTurnSpeed = new SettingSlider(
            "Turn Speed (deg/tick)", new double[]{5, 10, 15, 20, 25, 30, 35, 40}, 20
    );

    // Jitter-asetukset (vain silent-moodille)
    private final SettingBoolean simulateJitter = new SettingBoolean("Simulate Jitter", true);
    private final SettingSlider jitterAmount = new SettingSlider(
            "Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0}, 2.0
    );

    // Renderöinti
    private final SettingBoolean renderTargetBox = new SettingBoolean("Draw Box", true);
    private final SettingColor boxColor = new SettingColor("Box Color", Color.GREEN);

    public KillAura() {
        super("Kill Aura", "Kill Auta with multiple modes", ModuleCategory.COMBAT);

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
        addSetting(renderTargetBox);
        addHiddenSetting(boxColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        targetManager.reset();
        attackController.reset();
        rotationManager.reset();
        currentTarget = null;
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = targetManager.selectTarget(
                mc.player,
                mc.level,
                priorityMode.getMode(),
                ignoreBots.get(),
                targetMode.getMode()
        );
        currentTarget = target;
        if (target == null) return;

        double range = attackRange.getValue();
        if (mc.player.distanceTo(target) > range) return;

        if (checkWalls.get() && !isTargetVisible(target)) return;

        String currentMode = mode.getMode();

        if (currentMode.equals("Silent")) {
            // Laske tavoitearvot
            double yaw = getYawToTarget(target);
            double pitch = getPitchToTarget(target);

            // Asetetaan pää kääntymään (vain client-puoli, ei vaikuta serveriin)
            mc.player.setYHeadRot((float) yaw);
            mc.player.yHeadRotO = (float) yaw;

            // Lähetetään rotaatiopaketit VAIN jos hyökätään
            if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue())) {
                RotationHandler.rotate(yaw, pitch, 0, null);
            }
        } else if (currentMode.equals("Legit")) {
            // Legit: käännetään kameraa (ja päätä) sulavasti
            RotationManager.Rotation targetRotation = rotationManager.calculateRotation(
                    mc.player, target, predictMovement.get()
            );
            rotationManager.rotateSmoothly(
                    mc.player, targetRotation, (float) maxTurnSpeed.getValue(), 0f
            );
        }

        // Hyökkäys (tapahtuu vain kun cooldown sallii)
        if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue())) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
            attackController.recordAttack();
        }
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
}