package silversword.axiom.client.modules.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import silversword.axiom.client.event.mouse.MouseUpdateEvent;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.modules.moduleutils.killaura.AttackController;
import silversword.axiom.client.modules.moduleutils.killaura.TargetManager;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.Rotations;

import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class KillAura extends AxiomMod implements KeybindConfigurable, ColorConfigurable {
    private static KillAura instance;

    private final TargetManager targetManager = new TargetManager();
    private final AttackController attackController = new AttackController();

    private LivingEntity currentTarget = null;
    private float targetYawForMouse, targetPitchForMouse;
    private boolean shouldSimulateMouse = false;

    private boolean autoJumped = false;
    private boolean autoWaitingForFall = false;
    private int autoFallDelay = 0;

    // Smart-tilan apumuuttujat
    private int smartFallDelay = 0;



    public boolean isAutoCritsActive() {
        return isEnabled() && critsMode.getMode().equals("Auto");
    }

    // ---------- Settings ----------
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingMode mode = new SettingMode("Mode", new String[]{"Silent", "Legit"}, "Legit");
    private final SettingMode targetMode = new SettingMode("Target Mode", new String[]{"Players", "Mobs", "Both"}, "Players");
    private final SettingMode priorityMode = new SettingMode("Priority", new String[]{"Distance", "Health", "Armor", "Hybrid"}, "Distance");
    private final SettingSlider attackRange = new SettingSlider("Attack Range", new double[]{3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0}, 4.0);
    private final SettingBoolean checkWalls = new SettingBoolean("Check Walls", true);
    private final SettingBoolean ignoreBots = new SettingBoolean("Ignore Bots", true);
    private final SettingSlider maxTurnSpeed = new SettingSlider("Turn Speed (deg/tick)", new double[]{5, 10, 15, 20, 25, 30, 35, 40, 60, 80}, 40);
    private final SettingBoolean simulateJitter = new SettingBoolean("Simulate Jitter", true);
    private final SettingSlider jitterAmount = new SettingSlider("Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0}, 2.0);
    private final SettingBoolean attackJitter = new SettingBoolean("Attack Jitter", true);
    private final SettingSlider attackJitterAmount = new SettingSlider("Attack Jitter Amount", new double[]{0.5, 1.0, 1.5, 2.0, 2.5, 3.0}, 1.5);
    private final SettingBoolean renderTargetBox = new SettingBoolean("Draw Box", true);
    private final SettingColor portalColor = new SettingColor("Portal Color", new Color(100, 0, 150, 180));
    private final SettingBoolean renderPortalEffect = new SettingBoolean("Portal Effect", false);
    private final SettingSlider portalRadius = new SettingSlider("Portal Radius", new double[]{0.5, 0.8, 1.0, 1.2, 1.5, 1.8, 2.0}, 1.2);
    private final SettingSlider portalVerticalSegments = new SettingSlider("Vertical Segments", new double[]{4, 8, 12, 16, 20, 24, 32}, 16);
    private final SettingSlider portalHorizontalSegments = new SettingSlider("Horizontal Segments", new double[]{8, 12, 16, 20, 24, 32, 48}, 24);
    private final SettingColor boxColor = new SettingColor("Box Color", Color.GREEN);

    // Uusi asetus: Crits Mode
    private final SettingMode critsMode = new SettingMode("Crits Mode", new String[]{"None", "Smart", "Silent", "Auto"}, "None");

    public KillAura() {
        super("Kill Aura", "Automatically attacks entities", ModuleCategory.COMBAT);
        addSetting(mode);
        addSetting(targetMode);
        addSetting(priorityMode);
        addSetting(attackRange);
        addSetting(checkWalls);
        addSetting(ignoreBots);
        addSetting(maxTurnSpeed);
        addSetting(simulateJitter);
        addSetting(jitterAmount);
        addSetting(attackJitter);
        addSetting(attackJitterAmount);
        addSetting(renderTargetBox);
        addSetting(renderPortalEffect);
        addSetting(critsMode);

        addHiddenSetting(portalColor.getSetting());
        addSetting(portalRadius);
        addSetting(portalVerticalSegments);
        addSetting(portalHorizontalSegments);
        addHiddenSetting(boxColor.getSetting());
        addHiddenSetting(toggleKey);
        instance = this;
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        targetManager.reset();
        attackController.reset();
        currentTarget = null;
        shouldSimulateMouse = false;
        smartFallDelay = 0;
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        shouldSimulateMouse = false;
        if (attackController != null) attackController.reset();
        smartFallDelay = 0;
        autoJumped = false;
        autoWaitingForFall = false;
        autoFallDelay = 0;
    }

    @Subscribe
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (mc.player == null) return;
        if (!isEnabled() || !mode.getMode().equals("Legit") || !shouldSimulateMouse) return;

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        float yawDiff = Mth.wrapDegrees(targetYawForMouse - currentYaw);
        float pitchDiff = targetPitchForMouse - currentPitch;

        if (Math.abs(yawDiff) < 0.01f && Math.abs(pitchDiff) < 0.01f) {
            shouldSimulateMouse = false;
            return;
        }

        float maxTurnPerTick = (float) maxTurnSpeed.getValue();
        float yawStep = Mth.clamp(yawDiff, -maxTurnPerTick, maxTurnPerTick);
        float pitchStep = Mth.clamp(pitchDiff, -maxTurnPerTick, maxTurnPerTick);

        if (simulateJitter.get()) {
            float jitter = (float) jitterAmount.getValue();
            yawStep += (float) ((Math.random() - 0.5) * jitter);
            pitchStep += (float) ((Math.random() - 0.5) * jitter * 0.5);
        }

        event.setDeltaX(event.getDeltaX() + yawStep);
        event.setDeltaY(event.getDeltaY() + pitchStep);
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.level == null) return;

        // 1. Kohteen valinta
        currentTarget = targetManager.selectTarget(
                mc.player, mc.level,
                priorityMode.getMode(), ignoreBots.get(), targetMode.getMode()
        );

        if (currentTarget == null) {
            shouldSimulateMouse = false;
            smartFallDelay = 0;
            return;
        }

        if (mc.player.distanceTo(currentTarget) > attackRange.getValue()) {
            shouldSimulateMouse = false;
            smartFallDelay = 0;
            return;
        }

        if (checkWalls.get() && !isTargetVisible(currentTarget)) {
            shouldSimulateMouse = false;
            smartFallDelay = 0;
            return;
        }

        float targetYaw = (float) getYawToTarget(currentTarget);
        float targetPitch = (float) getPitchToTarget(currentTarget);
        targetYaw = Mth.wrapDegrees(targetYaw);
        targetPitch = Mth.clamp(targetPitch, -90f, 90f);

        // ─── CRITS MODE -LOGIIKKA ───────────────────────────────────────────
        String critMode = critsMode.getMode();
        boolean canCritNow = true; // oletus, että saa hyökätä

        if (!critMode.equals("None") && attackController.canAttack(mc.player)) {
            if (critMode.equals("Smart")) {
                // Jos pelaaja on nousussa (hyppää), laitetaan viive
                if (!mc.player.onGround() && mc.player.getDeltaMovement().y() > 0) {
                    smartFallDelay = 2; // odotetaan 2 tickiä putoamista
                }
                if (smartFallDelay > 0) {
                    smartFallDelay--;
                    canCritNow = false; // ei hyökätä tällä tickillä
                }
            }
            else if (critMode.equals("Silent")) {
                doPacketCrit(); // lähetetään packet-krit
            }
            else if (critMode.equals("Auto")) {
                if (!autoWaitingForFall && autoFallDelay == 0) {
                    // Ei odotustilassa, tee hyppy
                    doAutoJumpCrit();
                    canCritNow = false;
                } else if (autoFallDelay > 0) {
                    // Odotetaan viivettä
                    autoFallDelay--;
                    if (autoFallDelay == 0) {
                        // Viive ohi, salli isku ja nollaa tila
                        autoWaitingForFall = false;
                        canCritNow = true;
                    } else {
                        canCritNow = false;
                    }
                } else if (autoWaitingForFall) {
                    // Odotetaan putoamisen alkua
                    if (mc.player.getDeltaMovement().y() < 0) {
                        // Putoaminen alkoi, asetetaan viive (3 tickiä)
                        autoFallDelay = 3;
                        autoWaitingForFall = false; // siirrytään viivetilaan
                        canCritNow = false;
                    } else {
                        canCritNow = false;
                    }
                }
            }
        }

        if (!canCritNow) {
            return; // Smart-tila esti hyökkäyksen
        }

        // ─── VARSINAINEN HYÖKKÄYS ──────────────────────────────────────────
        String currentMode = mode.getMode();

        if (currentMode.equals("Legit")) {
            targetYawForMouse = targetYaw;
            targetPitchForMouse = targetPitch;
            shouldSimulateMouse = true;

            if (isCrosshairOnTarget(currentTarget) && attackController.canAttack(mc.player)) {
                performAttack(currentTarget);
            }
        }
        else if (currentMode.equals("Silent")) {
            float jitter = simulateJitter.get() ? (float) jitterAmount.getValue() : 0f;
            float yaw = Mth.wrapDegrees(targetYaw + (float)((Math.random() - 0.5) * jitter));
            float pitch = Mth.clamp(targetPitch + (float)((Math.random() - 0.5) * jitter * 0.5), -90f, 90f);

            if (attackController.canAttack(mc.player)) {
                Rotations.rotate(yaw, pitch, 10, false, () -> {
                    performAttack(currentTarget);
                });
            } else if (Rotations.getRotationTimer() > 5) {
                Rotations.rotate(yaw, pitch, 5, false, null);
            }
        }
    }

    @Override
    protected void onTick() {}

    private void performAttack(LivingEntity target) {
        if (!isEnabled()) return;
        if (target == null || !target.isAlive()) return;

        if (attackJitter.get()) {
            float jitter = (float) attackJitterAmount.getValue();
            float oldYaw = mc.player.getYRot();
            float oldPitch = mc.player.getXRot();
            mc.player.setYRot(oldYaw + (float)((Math.random() - 0.5) * jitter));
            mc.player.setXRot(oldPitch + (float)((Math.random() - 0.5) * jitter * 0.5));
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
            mc.player.setYRot(oldYaw);
            mc.player.setXRot(oldPitch);
        } else {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(mc.player.getUsedItemHand());
        }
        attackController.recordAttack();
    }


    private void doPacketCrit() {
        if (mc.player == null || mc.getConnection() == null) return;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        sendPosRot(x, y + 0.0625, z, yaw, pitch, false);
        sendPosRot(x, y,         z, yaw, pitch, false);
        sendPosRot(x, y + 1.0E-5, z, yaw, pitch, false);
        sendPosRot(x, y,         z, yaw, pitch, false);
    }

    private void doAutoJumpCrit() {
        if (mc.player == null) return;
        if (autoWaitingForFall || autoFallDelay > 0) return;
        mc.player.jumpFromGround();
        autoWaitingForFall = true;
        autoFallDelay = 0;
    }

    private void sendPosRot(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround, mc.player.horizontalCollision));
    }

    // ─── MUUT METODIT (ennallaan) ──────────────────────────────────────────
    private boolean isCrosshairOnTarget(LivingEntity target) {
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            return entityHit.getEntity() == target;
        }
        return false;
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
        HitResult result = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS || (result instanceof EntityHitResult ehr && ehr.getEntity() == target);
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled() || currentTarget == null) return;
        Renderer3D renderer = event.getRenderer();

        if (renderTargetBox.get()) {
            AABB box = currentTarget.getBoundingBox();
            renderer.boxOutline(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    boxColor.getCurrentColor().getARGB(), 0);
        }

        if (renderPortalEffect.get()) {
            renderPortalEffect(renderer, currentTarget, boxColor.getCurrentColor());
        }
    }

    private void renderPortalEffect(Renderer3D renderer, LivingEntity target, Color baseColor) {
        float td = renderer.getTickDelta();
        double x = Mth.lerp(td, target.xOld, target.getX());
        double y = Mth.lerp(td, target.yOld, target.getY());
        double z = Mth.lerp(td, target.zOld, target.getZ());
        double height = target.getBbHeight() / 2;
        double radius = (target.getBbWidth() / 2.0) * portalRadius.getValue();
        int vertSeg = (int) portalVerticalSegments.getValue();
        int horiSeg = (int) portalHorizontalSegments.getValue();
        Color portalColorCurrent = portalColor.getCurrentColor();
        renderer.drawPortalCylinder(x, y, z, radius, height, portalColorCurrent, vertSeg, horiSeg);
    }

    @Override
    public List<NamedColor> getColors() {
        return List.of(
                new NamedColor("Box Color", boxColor),
                new NamedColor("Portal Color", portalColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        factory.openCustomWindow("killaura_color", "KillAura Color Customizer",
                sw, sh, new ColorCustomizerView(this));
    }

    public AttackController getAttackController() { return this.attackController; }
    public static KillAura getInstance() { return instance; }
    public boolean isSilentModeActive() { return isEnabled() && mode.getMode().equals("Silent"); }
}