package silversword.axiom.client.gui.components;

import net.minecraft.client.Minecraft;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

public class LabeledColorPicker implements UiComponent {
    private Rect bounds;
    private final String label;
    private final SettingColor color;

    // Layout lasketaan dynaamisesti UiContextin fontin mukaan
    private Rect labelBounds;
    private Rect swatchBounds;

    public LabeledColorPicker(String label, SettingColor color) {
        this.label = label;
        this.color = color;
    }

    @Override public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        // Layout lasketaan uudelleen renderissä / klikkauksessa (tarvitaan UiContextia)
        // että pysyy synkronissa skaalauksen kanssa.
    }

    /** Lasketaan layout fontin mittojen perusteella. */
    private void updateLayout(UiContext ui) {
        if (bounds == null) return;

        int fontH      = ui.fontHeight();
        int padding    = Math.max(4, fontH / 2);
        int swatchSize = Math.max(12, fontH + 6);

        // Neliö oikeaan reunaan
        int swatchX = bounds.right() - swatchSize - padding;
        int swatchY = bounds.y + (bounds.h - swatchSize) / 2;

        labelBounds  = new Rect(bounds.x, bounds.y,
                Math.max(10, swatchX - bounds.x - padding), bounds.h);
        swatchBounds = new Rect(swatchX, swatchY, swatchSize, swatchSize);
    }

    @Override public int getPreferredHeight() { return 24; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        updateLayout(ui);

        int textY = labelBounds.y + labelBounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(label, labelBounds.x, textY, ui.theme.text);

        // Väri reunus (selkeämpi kontrasti tummalla taustalla)
        ui.fill(swatchBounds, color.getCurrentColor().getARGB());
        ui.drawOutline(swatchBounds, ui.theme.border);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 1) return false;
        updateLayout(ui);
        if (swatchBounds.contains(mouseX, mouseY)) {
            var factory = AxiomMod.getWindowFactory();
            if (factory != null) {
                var mc = Minecraft.getInstance();
                int sw = mc.getWindow().getGuiScaledWidth();
                int sh = mc.getWindow().getGuiScaledHeight();
                var picker = new HsvColorPicker(color, () -> {});
                factory.openColorPickerWindow("color_picker", "Pick Color", sw, sh, picker);
            }
            return true;
        }
        return false;
    }

    public SettingColor getColor() {
        return color;
    }

    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
}