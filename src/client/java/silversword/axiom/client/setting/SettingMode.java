package silversword.axiom.client.setting;

import java.util.Arrays;
import java.util.List;

public class SettingMode extends Setting {

    private final List<String> modes;
    private int index;

    public SettingMode(String name, String[] modes, String defaultMode) {
        super(name);
        this.modes = Arrays.asList(modes);
        int idx = this.modes.indexOf(defaultMode);
        this.index = Math.max(0, idx);
        if (this.index >= this.modes.size()) this.index = 0;
    }

    public List<String> getModes() {
        return modes;
    }

    public void setMode(String mode) {
        int next = modes.indexOf(mode);

        if (next >= 0) {
            index = next;

        } else {
            System.out.println(">>> SettingMode.setMode: mode not found in list!");
        }
    }

    public String getMode() {
        String m = modes.get(index);

        return m;
    }

    public void next() {
        index = (index + 1) % modes.size();
    }

    public void previous() {
        index = (index - 1 + modes.size()) % modes.size();
    }

    @Override
    public double getValue() {
        return index;
    }

    @Override
    public void setValue(double value) {
        int next = (int) Math.round(value);
        if (next < 0) index = 0;
        else if (next >= modes.size()) index = modes.size() - 1;
        else index = next;
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
        if (button == 0) next();
        if (button == 1) previous();
    }

    // ---- JSON ----
    @Override
    public String getType() {
        return "mode";
    }

    @Override
    public Object getJsonValue() {
        return getMode(); // ✅ EI getModeName
    }

    @Override
    public void setJsonValue(Object v) {
        if (v instanceof String s) {
            setMode(s);   // ✅ EI setModeByName
            return;
        }
        if (v instanceof Number n) {
            setValue(n.doubleValue()); // tukee myös indexillä
        }
    }
}
