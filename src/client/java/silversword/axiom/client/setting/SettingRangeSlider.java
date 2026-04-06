package silversword.axiom.client.setting;

public class SettingRangeSlider extends Setting {
    private double minVal, maxVal;
    private final double rangeMin, rangeMax, step;

    public SettingRangeSlider(String name, double minVal, double maxVal, double rangeMin, double rangeMax, double step) {
        super(name);
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.step = step;
    }

    public double getMin() { return minVal; }
    public double getMax() { return maxVal; }
    public void setMin(double v) { this.minVal = v; }
    public void setMax(double v) { this.maxVal = v; }

    @Override public String getType() { return "range"; }
    @Override public Object getJsonValue() { return minVal + "-" + maxVal; }
    @Override public void setJsonValue(Object v) { /* JSON logiikka tähän */ }
    @Override public double getValue() { return minVal; } // Fallback
    @Override public void setValue(double value) { this.minVal = value; }

    // GUI placeholderit
    @Override public int getHeight() { return 26; }
    @Override public void render(int x, int y, int mouseX, int mouseY) {}
    @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
}