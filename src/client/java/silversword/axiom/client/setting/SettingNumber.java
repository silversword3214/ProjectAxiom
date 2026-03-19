package silversword.axiom.client.setting;

public class SettingNumber extends Setting {

    private final double min;
    private final double max;
    private final double step;
    private double value;

    public SettingNumber(String name, double min, double max, double step, double defaultValue) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step <= 0 ? 1.0 : step;
        setValue(defaultValue);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public void setValue(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double snapped = Math.round(clamped / step) * step;
        this.value = Math.max(min, Math.min(max, snapped));
    }

    public String getDisplayValue() {
        double v = getValue();
        return (v == (int) v) ? String.valueOf((int) v) : String.format("%.2f", v);
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
        // jos haluat: LMB +step, RMB -step
        if (button == 0) setValue(getValue() + step);
        if (button == 1) setValue(getValue() - step);
    }

    // ---- JSON ----

    public String getType() {
        return "number";
    }

    @Override
    public Object getJsonValue() {
        return getValue();
    }

    @Override
    public void setJsonValue(Object value) {
        if (value instanceof Number n) {
            setValue(n.doubleValue());
            return;
        }
        if (value instanceof String s) {
            try {
                setValue(Double.parseDouble(s));
            } catch (NumberFormatException ignored) { }
        }
    }
}
