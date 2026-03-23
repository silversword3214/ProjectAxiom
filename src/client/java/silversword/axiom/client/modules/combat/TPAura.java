package silversword.axiom.client.modules.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.AttackController;
import silversword.axiom.client.modules.moduleutils.SmartKillAura.TargetManager;


import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
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

        double distance = mc.player.distanceTo(target);
        double maxTpDist = tpRange.getValue();

        if (distance > maxTpDist) return;

        if (checkWalls.get() && !isTargetVisible(target)) return;

        if (attackController.canAttack(mc.player, minCps.getValue(), maxCps.getValue())) {
            Vec3 tpPos = calculateTpPosition(target);
            if (tpPos != null && isSafePosition(tpPos)) {
                // Teleporttaa serverille ja clientille
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(tpPos.x, tpPos.y, tpPos.z, true, true));
                mc.player.setPos(tpPos);
                mc.player.setDeltaMovement(0, 0, 0);

                // Hyökkää
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(mc.player.getUsedItemHand());
                attackController.recordAttack();
            }
        }
    }

    private Vec3 calculateTpPosition(LivingEntity target) {
        Vec3 targetPos = target.position();
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
                float yaw = target.getYRot();
                double rad = Math.toRadians(yaw);
                return targetPos.add(-Math.sin(rad) * radius, 0, Math.cos(rad) * radius);
            default:
                angle = 0;
        }
        double x = targetPos.x + Math.cos(angle) * radius;
        double z = targetPos.z + Math.sin(angle) * radius;
        double y = targetPos.y; // pyritään samalle tasolle
        return new Vec3(x, y, z);
    }

    private boolean isSafePosition(Vec3 pos) {
        AABB playerBox = mc.player.getBoundingBox().move(pos.subtract(mc.player.position()));
        return mc.level.noCollision(playerBox);
    }

    private boolean isTargetVisible(LivingEntity target) {
        Vec3 start = mc.player.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        BlockHitResult result = mc.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player
        ));
        return result.getType() == HitResult.Type.MISS;
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