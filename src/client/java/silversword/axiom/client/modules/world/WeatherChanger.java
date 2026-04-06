package silversword.axiom.client.modules.world;

import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class WeatherChanger extends AxiomMod implements KeybindConfigurable {

    private final SettingMode mode = new SettingMode("Mode", new String[]{"Clear", "Rain", "Thunder"}, "Clear");
    private final SettingNumber rainStrength = new SettingNumber("Rain Strength", 0, 1, 0.1, 1.0);
    private final SettingNumber thunderStrength = new SettingNumber("Thunder Strength", 0, 1, 0.1, 1.0);
    private final SettingBoolean lockWeather = new SettingBoolean("Lock Weather", true);

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public WeatherChanger() {
        super("Weather Changer", "Customize client-side weather", ModuleCategory.WORLD);
        addSetting(mode);
        addSetting(rainStrength);
        addSetting(thunderStrength);
        addSetting(lockWeather);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        if (mc.level != null) {
            applyWeather();
        }
    }

    @Override
    public void onTick() {
        if (mc.level == null) return;
        if (lockWeather.get()) {
            applyWeather();
        }
    }

    private void applyWeather() {
        String m = mode.getMode();
        float rain = 0f;
        float thunder = 0f;

        switch (m) {
            case "Clear":
                rain = 0f;
                thunder = 0f;
                break;
            case "Rain":
                rain = (float) rainStrength.getValue();
                thunder = 0f;
                break;
            case "Thunder":
                rain = (float) rainStrength.getValue();
                thunder = (float) thunderStrength.getValue();
                break;
        }

        mc.level.setRainLevel(rain);
        mc.level.setThunderLevel(thunder);
    }


}