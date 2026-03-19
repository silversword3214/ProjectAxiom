package silversword.axiom.client.setting;

public class SettingKeybind extends Setting {
    private int keyCode;
    private boolean isToggle; // true = togglaa moduulia, false = trigger
    private Runnable onChange;

    public SettingKeybind(String name, int defaultKeyCode) {
        this(name, defaultKeyCode, true); // oletuksena toggle
    }

    public SettingKeybind(String name, int defaultKeyCode, boolean isToggle) {
        super(name);
        this.keyCode = defaultKeyCode;
        this.isToggle = isToggle;
    }

    public boolean isToggle() {
        return isToggle;
    }

    public void setToggle(boolean toggle) {
        if (this.isToggle == toggle) return;
        this.isToggle = toggle;
        if (onChange != null) onChange.run();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public int get() {
        return keyCode;
    }

    public void set(int keyCode) {
        if (this.keyCode == keyCode) return;
        this.keyCode = keyCode;
        if (onChange != null) onChange.run();
    }

    @Override
    public double getValue() {
        return keyCode;
    }

    @Override
    public void setValue(double value) {
        this.keyCode = (int) value;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void render(int x, int y, int mouseX, int mouseY) {
        // UI renderöidään muualla
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        // Ei tehdä mitään
    }

    @Override
    public String getType() {
        return "keybind";
    }

    @Override
    public Object getJsonValue() {
        // Tallennetaan taulukkona: [keyCode, isToggle]
        return new Object[]{keyCode, isToggle};
    }

    @Override
    public void setJsonValue(Object v) {
        if (v instanceof Object[] arr && arr.length == 2) {
            if (arr[0] instanceof Number n) {
                keyCode = n.intValue();
            }
            if (arr[1] instanceof Boolean b) {
                isToggle = b;
            }
        } else if (v instanceof Number n) {
            // Vanha formaatti (vain keyCode) – oletetaan toggleksi
            keyCode = n.intValue();
            isToggle = true;
        } else if (v instanceof String s) {
            try {
                keyCode = Integer.parseInt(s);
                isToggle = true;
            } catch (NumberFormatException ignored) {}
        }
    }
}