package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.setting.*;

import static java.lang.Math.*;

abstract class SettingRowBase implements UiComponent {
    protected Rect bounds = new Rect(0, 0, 10, 10);

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect r) { bounds = r; }

    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) { }
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }

    protected int rowH() { return 26; }
    @Override public int getPreferredHeight() { return rowH(); }

    protected static double clamp01(double t) { return max(0.0, min(1.0, t)); }

    protected static float smoothToward(float current, float target, float delta) {
        float k = 1.0f - (float) pow(0.001, delta);
        return current + (target - current) * k;
    }

    protected static String fmt(double v) {
        if (v == (int) v) return String.valueOf((int) v);
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    // Yhteinen tekstin pystysuuntainen keskitys
    protected int getTextY(UiContext ui) {
        return bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
    }
}

final class SettingFallbackRow extends SettingRowBase {
    private final Setting s;
    SettingFallbackRow(Setting s) { this.s = s; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        // No background for the row

        String name = s.getName() == null ? "" : s.getName();
        int textY = getTextY(ui);
        int nameColor = hover ? ui.theme.accent : ui.theme.text;
        ui.text(name, bounds.x + 8, textY, nameColor);

        String v = fmt(s.getValue());
        int vw = ui.textWidth(v);
        ui.text(v, bounds.right() - vw - 8, textY, ui.theme.textDim);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        s.mouseClicked(mouseX, mouseY, button);
        return true;
    }
}

final class SettingTimeRow extends SettingRowBase {
    private final SettingTime s;
    private boolean dragging = false;

    SettingTimeRow(SettingTime s) {
        this.s = s;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);

        int textY = getTextY(ui);
        ui.text(s.getName(), bounds.x + 8, textY, ui.theme.text);

        // Liukusäädin
        int sliderX = bounds.x + 100;
        int sliderY = bounds.y + 4;
        int sliderW = bounds.w - 108;
        int sliderH = bounds.h - 8;

        if (sliderW > 0) {
            double min = s.getMin();
            double max = s.getMax();
            double val = s.getValue();
            double t = (val - min) / (max - min);
            int knobX = sliderX + (int) (t * sliderW);

            ui.fill(sliderX, sliderY, sliderW, sliderH, ui.theme.sliderTrack);
            ui.fill(sliderX, sliderY, knobX - sliderX, sliderH, ui.theme.accent);
            ui.fill(knobX - 2, sliderY - 1, 4, sliderH + 2, ui.theme.text);
        }

        // Muotoiltu arvo
        String valueStr = s.getDisplayValue();
        int valueWidth = ui.textWidth(valueStr);
        ui.text(valueStr, bounds.right() - valueWidth - 8, textY, ui.theme.textDim);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0 && bounds.contains(mouseX, mouseY)) {
            dragging = true;
            updateValue(ui, mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            updateValue(ui, mouseX);
            return true;
        }
        return false;
    }

    private void updateValue(UiContext ui, double mouseX) {
        int sliderX = bounds.x + 100;
        int sliderW = bounds.w - 108;
        if (sliderW <= 0) return;
        double t = (mouseX - sliderX) / sliderW;
        t = Math.max(0, Math.min(1, t));
        double min = s.getMin();
        double max = s.getMax();
        double val = min + t * (max - min);
        double step = s.getStep();
        val = Math.round(val / step) * step;
        s.setValue(val);
    }
}

final class SettingTimeFieldRow extends SettingRowBase {
    private final SettingTime setting;
    private final TextField textField;
    private boolean editing = false;
    private String draft = "";

    SettingTimeFieldRow(SettingTime setting) {
        this.setting = setting;
        this.textField = new TextField();
        this.draft = setting.getDisplayValue();
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        int fieldX = bounds.right() - 80;
        int fieldY = bounds.y + (bounds.h - 20) / 2;
        textField.setBounds(new Rect(fieldX, fieldY, 72, 20));
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);

        int textY = getTextY(ui);
        ui.text(setting.getName(), bounds.x + 8, textY, ui.theme.text);

        // Jos muokataan, näytä tekstikenttä
        if (editing) {
            textField.render(ui, mouseX, mouseY, delta);
        } else {
            // Näytä arvo tekstinä
            String valueStr = setting.getDisplayValue() + " s";
            int valueWidth = ui.textWidth(valueStr);
            ui.text(valueStr, bounds.right() - valueWidth - 8, textY, ui.theme.textDim);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (bounds.contains(mouseX, mouseY)) {
                if (!editing) {
                    editing = true;
                    draft = setting.getDisplayValue();
                    textField.setText(draft);
                }
                return textField.mouseClicked(ui, mouseX, mouseY, button);
            } else if (editing) {
                // Klikkaus rivin ulkopuolelle -> tallenna
                applyEdit();
                editing = false;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (!editing) return false;
        if (keyCode == 257 || keyCode == 335) { // Enter
            applyEdit();
            editing = false;
            return true;
        }
        if (keyCode == 259) { // Backspace
            String t = textField.getText();
            if (t.length() > 0) {
                textField.setText(t.substring(0, t.length() - 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!editing) return false;
        return textField.charTyped(ui, chr, modifiers);
    }

    private void applyEdit() {
        String newText = textField.getText();
        if (setting.setValueFromString(newText)) {
            // ok
        } else {
            // virheellinen syöte, palauta vanha
        }
        draft = setting.getDisplayValue();
    }
}

final class SettingToggleRow extends SettingRowBase {
    private final Toggle toggle;

    SettingToggleRow(SettingBoolean s) {
        this.toggle = new Toggle(
                s.getName(),
                s::get,
                b -> {
                    if (b != s.get()) s.toggle();
                }
        );
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        toggle.setBounds(bounds);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        toggle.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return toggle.mouseClicked(ui, mouseX, mouseY, button);
    }

    // Muut tapahtumat voi delegoida tarvittaessa, mutta Toggle ei niitä käytä,
    // joten ne voidaan jättää oletukseksi (SettingRowBase hoitaa ne)
}

final class SettingModeRow extends SettingRowBase {
    private final SettingMode s;
    // private ModeDropdown activeDropdown = null; // Poistetaan, käytetään staattista muuttujaa

    SettingModeRow(SettingMode s) { this.s = s; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        // No background for the row

        int textY = getTextY(ui);
        int labelColor = hover ? ui.theme.accent : ui.theme.text;
        ui.text(s.getName(), bounds.x + 8, textY, labelColor);

        String mode = s.getMode();
        int modeWidth = ui.textWidth(mode) + 16;
        int pillerX = bounds.right() - modeWidth - 8;
        int pillerY = bounds.y + (bounds.h - 18) / 2;
        Rect pillerRect = new Rect(pillerX, pillerY, modeWidth, 18);

        boolean pillerHover = pillerRect.contains(mouseX, mouseY);
        ui.fill(pillerRect, pillerHover ? ui.theme.buttonHover : ui.theme.button);

        int modeTextY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(mode, pillerRect.x + 6, modeTextY, ui.theme.text);

        String arrow = ">";
        int arrowX = pillerRect.right() - ui.textWidth(arrow) - 6;
        ui.text(arrow, arrowX, modeTextY, ui.theme.textDim);

        if (ClickGuiScreen.currentDropdown != null) {
            ClickGuiScreen.currentDropdown.render(ui, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        // Jos dropdown on auki, käsitellään se ensin
        if (ClickGuiScreen.currentDropdown != null) {
            // Jos klikkaus on dropdownin sisällä, välitä sille
            if (ClickGuiScreen.currentDropdown.getBounds().contains(mouseX, mouseY)) {
                return ClickGuiScreen.currentDropdown.mouseClicked(ui, mouseX, mouseY, button);
            } else {
                // Klikkaus dropdownin ulkopuolella -> sulje se
                ClickGuiScreen.currentDropdown = null;
                return true;
            }
        }

        // Jos dropdown ei ole auki, tarkista klikataanko tätä riviä
        if (!bounds.contains(mouseX, mouseY)) return false;

        if (button == 0) {
            // Laske dropdownin koko
            int maxModeWidth = 0;
            for (String m : s.getModes()) {
                int w = ui.textWidth(m);
                if (w > maxModeWidth) maxModeWidth = w;
            }
            int dropdownW = maxModeWidth + 16;

            int count = s.getModes().size();
            int itemHeight = 16;
            int gap = 2;
            int padding = 4;
            int dropdownH = count * itemHeight + (count - 1) * gap + padding;

            int dropdownX = bounds.right() + 13;
            int dropdownY = bounds.y;

            int screenWidth = ui.mc.getWindow().getGuiScaledWidth();
            int screenHeight = ui.mc.getWindow().getGuiScaledHeight();

            if (dropdownX + dropdownW > screenWidth) {
                dropdownX = bounds.x - dropdownW - 5;
            }
            dropdownX = Math.max(0, Math.min(screenWidth - dropdownW, dropdownX));

            if (dropdownY + dropdownH > screenHeight) {
                dropdownY = bounds.y + bounds.h - dropdownH;
            }
            dropdownY = Math.max(0, Math.min(screenHeight - dropdownH, dropdownY));

            Rect dropdownBounds = new Rect(dropdownX, dropdownY, dropdownW, dropdownH);

            ModeDropdown dropdown = new ModeDropdown(
                    s.getModes().toArray(new String[0]),
                    s.getMode(),
                    selectedMode -> {
                        s.setMode(selectedMode);
                        ClickGuiScreen.currentDropdown = null;
                    }
            );

            dropdown.setBounds(dropdownBounds);
            ClickGuiScreen.currentDropdown = dropdown;
            return true;
        } else if (button == 1) {
            s.previous();
            return true;
        }
        return false;
    }

    // Muut tapahtumienkäsittelijät (mouseReleased, mouseDragged, jne.) pitää myös delegoida dropdownille
    // mutta koska käytetään staattista muuttujaa, niiden täytyy tarkistaa se.
    // Tässä ne toteutetaan samalla tavalla kuin ennen, mutta käyttäen staattista muuttujaa.

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        if (ClickGuiScreen.currentDropdown != null) {
            ClickGuiScreen.currentDropdown.mouseReleased(ui, mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (ClickGuiScreen.currentDropdown != null) {
            return ClickGuiScreen.currentDropdown.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (ClickGuiScreen.currentDropdown != null && ClickGuiScreen.currentDropdown.getBounds().contains(mouseX, mouseY)) {
            return ClickGuiScreen.currentDropdown.mouseScrolled(ui, mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (ClickGuiScreen.currentDropdown != null) {
            return ClickGuiScreen.currentDropdown.keyPressed(ui, keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (ClickGuiScreen.currentDropdown != null) {
            return ClickGuiScreen.currentDropdown.charTyped(ui, chr, modifiers);
        }
        return false;
    }
}

final class SettingNumberSliderRow extends SettingRowBase {
    private final Slider slider;

    SettingNumberSliderRow(SettingNumber s) {
        this.slider = new Slider(
                s.getName(),
                s.getMin(),
                s.getMax(),
                0.0, // jatkuva liukusäädin
                s::getValue,
                s::setValue
        );
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        slider.setBounds(bounds);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        slider.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return slider.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        slider.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return slider.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }

}

final class SettingPresetSliderRow extends SettingRowBase {
    private final SettingSlider s;
    private final Slider slider;
    private final double[] presets;

    SettingPresetSliderRow(SettingSlider s) {
        this.s = s;
        this.presets = s.getPresets();
        int n = presets.length;

        // Luodaan slider, joka liikkuu indeksien 0..n-1 välillä askeleella 1
        this.slider = new Slider(
                s.getName(),
                0,
                n - 1,
                1.0,
                // Getteri palauttaa nykyistä preset-arvoa vastaavan indeksin
                () -> {
                    double val = s.getValue();
                    return findClosestIndex(presets, val);
                },
                // Setteri asettaa preset-arvon indeksin perusteella
                (idxDouble) -> {
                    int idx = (int) Math.round(idxDouble);
                    if (idx >= 0 && idx < n) {
                        s.setValue(presets[idx]);
                    }
                },
                // Muotoilija: näytetään preset-arvo eikä indeksi
                idx -> s.getDisplayValue()
        );
    }

    private static int findClosestIndex(double[] arr, double value) {
        int best = 0;
        double bestD = Math.abs(arr[0] - value);
        for (int i = 1; i < arr.length; i++) {
            double d = Math.abs(arr[i] - value);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        slider.setBounds(bounds);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        slider.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return slider.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        slider.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return slider.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }
}

final class SettingStringRow implements UiComponent {
    private final SettingString setting;
    private final TextField textField;
    private Rect bounds;

    public SettingStringRow(SettingString setting) {
        this.setting = setting;
        this.textField = new TextField();
        this.textField.setText(setting.getString());
        this.textField.setPlaceholder("Enter text...");
        this.textField.setOnChange(newValue -> setting.setValue(newValue));
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        // textField vie koko rivin leveyden, korkeus 16
        textField.setBounds(new Rect(bounds.x, bounds.y, bounds.w, 16));
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public int getPreferredHeight() { return 16; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // Piirretään label ensin? Yleensä asetuksen nimi ja sitten kenttä.
        // Toteutetaan niin, että vasemmalla nimi, oikealla textField.
        // Yksinkertainen: koko rivi on textField, mutta näytetään myös nimi?
        // Parempi: tehdään kuten SettingNumberSliderRow: nimi + komponentti.
        // Käytetään nyt yksinkertaista: nimi vasemmalla, textField oikealla.
        int labelWidth = 80;
        ui.text(setting.getName(), bounds.x, bounds.y + 4, ui.theme.text);
        Rect fieldRect = new Rect(bounds.x + labelWidth, bounds.y, bounds.w - labelWidth, 16);
        textField.setBounds(fieldRect);
        textField.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return textField.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        textField.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return textField.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return textField.mouseScrolled(ui, mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        return textField.keyPressed(ui, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return textField.charTyped(ui, chr, modifiers);
    }
}