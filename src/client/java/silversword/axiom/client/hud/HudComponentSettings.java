package silversword.axiom.client.hud;

import silversword.axiom.client.setting.Setting;
import silversword.axiom.client.modules.NamedColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HudComponentSettings {
    private final List<Setting> settings = new ArrayList<>();
    private final List<NamedColor> namedColors = new ArrayList<>();

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public List<NamedColor> getNamedColors() {
        return Collections.unmodifiableList(namedColors);
    }

    public void addSetting(Setting setting) {
        settings.add(setting);
    }

    public void addNamedColor(NamedColor namedColor) {
        namedColors.add(namedColor);
    }
}