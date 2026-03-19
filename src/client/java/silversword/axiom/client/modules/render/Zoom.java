package silversword.axiom.client.modules.render;

import net.minecraft.util.math.MathHelper;
import silversword.axiom.client.event.GetFovEvent;
import silversword.axiom.client.event.MouseScrollEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class Zoom extends AxiomMod implements KeybindConfigurable {

    private final SettingNumber zoom = new SettingNumber("Zoom", 1, 50, 1, 6);
    private final SettingNumber scrollSensitivity = new SettingNumber("Scroll Sensitivity", 0.0, 2.0, 0.1, 1.0);
    private final SettingBoolean smooth = new SettingBoolean("Smooth", true);
    private final SettingBoolean cinematic = new SettingBoolean("Cinematic", false);
    private final SettingBoolean hideHud = new SettingBoolean("Hide HUD", false);
    private final SettingBoolean renderHands = new SettingBoolean("Show Hands", false);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Internal state
    private double targetZoom;
    private double currentZoom;
    private double lastFov;
    private boolean preCinematic;
    private double preMouseSensitivity;
    private double value;

    private double time;

    public Zoom() {
        super("Zoom", "Zooms your view", ModuleCategory.RENDER);
        addSetting(scrollSensitivity);
        addSetting(smooth);
        addSetting(cinematic);
        addSetting(hideHud);

        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) {
            toggle(); // automatically turn off if player not loaded
            return;
        }

        preCinematic = mc.options.smoothCameraEnabled;
        preMouseSensitivity = mc.options.getMouseSensitivity().getValue();
        targetZoom = zoom.getValue();
        currentZoom = 1.0; // start from 1x zoom
        lastFov = mc.options.getFov().getValue();

        if (hideHud.get()) {
            mc.options.hudHidden = true;
        }
    }

    @Override
    protected void onDisable() {
        mc.options.smoothCameraEnabled = preCinematic;
        mc.options.getMouseSensitivity().setValue(preMouseSensitivity);
        if (hideHud.get()) {
            mc.options.hudHidden = false;
        }
        // Force terrain update to reset FOV
        if (mc.worldRenderer != null) {
            mc.worldRenderer.scheduleTerrainUpdate();
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Cinematic mode
        mc.options.smoothCameraEnabled = cinematic.get();

        // Adjust mouse sensitivity when not cinematic
        if (!cinematic.get()) {
            double scaling = getCurrentScaling();
            mc.options.getMouseSensitivity().setValue(preMouseSensitivity / Math.max(scaling * 0.5, 1));
        }
    }

    @AxiomEvent
    private void onRender3D(Render3DEvent event) {
        if (!smooth.get()) {
            currentZoom = isEnabled() ? targetZoom : 1.0;
            return;
        }

        // Smooth transition – constant step per frame
        double step = 0.05;
        if (isEnabled()) {
            currentZoom = Math.min(currentZoom + step, targetZoom);
        } else {
            currentZoom = Math.max(currentZoom - step, 1.0);
        }
    }

    @AxiomEvent
    private void onMouseScroll(MouseScrollEvent event) {
        if (!isEnabled()) return;
        if (scrollSensitivity.getValue() <= 0) return;
        // Älä zoomaa jos ruudulla on jokin näyttö (menu, ClickGUI)
        if (mc.currentScreen != null) return;

        double delta = event.value * 0.25 * scrollSensitivity.getValue() * targetZoom;
        targetZoom += delta;
        targetZoom = MathHelper.clamp(targetZoom, 1, 50);
        event.cancel();
        System.out.println("Mouse scroll, targetZoom=" + targetZoom);
    }

    @AxiomEvent
    private void onGetFov(GetFovEvent event) {
        if (!isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        // Älä zoomaa jos ruudulla on jokin näyttö (menu, ClickGUI)
        if (mc.currentScreen != null) return;

        double scaling = getCurrentScaling();
        event.fov /= scaling;

        if (lastFov != event.fov && mc.worldRenderer != null) {
            mc.worldRenderer.scheduleTerrainUpdate();
            lastFov = event.fov;
        }
    }

    public double getScaling() {
        double delta = time < 0.5 ? 4 * time * time * time : 1 - Math.pow(-2 * time + 2, 3) / 2; // Ease in out cubic
        return MathHelper.lerp(delta, 1, value);
    }

    private double getCurrentScaling() {
        return currentZoom;
    }

    public boolean renderHands() {
        return !isEnabled() || renderHands.get();
    }
}