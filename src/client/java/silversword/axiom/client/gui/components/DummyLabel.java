package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

public final class DummyLabel implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final String text;

    public DummyLabel(String text) {
        this.text = text;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 16; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        int textY = bounds.y + 2 + ui.fontAscent();
        ui.text(text, bounds.x, textY, ui.theme.textDim);
    }

    @Override public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) { return false; }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}