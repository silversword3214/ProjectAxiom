package silversword.axiom.client.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SearchBar implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private boolean focused = false;

    // Kursori
    private float blinkTimer = 0f;
    private static final float BLINK_INTERVAL = 10f;

    // Visuaalinen tyyli — sama kuin ModuleSearchBar
    private static final int RADIUS = 4;
    private static final float OUTLINE_THICKNESS = 1.5f;
    private static final int CURSOR_PADDING_Y = 3;

    public SearchBar(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return 18; }

    public boolean isFocused() { return focused; }

    public void setFocused(boolean f) {
        this.focused = f;
        if (f) blinkTimer = 0f;
    }

    private String currentText() {
        String s = getter.get();
        return s == null ? "" : s;
    }

    /** Julkinen: reititys Screenistä käyttää tätä. */
    public void appendChar(char c) {
        if (c < 32 || c == 127 || c == 167) return;
        setter.accept(currentText() + c);
        blinkTimer = 0f;
    }

    public void backspace() {
        String s = currentText();
        if (!s.isEmpty()) {
            setter.accept(s.substring(0, s.length() - 1));
            blinkTimer = 0f;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Render
    // ─────────────────────────────────────────────────────────────

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        blinkTimer += delta;

        boolean hover = bounds.contains(mouseX, mouseY);
        int bg = focused ? ui.theme.panel : (hover ? ui.theme.buttonHover : ui.theme.button);
        ui.fillRounded(bounds, bg, RADIUS);

        if (focused) {
            ui.drawRoundedOutline(bounds, ui.theme.accent, RADIUS, OUTLINE_THICKNESS);
        }

        String text = currentText();
        String shown = text.isEmpty() && !focused ? "Search..." : text;
        int color = text.isEmpty() && !focused ? ui.theme.textDim : ui.theme.text;

        int textX = bounds.x + ui.theme.innerPadding;
        int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 3;
        ui.text(shown, textX, textY, color);

        // VILKKUVA KURSORI
        if (focused && ((int)(blinkTimer / BLINK_INTERVAL) % 2 == 0)) {
            int caretX = textX + ui.textWidth(text);
            ui.fill(caretX,
                    bounds.y + CURSOR_PADDING_Y,
                    1,
                    bounds.h - CURSOR_PADDING_Y * 2,
                    ui.theme.accent);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Hiiri
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        boolean wasFocused = focused;
        setFocused(bounds.contains(mouseX, mouseY));
        return wasFocused || focused;
    }

    // ─────────────────────────────────────────────────────────────
    //  Näppäimistö — vain ERIKOISNÄPPÄIMET
    //  Merkkinsyöttö hoidetaan Screen.charTyped-polulla tai
    //  Screen.tryHandleTyping(SDL_GetKeyName)-polulla.
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(UiContext ui, int scancode, int keycode, int modifiers) {
        if (!focused) return false;

        if (scancode == InputConstants.KEY_ESCAPE
                || scancode == InputConstants.KEY_RETURN) {
            setFocused(false);
            return true;
        }
        if (scancode == InputConstants.KEY_BACKSPACE) {
            backspace();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!focused) return false;
        appendChar(chr);
        return true;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}