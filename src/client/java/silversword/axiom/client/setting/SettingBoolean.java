package silversword.axiom.client.setting;

public class SettingBoolean extends Setting {

    private boolean value;

    public SettingBoolean(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }
    private Runnable onChange;

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        if (this.value == value) return;

        this.value = value;

        if (onChange != null) {
            onChange.run();
        }
    }

    public void toggle() {
        set(!this.value);
    }

    @Override
    public double getValue() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public void setValue(double value) {
        this.value = value >= 0.5;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // UI renderöidään SettingRow/ModuleSettingsView:ssä
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) toggle();
    }

    // ---- JSON ----
    @Override
    public String getType() {
        return "boolean";
    }

    @Override
    public Object getJsonValue() {
        return get();
    }

    @Override
    public void setJsonValue(Object v) {
        if (v instanceof Boolean b) {
            set(b);
            return;
        }
        if (v instanceof Number n) {
            set(n.doubleValue() >= 0.5);
            return;
        }
        if (v instanceof String s) {
            if ("true".equalsIgnoreCase(s)) set(true);
            else if ("false".equalsIgnoreCase(s)) set(false);
            else {
                try {
                    set(Double.parseDouble(s) >= 0.5);
                } catch (NumberFormatException ignored) { }
            }
        }
    }
}
