package silversword.axiom.client.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.killaura.AttackController;
import silversword.axiom.client.modules.moduleutils.killaura.TargetManager;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.Rotations;

import java.util.List;
import java.util.stream.Collectors;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class MultiAura extends AxiomMod implements KeybindConfigurable {

    private final TargetManager targetManager = new TargetManager();
    private final AttackController attackController = new AttackController();
    private List<LivingEntity> currentTargets = null;

    // ---------- Settings ----------
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingSlider attackRange = new SettingSlider("Range", new double[]{3.0, 4.0, 5.0, 6.0}, 4.5);
    private final SettingSlider maxTargets = new SettingSlider("Max Targets", new double[]{2, 3, 4, 5, 10}, 3);
    private final SettingMode targetMode = new SettingMode("Targets", new String[]{"Players", "Mobs", "Both"}, "Both");
    private final SettingBoolean renderBoxes = new SettingBoolean("Draw Boxes", true);
    private final SettingColor boxColor = new SettingColor("Target Color", Color.RED);

    public MultiAura() {
        super("Multi Aura", "Attacks multiple entities at once", ModuleCategory.COMBAT);
        addSetting(attackRange);
        addSetting(maxTargets);
        addSetting(targetMode);
        addSetting(renderBoxes);
        addHiddenSetting(boxColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onDisable() {
        currentTargets = null;
        attackController.reset();
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // 1. Etsitään kaikki mahdolliset kohteet alueelta
        // Käytetään streamia suodattamiseen
        currentTargets = mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(attackRange.getValue()))
                .stream()
                .filter(e -> e != mc.player && e.isAlive())
                .filter(e -> mc.player.distanceTo(e) <= attackRange.getValue())
                .filter(e -> isValidTarget(e))
                .limit((int) maxTargets.getValue())
                .collect(Collectors.toList());

        if (currentTargets.isEmpty()) return;

        // 2. Hyökkäyslogiikka
        // MultiAurassa usein ohitetaan monimutkaiset kääntymiset (Silent-tyyli on tehokkain)
        if (attackController.canAttack(mc.player)) {
            for (LivingEntity entity : currentTargets) {
                performMultiAttack(entity);
            }
            attackController.recordAttack();
        }
    }

    private void performMultiAttack(LivingEntity target) {
        // Lasketaan suunta kohteeseen (Silent rotation periaate, mutta ei välttämättä päivitetä serverille asti jokaiselle erikseen)
        float yaw = (float) getYawToTarget(target);
        float pitch = (float) getPitchToTarget(target);

        // Suoritetaan hyökkäys paketteina
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(mc.player.getUsedItemHand());
    }

    private boolean isValidTarget(LivingEntity e) {
        String mode = targetMode.getMode();
        boolean isPlayer = e instanceof net.minecraft.world.entity.player.Player;
        if (mode.equals("Players")) return isPlayer;
        if (mode.equals("Mobs")) return !isPlayer;
        return true;
    }

    // --- Apu-metodit (Kopioitu KillAurasta yhteensopivuuden vuoksi) ---

    private double getYawToTarget(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        return Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
    }

    private double getPitchToTarget(LivingEntity target) {
        double diffY = target.getY() + target.getBbHeight() / 2 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        return -Math.toDegrees(Math.atan2(diffY, distance));
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || currentTargets == null || !renderBoxes.get()) return;
        Renderer3D renderer = event.getRenderer();
        for (LivingEntity target : currentTargets) {
            AABB box = target.getBoundingBox();
            renderer.boxOutline(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, boxColor.getCurrentColor().getARGB(), 0);
        }
    }
}