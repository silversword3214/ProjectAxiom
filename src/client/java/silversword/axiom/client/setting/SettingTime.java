package silversword.axiom.client.setting;

import silversword.axiom.client.utils.NumberParser;

public class SettingTime extends Setting {
    private double value; // sekunteina
    private final double min;
    private final double max;
    private final double step;

    public SettingTime(String name, double min, double max, double step, double defaultValue) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        setValue(defaultValue);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    @Override
    public double getValue() { return value; }

    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    // Aseta arvo tekstistä, palauttaa true jos onnistui
    public boolean setValueFromString(String text) {
        double val = NumberParser.parseDouble(text, value);
        if (val >= min && val <= max) {
            setValue(val);
            return true;
        }
        return false;
    }

    public String getDisplayValue() {
        double v = value;
        if (v == (int) v) {
            return String.valueOf((int) v);
        } else {
            return String.format("%.2f", v);
        }
    }

    @Override
    public String getType() { return "time"; }

    @Override
    public Object getJsonValue() { return value; }

    @Override
    public void setJsonValue(Object v) {
        if (v instanceof Number n) setValue(n.doubleValue());
        else if (v instanceof String s) setValueFromString(s);
    }

    @Override
    public int getHeight() { return 20; }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // Renderöidään erillisessä rivissä
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {}
}