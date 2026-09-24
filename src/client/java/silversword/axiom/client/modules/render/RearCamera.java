package silversword.axiom.client.modules.render;

import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.components.RearCameraHud;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera.RearCameraRenderer;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;

public final class RearCamera extends AxiomMod {

    private final SettingBoolean showHud;
    private final SettingNumber  viewW;
    private final SettingNumber  viewH;
    private final SettingSlider  updateHz;
    private final SettingBoolean invertPitch;

    public RearCamera() {
        super("RearCamera",
                "Renders a rear-view camera and displays it on the HUD",
                ModuleCategory.RENDER);

        showHud = new SettingBoolean("Show HUD Element", true);
        viewW   = new SettingNumber("Viewport Width",  64, 640, 16, 200);
        viewH   = new SettingNumber("Viewport Height", 36, 360,  9, 120);

        updateHz = new SettingSlider("Update Rate (Hz)",
                new double[]{5, 10, 15, 20, 30, 60}, 20);

        invertPitch = new SettingBoolean("Invert Pitch", true);

        addSetting(showHud);
        addSetting(viewW);
        addSetting(viewH);
        addSetting(updateHz);
        addSetting(invertPitch);
    }

    @Override
    protected void onEnable() {
        RearCameraRenderer.init();
        RearCameraRenderer.setEnabled(true);
        applyHudState(true);
        applyRendererSettings();
    }

    @Override
    protected void onDisable() {
        RearCameraRenderer.setEnabled(false);
        applyHudState(false);
    }

    @Override
    protected void onTick() {
        applyHudState(showHud.get());
        applyRendererSettings();
    }

    private void applyHudState(boolean on) {
        RearCameraHud hud = findHud();
        if (hud == null) return;
        hud.setEnabled(on);
        hud.setViewportSize((int) viewW.getValue(), (int) viewH.getValue());
    }

    private void applyRendererSettings() {
        RearCameraRenderer.setUpdateHz((int) updateHz.getValue());
        RearCameraRenderer.setInvertPitch(invertPitch.get());
    }

    private RearCameraHud findHud() {
        for (HudElement e : HudManager.get().elements()) {
            if (e instanceof RearCameraHud hud) return hud;
        }
        return null;
    }
}