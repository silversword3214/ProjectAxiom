package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModeDropdown implements UiComponent {
    private Rect bounds;
    private final String[] modes;
    private final List<Rect> itemBounds = new ArrayList<>();
    private String selected;
    private final Consumer<String> onSelect;

    public ModeDropdown(String[] modes, String defaultMode, Consumer<String> onSelect) {
        this.modes = modes;
        this.selected = defaultMode;
        this.onSelect = onSelect;
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        itemBounds.clear();
        int y = bounds.y + 2;
        int itemHeight = 16;
        int gap = 2;
        for (int i = 0; i < modes.length; i++) {
            Rect r = new Rect(bounds.x + 2, y, bounds.w - 4, itemHeight);
            itemBounds.add(r);
            y += itemHeight + gap;
        }
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public int getPreferredHeight() {
        return modes.length * 16 + (modes.length - 1) * 2 + 4;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        ui.fill(bounds, ui.theme.panel);
        // reunat
        ui.fill(bounds.x, bounds.y, bounds.w, 1, ui.theme.border);
        ui.fill(bounds.x, bounds.bottom() - 1, bounds.w, 1, ui.theme.border);
        ui.fill(bounds.x, bounds.y, 1, bounds.h, ui.theme.border);
        ui.fill(bounds.right() - 1, bounds.y, 1, bounds.h, ui.theme.border);

        for (int i = 0; i < modes.length; i++) {
            Rect r = itemBounds.get(i);
            boolean hover = r.contains(mouseX, mouseY);
            boolean isSelected = modes[i].equals(selected);
            int bgColor = isSelected ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
            ui.fill(r, bgColor);
            int textY = r.y + r.h / 2 - ui.fontHeight() / 2 + 4;
            ui.text(modes[i], r.x + 4, textY, ui.theme.text);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        for (int i = 0; i < modes.length; i++) {
            if (itemBounds.get(i).contains(mouseX, mouseY)) {
                if (button == 0) {
                    selected = modes[i];
                    onSelect.accept(selected);
                }
                return true;
            }
        }
        return false;
    }

    public void setSelected(String mode) {
        this.selected = mode;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}