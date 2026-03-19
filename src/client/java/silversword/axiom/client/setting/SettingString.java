package silversword.axiom.client.setting;

public class SettingString extends Setting {
    private String value;

    public SettingString(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public String getString() {
        return value;
    }

    @Override
    public void setValue(double value) {

    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public Object getJsonValue() {
        return value;
    }

    @Override
    public void setJsonValue(Object value) {
        if (value instanceof String) {
            this.value = (String) value;
        }
    }

    @Override
    public double getValue() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // Piilotettu asetus, ei renderöidä
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {

    }
}