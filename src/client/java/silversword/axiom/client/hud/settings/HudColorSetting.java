package silversword.axiom.client.hud.settings;

import silversword.axiom.client.setting.Setting;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

public class HudColorSetting extends Setting {
    private final SettingColor color;

    public HudColorSetting(String name, SettingColor color) {
        super(name);
        this.color = color;
    }

    public SettingColor getColor() {
        return color;
    }

    // Setting-rajapinnan metodit – delegoidaan color-oliolle sopivasti

    @Override
    public double getValue() {
        // Ei mielekästä arvoa, palautetaan 0
        return 0;
    }

    @Override
    public void setValue(double value) {
        // Ei tehdä mitään
    }

    @Override
    public String getType() {
        return "hudcolor";
    }

    @Override
    public Object getJsonValue() {
        // Käytetään SettingColorin omaa JSON-serialisointia
        return color.getSetting().getJsonValue();
    }

    @Override
    public void setJsonValue(Object v) {
        color.getSetting().setJsonValue(v);
    }

    @Override
    public int getHeight() {
        return 24; // sama kuin LabeledColorPickerin preferredHeight
    }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // Ei renderöidä suoraan
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        // Ei käsitellä
    }
}