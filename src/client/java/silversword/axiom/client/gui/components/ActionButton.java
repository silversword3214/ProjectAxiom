package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

public final class ActionButton implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private String label;
    private final Runnable onClick;

    public ActionButton(String label, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.onClick = onClick;
    }

    public void setLabel(String newLabel) { this.label = newLabel; }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect r) { this.bounds = r; }
    @Override public int getPreferredHeight() { return 26; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);

        int textX = bounds.x + ui.theme.innerPadding;
        // Yksinkertainen pystysuuntainen keskitys (kuten ModuleRow)
        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;

        // Pakotetaan väri valkoiseksi testiksi, tai käytä teeman text
        ui.text(label, textX, textY, hover ? ui.theme.scrollbarHover : ui.theme.text);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        if (button == 0 && onClick != null) onClick.run();
        return true;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}