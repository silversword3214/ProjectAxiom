package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.NamedColor;

import java.util.ArrayList;
import java.util.List;

public class ColorCustomizerView implements UiComponent {
    private Rect bounds;
    private final List<LabeledColorPicker> pickers = new ArrayList<>();

    public ColorCustomizerView(ColorConfigurable configurable) {
        for (NamedColor nc : configurable.getColors()) {
            pickers.add(new LabeledColorPicker(nc.getName(), nc.getColor()));
        }
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int y = bounds.y + 4;
        int rowHeight = 20;
        int rowWidth = Math.max(10, bounds.w - 8);

        for (LabeledColorPicker picker : pickers) {
            picker.setBounds(new Rect(bounds.x + 4, y, rowWidth, rowHeight));
            y += rowHeight + 2;
        }
    }

    @Override
    public int getPreferredHeight() {
        return pickers.size() * 22 + 4;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        for (LabeledColorPicker picker : pickers) {
            picker.render(ui, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        for (LabeledColorPicker picker : pickers) {
            if (picker.getBounds().contains(mouseX, mouseY) && picker.mouseClicked(ui, mouseX, mouseY, button))
                return true;
        }
        return false;
    }

    // Muut UiComponent-metodit (mouseReleased, mouseDragged, jne.) delegoidaan pickereille
    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        for (LabeledColorPicker picker : pickers) {
            picker.mouseReleased(ui, mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (LabeledColorPicker picker : pickers) {
            if (picker.getBounds().contains(mouseX, mouseY) && picker.mouseDragged(ui, mouseX, mouseY, button, deltaX, deltaY))
                return true;
        }
        return false;
    }

    // Muut metodit (keyPressed, charTyped, mouseScrolled) – palautetaan false / ei tehdä mitään
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}