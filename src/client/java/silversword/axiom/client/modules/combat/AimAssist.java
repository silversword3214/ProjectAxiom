package silversword.axiom.client.modules.combat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import silversword.axiom.client.event.mouse.MouseUpdateEvent;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.moduleutils.killaura.TargetManager;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AimAssist extends AxiomMod implements KeybindConfigurable {

    private final TargetManager targetManager = new TargetManager();
    private LivingEntity currentTarget = null;

    private float targetYawForMouse, targetPitchForMouse;
    private boolean shouldSimulateMouse = false;

    // ---------- Settings ----------
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    public final SettingMode targetMode = new SettingMode("Target", new String[]{"Players", "Mobs", "Both"}, "Both");
    public final SettingSlider range = new SettingSlider("Range", new double[]{3.0, 4.0, 5.0, 6.0}, 5.0);
    public final SettingMode bodyPart = new SettingMode("Body Part", new String[]{"Head", "Body", "Feet"}, "Body");

    public final SettingSlider maxTurnSpeed = new SettingSlider("Max Turn Speed (deg/tick)", new double[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 80}, 40.0);

    public final SettingBoolean onlyOnHold = new SettingBoolean("Only on Hold", true);
    public final SettingKeybind holdKey = new SettingKeybind("Hold Key", 0);
    public final SettingBoolean manualOverride = new SettingBoolean("Manual Override", false);
    public final SettingBoolean checkWalls = new SettingBoolean("Check Walls", true);
    public final SettingBoolean ignoreBots = new SettingBoolean("Ignore Bots", true);

    public AimAssist() {
        super("Aim Assist", "Get better aiming", ModuleCategory.COMBAT);
        addSetting(targetMode);
        addSetting(range);
        addSetting(bodyPart);
        addSetting(maxTurnSpeed);
        addSetting(onlyOnHold);
        addSetting(holdKey);
        addSetting(manualOverride);
        addSetting(checkWalls);
        addSetting(ignoreBots);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        currentTarget = null;
        shouldSimulateMouse = false;
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        shouldSimulateMouse = false;
    }

    @Override
    protected void onTick() {

    }

    @Subscribe
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (!isEnabled() || !shouldSimulateMouse) return;

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDiff = Mth.wrapDegrees(targetYawForMouse - currentYaw);
        float pitchDiff = targetPitchForMouse - currentPitch;

        if (Math.abs(yawDiff) < 0.01f && Math.abs(pitchDiff) < 0.01f) {
            shouldSimulateMouse = false;
            return;
        }

        float maxSpeed = (float) maxTurnSpeed.getValue();
        float yawStep = Mth.clamp(yawDiff, -maxSpeed, maxSpeed);
        float pitchStep = Mth.clamp(pitchDiff, -maxSpeed, maxSpeed);

        double sens = mc.options.sensitivity().get();
        double deltaX = yawStep * 0.6 * sens;
        double deltaY = pitchStep * 0.6 * sens;

        event.setDeltaX(event.getDeltaX() + deltaX);
        event.setDeltaY(event.getDeltaY() + deltaY);
    }

    @Subscribe
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // Manual override ohittaa hold-näppäimen
        boolean override = manualOverride.get();
        if (!override && onlyOnHold.get() && holdKey.get() != 0) {
            int key = holdKey.get();
            boolean isMouse = key < 8;
            boolean pressed = isMouse
                    ? isMouseButtonPressed(key)
                    : InputConstants.isKeyDown(key);

            if (!pressed) {
                currentTarget = null;
                shouldSimulateMouse = false;
                return;
            }
        }

        // Etsi kohde
        currentTarget = targetManager.selectTarget(
                mc.player, mc.level,
                "Distance", ignoreBots.get(), targetMode.getMode()
        );
        if (currentTarget == null) {
            shouldSimulateMouse = false;
            return;
        }

        if (mc.player.distanceTo(currentTarget) > range.getValue()) {
            shouldSimulateMouse = false;
            return;
        }

        if (checkWalls.get() && !mc.player.hasLineOfSight(currentTarget)) {
            shouldSimulateMouse = false;
            return;
        }

        // Kulmat kohteeseen
        float targetYaw = (float) getYawToTarget(currentTarget);
        float targetPitch = (float) getPitchToTarget(currentTarget);
        targetYaw = Mth.wrapDegrees(targetYaw);
        targetPitch = Mth.clamp(targetPitch, -90f, 90f);

        targetYawForMouse = targetYaw;
        targetPitchForMouse = targetPitch;
        shouldSimulateMouse = true;
    }

    /** Hiiren napin tila MouseHandler:in kautta (26.3: ei enää glfwGetMouseButton). */
    private static boolean isMouseButtonPressed(int button) {
        var mh = mc.mouseHandler;
        return switch (button) {
            case 0 -> mh.isLeftPressed();     // GLFW_MOUSE_BUTTON_1
            case 1 -> mh.isRightPressed();    // GLFW_MOUSE_BUTTON_2
            case 2 -> mh.isMiddlePressed();   // GLFW_MOUSE_BUTTON_3
            default -> false;                 // sivunapit: ei suoraa julkista API:a
        };
    }

    // Apumetodit
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
}