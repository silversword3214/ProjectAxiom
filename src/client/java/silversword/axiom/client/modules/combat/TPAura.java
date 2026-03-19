package silversword.axiom.client.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.AttackController;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.TargetManager;
import silversword.axiom.client.eventbus.AxiomEvent;

import silversword.axiom.client.render.rendersystem.Renderer3D;

import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TPAura extends AxiomMod implements KeybindConfigurable {

    private final TargetManager targetManager = new TargetManager();
    private final AttackController attackController = new AttackController();

    private LivingEntity currentTarget = null;

    // Asetukset
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingMode targetMode = new SettingMode(
            "Target Mode", new String[]{"Players", "Mobs", "Both"}, "Players"
    );

    private final SettingMode priorityMode = new SettingMode(
            "Priority", new String[]{"Distance", "Health", "Armor", "Hybrid"}, "Distance"
    );

    private final SettingSlider tpRange = new SettingSlider(
            "TP Range", new double[]{2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0}, 4.0
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

    private final SettingMode tpMode = new SettingMode(
            "TP Mode", new String[]{"Random", "Circle", "Above", "Behind"}, "Random"
    );

    private final SettingBoolean renderTargetBox = new SettingBoolean("Draw Box", true);
    private final SettingColor boxColor = new SettingColor("Box Color", Color.GREEN);

    public TPAura() {
        super("TP Aura", "Teleports around the target and attacks", ModuleCategory.COMBAT);

        addSetting(targetMode);
        addSetting(priorityMode);
        addSetting(tpRange);
        addSetting(attackRange);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(checkWalls);
        addSetting(predictMovement);
        addSetting(ignoreBots);
        addSetting(tpMode);
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
        currentTarget = null;
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = targetManager.selectTarget(
                mc.player,
                mc.world,
                priorityMode.getMode(),
                ignoreBots.get(),
                targetMode.getMode()
        );
        currentTarget = target;
        if (target == null) return;

        double distance = mc.player.distanceTo(target);
        double maxTpDist = tpRange.getValue();

        if (distance > maxTpDist) return;

        if (checkWalls.get() && !isTargetVisible(target)) return;

        if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue())) {
            Vec3d tpPos = calculateTpPosition(target);
            if (tpPos != null && isSafePosition(tpPos)) {
                // Teleporttaa serverille ja clientille
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(tpPos.x, tpPos.y, tpPos.z, true, true));
                mc.player.setPosition(tpPos);
                mc.player.setVelocity(0, 0, 0);

                // Hyökkää
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(mc.player.getActiveHand());
                attackController.recordAttack();
            }
        }
    }

    private Vec3d calculateTpPosition(LivingEntity target) {
        Vec3d targetPos = target.getEntityPos();
        double radius = tpRange.getValue() * 0.9; // pieni marginaali
        double angle;

        switch (tpMode.getMode()) {
            case "Random":
                angle = Math.random() * 2 * Math.PI;
                break;
            case "Circle":
                angle = (System.currentTimeMillis() / 1000.0) % (2 * Math.PI);
                break;
            case "Above":
                return targetPos.add(0, radius, 0);
            case "Behind":
                float yaw = target.getYaw();
                double rad = Math.toRadians(yaw);
                return targetPos.add(-Math.sin(rad) * radius, 0, Math.cos(rad) * radius);
            default:
                angle = 0;
        }
        double x = targetPos.x + Math.cos(angle) * radius;
        double z = targetPos.z + Math.sin(angle) * radius;
        double y = targetPos.y; // pyritään samalle tasolle
        return new Vec3d(x, y, z);
    }

    private boolean isSafePosition(Vec3d pos) {
        Box playerBox = mc.player.getBoundingBox().offset(pos.subtract(mc.player.getEntityPos()));
        return mc.world.isSpaceEmpty(playerBox);
    }

    private boolean isTargetVisible(LivingEntity target) {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = target.getBoundingBox().getCenter();
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        ));
        return result.getType() == HitResult.Type.MISS;
    }

    @AxiomEvent
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || currentTarget == null || !renderTargetBox.get()) return;

        Renderer3D renderer = event.render; // Oletetaan, että event.renderer on nyt tyyppiä WorldRenderer
        Box box = currentTarget.getBoundingBox();
        Color color = boxColor.getCurrentColor().copy().a(255);
        renderer.boxOutline(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, 0);
    }
}