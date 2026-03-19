package silversword.axiom.client.setting;

public abstract class Setting {
    protected final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // JSON-hallinta
    public abstract String getType();
    public abstract Object getJsonValue();
    public abstract void setJsonValue(Object v);

    // Arvon hallinta (Double-pohjainen yleislogiikka)
    public abstract double getValue();
    public abstract void setValue(double value);

    // GUI-metodit
    public abstract int getHeight();
    public abstract void render(int x, int y, int mouseX, int mouseY);
    public abstract void mouseClicked(double mouseX, double mouseY, int button);
}