package silversword.axiom.client.modules;

import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

// Apuluokka värin nimeämiseen
public class NamedColor {
    private final String name;
    private final SettingColor color;

    public NamedColor(String name, SettingColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public SettingColor getColor() { return color; }
}
