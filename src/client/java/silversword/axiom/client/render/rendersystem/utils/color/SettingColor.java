package silversword.axiom.client.render.rendersystem.utils.color;

import net.minecraft.nbt.CompoundTag;
import silversword.axiom.client.setting.Setting;

import java.util.List;

public class SettingColor extends Color {
    public boolean rainbow;
    public float speed;
    private final Setting internalSetting;
    private String name;

    public SettingColor(String name, Color defaultColor) {
        super(defaultColor);
        this.rainbow = false;
        this.speed = 1.0f;


        this.internalSetting = new Setting(name) {
            private boolean hidden = true;
            @Override
            public double getValue() { return 0; }

            @Override
            public void setValue(double value) {}

            @Override
            public int getHeight() {
                return hidden ? 0 : 14;
            }

            @Override
            public void render(int x, int y, int mouseX, int mouseY) {
                if (hidden) return;

            }

            @Override
            public void mouseClicked(double mouseX, double mouseY, int button) {}

            @Override
            public String getType() { return "color"; }

            @Override
            public Object getJsonValue() {
                return new Object[]{
                        r, g, b, a,
                        rainbow ? 1 : 0,
                        speed
                };
            }

            @Override
            public void setJsonValue(Object v) {
                if (v instanceof int[] data && data.length >= 4) {
                    set(data[0], data[1], data[2], data[3]);
                    if (data.length >= 5) rainbow = data[4] == 1;
                    if (data.length >= 6) speed = data[5];
                } else if (v instanceof List<?> list && list.size() >= 4) {
                    try {
                        int r = ((Number) list.get(0)).intValue();
                        int g = ((Number) list.get(1)).intValue();
                        int b = ((Number) list.get(2)).intValue();
                        int a = ((Number) list.get(3)).intValue();
                        set(r, g, b, a);
                        if (list.size() >= 5) {
                            int rainbowValue = ((Number) list.get(4)).intValue();
                            rainbow = rainbowValue == 1;
                        }
                        if (list.size() >= 6) {
                            speed = ((Number) list.get(5)).floatValue();
                        }
                    } catch (Exception ignored) {}
                } else if (v instanceof Object[] arr) {
                    try {
                        int r = ((Number) arr[0]).intValue();
                        int g = ((Number) arr[1]).intValue();
                        int b = ((Number) arr[2]).intValue();
                        int a = ((Number) arr[3]).intValue();
                        set(r, g, b, a);
                        if (arr.length > 4) {
                            int rainbowValue = ((Number) arr[4]).intValue();
                            rainbow = rainbowValue == 1;
                        }
                        if (arr.length > 5) {
                            speed = ((Number) arr[5]).floatValue();
                        }
                    } catch (Exception ignored) {}
                }
            }
        };
    }

    public Setting getSetting() {
        return internalSetting;
    }

    public Color getCurrentColor() {
        if (rainbow) {
            float hue = (System.currentTimeMillis() % (int)(5000 / speed)) / (5000f / speed);
            int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, this.a);
        }
        return this;
    }

    @Override
    public SettingColor copy() {
        SettingColor c = new SettingColor(name + "_copy", new Color(r, g, b, a));
        c.rainbow = this.rainbow;
        c.speed = this.speed;
        return c;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();
        tag.putBoolean("rainbow", rainbow);
        tag.putFloat("speed", speed);
        return tag;
    }

    @Override
    public Color fromTag(CompoundTag tag) {
        super.fromTag(tag);
        this.rainbow = tag.getBoolean("rainbow").orElse(false);
        this.speed = tag.getFloat("speed").orElse(1.0f);
        return this;
    }
}