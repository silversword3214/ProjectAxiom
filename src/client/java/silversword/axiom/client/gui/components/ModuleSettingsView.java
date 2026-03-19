package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.misc.TunnelMiner;
import silversword.axiom.client.modules.render.SearchBlocks;
import silversword.axiom.client.setting.*;

import java.util.List;
import java.util.Objects;

public final class ModuleSettingsView implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final AxiomMod module;
    private final ScrollContainer scroll = new ScrollContainer();
    private int lastCount = -1;

    public ModuleSettingsView(AxiomMod module) {
        this.module = Objects.requireNonNull(module);
        scroll.setInnerPadding(6);
        scroll.setGap(6);
    }

    @Override public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect r) {
        bounds = r;
        scroll.setBounds(r);
    }

    @Override
    public int getPreferredHeight() {
        return 9999; // scroll hoitaa
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        List<Setting> settings = module.getSettings();
        int count = settings == null ? 0 : settings.size();

        if (scroll.getChildren().isEmpty() || count != lastCount) {
            rebuild(settings);
            lastCount = count;
        }

        scroll.render(ui, mouseX, mouseY, delta);
    }

    private void rebuild(List<Setting> settings) {
        scroll.clear();

        if (settings == null || settings.isEmpty()) {
            scroll.add(new DummyLabel("No settings"));
        } else {
            for (Setting s : settings) {
                if (s == null) continue;
                UiComponent row =
                        (s instanceof SettingBoolean b) ? new SettingToggleRow(b) :
                                (s instanceof SettingMode m) ? new SettingModeRow(m) :
                                        (s instanceof SettingNumber n) ? new SettingNumberSliderRow(n) :
                                                (s instanceof SettingSlider sl) ? new SettingPresetSliderRow(sl) :
                                                        (s instanceof SettingTime t) ? new SettingTimeFieldRow(t) : // <-- MUUTOS
                                                                new SettingFallbackRow(s);


                scroll.add(row);
            }
        }

        // COLOR CUSTOMIZER
        if (module instanceof ColorConfigurable) {
            scroll.add(new ActionButton("Edit Colors", () -> {
                ((ColorConfigurable) module).openColorEditor();
            }));
        }

        // BLOCK TARGETING
        if (module instanceof SearchBlocks) {
            scroll.add(new ActionButton("Target Blocks", () -> {
                ((SearchBlocks) module).openBlockSelector();
            }));
        }

        if (module instanceof TunnelMiner) {
            scroll.add(new ActionButton("Search Blocks", () -> {
                ((TunnelMiner) module).openBlockSelector();
            }));
        }
    }

    // input forward
    @Override public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) { return scroll.mouseClicked(ui, mouseX, mouseY, button); }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) { scroll.mouseReleased(ui, mouseX, mouseY, button); }
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return scroll.mouseDragged(ui, mouseX, mouseY, button, dx, dy); }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return scroll.mouseScrolled(ui, mouseX, mouseY, amount); }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return scroll.keyPressed(ui, keyCode, scanCode, modifiers); }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return scroll.charTyped(ui, chr, modifiers); }
}