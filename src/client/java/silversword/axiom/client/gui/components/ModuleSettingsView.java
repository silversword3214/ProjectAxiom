package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.misc.AutoMine;
import silversword.axiom.client.modules.render.SearchBlocks;
import silversword.axiom.client.setting.*;

import java.util.List;
import java.util.Objects;

public final class ModuleSettingsView implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final AxiomMod module;
    private final ScrollContainer scroll = new ScrollContainer();
    private int lastCount = -1;
    private boolean needsRebuild = false;

    public ModuleSettingsView(AxiomMod module) {
        this.module = Objects.requireNonNull(module);
        scroll.setInnerPadding(2);
        scroll.setGap(0);
    }

    @Override public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect r) {
        bounds = r;
        scroll.setBounds(r);
    }

    @Override
    public int getPreferredHeight() { return 9999; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        List<Setting> settings = module.getSettings();
        int count = settings == null ? 0 : settings.size();

        if (scroll.getChildren().isEmpty() || count != lastCount || needsRebuild) {
            rebuild(settings);
            lastCount = count;
            needsRebuild = false;
        }

        scroll.render(ui, mouseX, mouseY, delta);
    }

    private void rebuild(List<Setting> settings) {
        scroll.clear();

        if (settings == null || settings.isEmpty()) {
            scroll.add(new DummyLabel("No settings"));
            return;
        }

        for (Setting s : settings) {
            if (s == null) continue;

            // Piilota tällä hetkellä ei-aktiiviset subsettingit
            if (!s.isVisible()) continue;

            UiComponent row = buildRow(s);

            // Jos on parent, kääritään sisennyskomponenttiin
            if (s.getParent() != null) {
                scroll.add(new IndentedRow(row, 12));
            } else {
                scroll.add(row);
            }

            // Jos tämä on SettingBoolean jolla on lapsia, kuunnellaan muutoksia
            if (s instanceof SettingBoolean b && hasChildren(settings, b)) {
                b.setOnChange(() -> needsRebuild = true);
            }
        }

        // Color/block buttons
        if (module instanceof ColorConfigurable) {
            scroll.add(new ActionButton("Edit Colors", () ->
                    ((ColorConfigurable) module).openColorEditor()));
        }
        if (module instanceof SearchBlocks) {
            scroll.add(new ActionButton("Target Blocks", () ->
                    ((SearchBlocks) module).openBlockSelector()));
        }
        if (module instanceof AutoMine) {
            scroll.add(new ActionButton("Search Blocks", () ->
                    ((AutoMine) module).openBlockSelector()));
        }
    }

    private UiComponent buildRow(Setting s) {
        return switch (s) {
            case SettingBoolean b  -> new SettingToggleRow(b);
            case SettingMode m     -> new SettingModeRow(m);
            case SettingNumber n   -> new SettingNumberSliderRow(n);
            case SettingSlider sl  -> new SettingPresetSliderRow(sl);
            case SettingTime t     -> new SettingTimeFieldRow(t);
            case SettingString str -> new SettingStringRow(str);
            default                -> new SettingFallbackRow(s);
        };
    }

    /** Tarkistaa onko jollakin settingillä tämä boolean parenttina */
    private boolean hasChildren(List<Setting> settings, SettingBoolean parent) {
        for (Setting s : settings) {
            if (s != null && s.getParent() == parent) return true;
        }
        return false;
    }

    @Override public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) { return scroll.mouseClicked(ui, mouseX, mouseY, button); }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) { scroll.mouseReleased(ui, mouseX, mouseY, button); }
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy); }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return scroll.mouseScrolled(ui, mouseX, mouseY, amount); }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return scroll.keyPressed(ui, keyCode, scanCode, modifiers); }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return scroll.charTyped(ui, chr, modifiers); }
}