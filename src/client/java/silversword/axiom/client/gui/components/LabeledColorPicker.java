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
        int labelWidth = 60;
        labelBounds = new Rect(bounds.x, bounds.y, labelWidth, bounds.h);
        int swatchSize = 20;
        swatchBounds = new Rect(bounds.x + labelWidth + 4, bounds.y + (bounds.h - swatchSize) / 2, swatchSize, swatchSize);
    }

    @Override public int getPreferredHeight() { return 24; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        int textY = labelBounds.y + labelBounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(label, labelBounds.x, textY, ui.theme.text);
        ui.fill(swatchBounds, color.getCurrentColor().getARGB());
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (swatchBounds.contains(mouseX, mouseY)) {
            var factory = AxiomMod.getWindowFactory();
            if (factory != null) {
                var mc = Minecraft.getInstance();
                int sw = mc.getWindow().getGuiScaledWidth();
                int sh = mc.getWindow().getGuiScaledHeight();
                var picker = new HsvColorPicker(color, () -> {});
                factory.openCustomWindow("color_picker", "Pick Color", sw, sh, picker);
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