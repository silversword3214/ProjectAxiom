package silversword.axiom.client.modules.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;

public class Zoom extends AxiomMod {

    public final SettingNumber  zoomLevel;
    public final SettingBoolean smooth;
    public final SettingNumber  smoothSpeed;
    public final SettingBoolean reduceSensitivity;
    public final SettingNumber  sensitivityFactor;
    public final SettingBoolean scrollZoom;
    public final SettingNumber  scrollStep;
    public final SettingBoolean resetScrollOnRelease;
    public final SettingNumber  zoomKey;

    private boolean zooming = false;
    private double  savedSensitivity = -1.0;

    public Zoom() {
        super("Zoom", "Hold key to zoom in (default: Z)", ModuleCategory.RENDER);

        zoomLevel = new SettingNumber("Zoom Level", 1.0, 20.0, 0.5, 4.0);
        smooth = new SettingBoolean("Smooth", true);
        smoothSpeed = new SettingNumber("Smooth Speed", 1.0, 30.0, 1.0, 15.0);
        reduceSensitivity = new SettingBoolean("Reduce Sensitivity", true);
        sensitivityFactor = new SettingNumber("Sensitivity", 0.05, 1.0, 0.05, 0.5);
        scrollZoom = new SettingBoolean("Scroll to Zoom", true);
        scrollStep = new SettingNumber("Scroll Step", 0.1, 2.0, 0.1, 0.5);
        resetScrollOnRelease = new SettingBoolean("Reset Scroll on Release", true);
        zoomKey = new SettingNumber("Zoom Key", 0, 400, 1, InputConstants.KEY_Z);

        addSetting(zoomLevel);
        addSetting(smooth);
        addSetting(smoothSpeed);
        addSetting(reduceSensitivity);
        addSetting(sensitivityFactor);
        addSetting(scrollZoom);
        addSetting(scrollStep);
        addSetting(resetScrollOnRelease);
        addHiddenSetting(zoomKey);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.getWindow() == null) {
            if (zooming) stopZooming(mc);
            return;
        }
        if (mc.gui.screen() != null) {
            if (zooming) stopZooming(mc);
            return;
        }

        int keyCode = (int) zoomKey.getValue();
        boolean pressed = keyCode > 0 && InputConstants.isKeyDown(keyCode);

        if (pressed && !zooming) startZooming(mc);
        else if (!pressed && zooming) stopZooming(mc);

        if (zooming) {
            ZoomState.targetMultiplier = zoomLevel.getValue() * ZoomState.scrollZoom;
        } else {
            ZoomState.targetMultiplier = 1.0;
        }

        ZoomState.smoothCamera      = smooth.get();
        ZoomState.smoothSpeed       = smoothSpeed.getValue();
        ZoomState.reduceSensitivity = reduceSensitivity.get();
        ZoomState.sensitivityFactor = sensitivityFactor.getValue();
    }

    private void startZooming(Minecraft mc) {
        zooming = true;
        ZoomState.zooming = true;
        ZoomState.scrollZoom = 1.0;

        if (reduceSensitivity.get() && mc.options != null) {
            savedSensitivity = mc.options.sensitivity().get();
            double newSens = Mth.clamp(
                    savedSensitivity * sensitivityFactor.getValue(),
                    0.0, 1.0);
            mc.options.sensitivity().set(newSens);
        }
    }

    private void stopZooming(Minecraft mc) {
        zooming = false;
        ZoomState.zooming = false;

        if (savedSensitivity >= 0 && mc != null && mc.options != null) {
            mc.options.sensitivity().set(savedSensitivity);
            savedSensitivity = -1.0;
        }
        if (resetScrollOnRelease.get()) {
            ZoomState.scrollZoom = 1.0;
        }
    }

    public void onScroll(double delta) {
        if (!zooming) return;
        if (!scrollZoom.get()) return;
        double step = scrollStep.getValue();
        double newScroll = ZoomState.scrollZoom + delta * step;
        ZoomState.scrollZoom = Mth.clamp(newScroll, 0.1, 20.0);
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
    @Override protected void onTick()    {}

    public boolean isZooming() { return zooming; }
}