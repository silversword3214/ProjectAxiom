package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

public class Button implements UiComponent {
    private Rect bounds;
    private final String label;
    private final Runnable onClick;
    private boolean drawBackground = true;

    public Button(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    @Override
    public Rect getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }

    @Override
    public int getPreferredHeight() {
        return 16;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        if (drawBackground) {
            int bgColor = hover ? ui.theme.buttonHover : ui.theme.button;
            ui.fillRounded(bounds, bgColor, 3);
        }
        // Draw outline if hover and no background
        if (!drawBackground && hover) {
            ui.drawOutline(bounds, ui.theme.accent);
        }

        int textX = bounds.x + (bounds.w - ui.textWidth(label)) / 2;
        int textY = bounds.y + (bounds.h - ui.fontHeight()) / 2 + 4;
        ui.text(label, textX, textY, ui.theme.text);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0 && bounds.contains(mouseX, mouseY)) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }

    // Other UiComponent methods with empty implementations
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}