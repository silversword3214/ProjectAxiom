package silversword.axiom.client.modules.render;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingNumber;

public class CameraDistance extends AxiomMod {

    private final SettingNumber distance = new SettingNumber("Distance", 0.5, 150.0, 0.1, 4.0);

    public CameraDistance() {
        super("Camera Distance", "Adjusts the third-person camera distance.", ModuleCategory.RENDER);
        addSetting(distance);
    }

    @Override
    protected void onEnable() {
        // Ei pakoteta perspektiiviä
    }

    @Override
    protected void onDisable() {
        // Ei tarvetta toimenpiteille
    }

    @Override
    protected void onTick() {

    }

    public double getDistance() {
        return distance.getValue();
    }
}