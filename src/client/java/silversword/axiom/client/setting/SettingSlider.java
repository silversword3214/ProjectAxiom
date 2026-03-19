package silversword.axiom.client.setting;

public class SettingSlider extends Setting {

    private final double[] presets;
    private int index;

    public SettingSlider(String name, double[] presets, double defaultValue) {
        super(name);
        this.presets = presets;
        this.index = findClosestIndex(defaultValue);
    }

    private int findClosestIndex(double value) {
        int closest = 0;
        double diff = Double.MAX_VALUE;

        for (int i = 0; i < presets.length; i++) {
            double d = Math.abs(presets[i] - value);
            if (d < diff) {
                diff = d;
                closest = i;
            }
        }
        return closest;
    }

    /**
     * Palauttaa valitun arvon siistinä merkkijonona.
     * Jos luku on kokonaisluku, poistaa turhat nollat (esim. 10.0 -> "10").
     */
    public String getDisplayValue() {
        double v = getValue();
        if (v == (int) v) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    public double[] getPresets() {
        return presets;
    }

    // --- Setting-kantaluokan metodien toteutus ---

    @Override
    public double getValue() {
        return presets[index];
    }

    @Override
    public void setValue(double value) {
        this.index = findClosestIndex(value);
    }

    @Override
    public String getType() {
        return "slider";
    }

    @Override
    public Object getJsonValue() {
        return getValue(); // Tallennetaan valittu numero
    }

    @Override
    public void setJsonValue(Object v) {
        if (v instanceof Number n) {
            setValue(n.doubleValue());
        }
    }

    // --- GUI-metodit ---

    public void next() {
        index = (index + 1) % presets.length;
    }

    public void previous() {
        index = (index - 1 + presets.length) % presets.length;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // Renderöinti hoidetaan yleensä GUI-luokassa, joka hyödyntää getDisplayValue() metodia
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) next();
        if (button == 1) previous();
    }
}