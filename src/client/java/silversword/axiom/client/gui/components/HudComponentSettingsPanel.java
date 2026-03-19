package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.HudComponentSettings;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.setting.*;

public class HudComponentSettingsPanel implements UiComponent {
    private Rect bounds;
    private final HudElement element;
    private final HudComponentSettings settings;
    private final ScrollContainer scrollContainer = new ScrollContainer();

    private SettingBoolean customColorsToggle; // viittaus toggleen, jos sellainen on

    public HudComponentSettingsPanel(HudElement element) {
        this.element = element;
        this.settings = ((BaseHudElement) element).getSettings();

        // Etsitään "Custom Colors" -toggle
        for (Setting setting : settings.getSettings()) {
            if (setting instanceof SettingBoolean && setting.getName().equals("Custom Colors")) {
                customColorsToggle = (SettingBoolean) setting;
                break;
            }
        }

        // Rakennetaan komponentit aluksi
        rebuildComponents();
    }

    private void rebuildComponents() {
        scrollContainer.clear();

        // Lisätään kaikki tavalliset asetukset (mukaan lukien toggle)
        for (Setting setting : settings.getSettings()) {
            UiComponent comp = createComponent(setting);
            if (comp != null) {
                scrollContainer.add(comp);
            }
        }

        // Lisätään väripaletti vain jos toggle on päällä (tai togglea ei ole)
        boolean showColors = (customColorsToggle == null) || customColorsToggle.get();
        if (showColors) {
            for (NamedColor nc : settings.getNamedColors()) {
                scrollContainer.add(new LabeledColorPicker(nc.getName(), nc.getColor()));
            }
        }

    }

    private UiComponent createComponent(Setting setting) {
        if (setting instanceof SettingSlider slider) {
            double[] presets = slider.getPresets();
            double min = presets[0];
            double max = presets[presets.length - 1];
            double step = presets.length > 1 ? (presets[1] - presets[0]) : 0.1;
            return new Slider(
                    setting.getName(),
                    min, max, step,
                    slider::getValue,
                    v -> slider.setValue(v),
                    v -> slider.getDisplayValue()
            );
        } else if (setting instanceof SettingNumber number) {
            return new Slider(
                    setting.getName(),
                    number.getMin(),
                    number.getMax(),
                    number.getStep(),
                    number::getValue,
                    v -> number.setValue(v),
                    v -> number.getDisplayValue()
            );
        } else if (setting instanceof SettingBoolean bool) {
            // Jos tämä on "Custom Colors" -toggle, sen setteri päivittää koko paneelin
            if (bool == customColorsToggle) {
                return new Toggle(
                        setting.getName(),
                        bool::get,
                        v -> {
                            bool.set(v);
                            rebuildComponents(); // päivitetään näkyvyys
                        }
                );
            } else {
                return new Toggle(
                        setting.getName(),
                        bool::get,
                        bool::set
                );
            }
        }
        // Muut asetustyypit (SettingMode) voidaan lisätä myöhemmin
        return null;
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        scrollContainer.setBounds(bounds);
    }

    @Override
    public int getPreferredHeight() {
        // Paneeli voi haluta olla esim. 200px korkea, tai se voi laskea scrollContainerin kautta,
        // mutta ScrollContainerin preferredHeight on 120. Annetaan nyt 200, joka on järkevä oletus.
        return 200;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        scrollContainer.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return scrollContainer.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        scrollContainer.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return scrollContainer.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return scrollContainer.mouseScrolled(ui, mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        return scrollContainer.keyPressed(ui, keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return scrollContainer.charTyped(ui, chr, modifiers);
    }
}