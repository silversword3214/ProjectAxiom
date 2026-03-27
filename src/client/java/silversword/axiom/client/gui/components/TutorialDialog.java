package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import java.util.function.Consumer;

public class TutorialDialog implements UiComponent {
    private Rect bounds;
    private final String title;
    private final String text;
    private final Runnable onConfirm;
    private final Consumer<Boolean> onDontShowChanged;

    private Toggle dontShowToggle;
    private ActionButton okButton;
    private boolean dontShow = false;

    public TutorialDialog(String title, String text, Runnable onConfirm, Consumer<Boolean> onDontShowChanged) {
        this.title = title;
        this.text = text;
        this.onConfirm = onConfirm;
        this.onDontShowChanged = onDontShowChanged;

        this.dontShowToggle = new Toggle("Don't show this again", () -> dontShow, val -> {
            dontShow = val;
            if (onDontShowChanged != null) onDontShowChanged.accept(val);
        });
        this.okButton = new ActionButton("OK", () -> {
            if (onConfirm != null) onConfirm.run();
        });
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int padding = 12;
        int w = bounds.w - 2 * padding;
        int y = bounds.y + padding;

        // Toggle at bottom
        int toggleH = 20;
        dontShowToggle.setBounds(new Rect(bounds.x + padding, bounds.bottom() - toggleH - 30, w, toggleH));

        // OK button below toggle
        int btnW = 80;
        int btnH = 24;
        okButton.setBounds(new Rect(bounds.x + (bounds.w - btnW) / 2, dontShowToggle.getBounds().bottom() + 8, btnW, btnH));
    }

    @Override
    public int getPreferredHeight() {
        return 180; // enough for text + toggle + button
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // Title
        int titleY = bounds.y + 10;
        ui.text(title, bounds.x + 10, titleY, ui.theme.text);
        // Text lines
        String[] lines = text.split("\n");
        int y = titleY + ui.fontHeight() + 5;
        for (String line : lines) {
            ui.text(line, bounds.x + 10, y, ui.theme.textDim);
            y += ui.fontHeight();
        }

        dontShowToggle.render(ui, mouseX, mouseY, delta);
        okButton.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (dontShowToggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (okButton.mouseClicked(ui, mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        dontShowToggle.mouseReleased(ui, mouseX, mouseY, button);
        okButton.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return false;
    }
}