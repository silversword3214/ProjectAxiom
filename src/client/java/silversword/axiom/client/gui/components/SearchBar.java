package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SearchBar implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private boolean focused = false;

    public SearchBar(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        int bg = focused ? ui.theme.panel : (hover ? ui.theme.buttonHover : ui.theme.button);
        ui.fillRounded(bounds, bg, 4); // pyöristetty

        String text = getter.get();
        if (text == null) text = "";

        String shown = text.isEmpty() && !focused ? "Search..." : text;
        int color = text.isEmpty() && !focused ? ui.theme.textDim : ui.theme.text;

        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(shown, bounds.x + ui.theme.innerPadding, textY, color);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        focused = bounds.contains(mouseX, mouseY);
        return focused;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (keyCode == 256 || keyCode == 257) {
            focused = false;
            return true;
        }
        if (keyCode == 259) {
            String s = getter.get();
            if (s == null) s = "";
            if (!s.isEmpty()) setter.accept(s.substring(0, s.length() - 1));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            String s = getter.get();
            if (s == null) s = "";
            setter.accept(s + chr);
            return true;
        }
        return false;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}