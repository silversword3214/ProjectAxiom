package silversword.axiom.client.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.function.Consumer;

public class TextField implements UiComponent {

    private Rect bounds;
    private String text = "";
    private String placeholder = "";
    private boolean focused = false;
    private int cursorPos = 0;
    private float blinkTimer = 0;
    private static final float BLINK_INTERVAL = 10f;
    private Consumer<String> onChange = null;

    private static final int RADIUS = 4;
    private static final float OUTLINE_THICKNESS = 1.5f;

    /** Globaali fokusoitu kenttä – ClickGuiScreen käyttää tätä SDL-syötön reititykseen. */
    private static TextField focusedField = null;
    public static TextField getFocusedField() { return focusedField; }

    // ─────────────────────────────────────────────────────────────
    //  Perus-API
    // ─────────────────────────────────────────────────────────────

    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public Rect getBounds() { return bounds; }
    public String getText() { return text; }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        this.cursorPos = this.text.length();   // kursori aina tekstin LOPPUUN
        this.blinkTimer = 0;
        if (onChange != null) onChange.accept(this.text);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public void setOnChange(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public boolean isFocused() { return focused; }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            focusedField = this;
            blinkTimer = 0;
        } else if (focusedField == this) {
            focusedField = null;
        }
    }

    private void clampCursor() {
        if (cursorPos < 0) cursorPos = 0;
        if (cursorPos > text.length()) cursorPos = text.length();
    }

    @Override public int getPreferredHeight() { return 16; }

    // ─────────────────────────────────────────────────────────────
    //  Syöttö
    // ─────────────────────────────────────────────────────────────

    public void appendChar(char c) {
        if (c < 32 || c == 127 || c == 167) return;
        clampCursor();
        text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
        cursorPos++;
        if (onChange != null) onChange.accept(text);
        blinkTimer = 0;
    }

    public void backspace() {
        clampCursor();
        if (cursorPos > 0) {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            cursorPos--;
            if (onChange != null) onChange.accept(text);
            blinkTimer = 0;
        }
    }

    public void delete() {
        clampCursor();
        if (cursorPos < text.length()) {
            text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
            if (onChange != null) onChange.accept(text);
            blinkTimer = 0;
        }
    }

    public void moveCursor(int delta) {
        clampCursor();
        int old = cursorPos;
        cursorPos = Math.max(0, Math.min(text.length(), cursorPos + delta));
        if (cursorPos != old) blinkTimer = 0;
    }

    public void home() { cursorPos = 0; blinkTimer = 0; }
    public void end()  { cursorPos = text.length(); blinkTimer = 0; }

    // ─────────────────────────────────────────────────────────────
    //  Render — sama tyyli kuin ModuleSearchBar
    // ─────────────────────────────────────────────────────────────

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        blinkTimer += delta;
        boolean hover = bounds.contains(mouseX, mouseY);

        // 1. Taustaväri — sama logiikka kuin ModuleSearchBar
        int bg = focused
                ? ui.theme.panel
                : (hover ? ui.theme.buttonHover : ui.theme.button);

        // 2. Pyöristetty tausta (kaikki kulmat pyöreät)
        ui.fillRounded(bounds, bg, RADIUS);

        // 3. Fokus-rengas — sama kuin ModuleSearchBar
        if (focused) {
            ui.drawRoundedOutline(bounds, ui.theme.accent, RADIUS, OUTLINE_THICKNESS);
        }

        // 4. Teksti
        int fontHeight = ui.fontHeight();
        int textX = bounds.x + ui.theme.innerPadding;
        int textY = bounds.y + bounds.h / 2 - fontHeight / 2 + 4;

        String displayText = text;
        boolean usePlaceholder = displayText.isEmpty() && !focused;
        int color = usePlaceholder ? ui.theme.textDim : ui.theme.text;

        ui.text(usePlaceholder ? placeholder : displayText, textX, textY, color);

        // 5. Kursori — piirretään aina kun fokusoituna, vilkkuu
        if (focused && ((int)(blinkTimer / BLINK_INTERVAL) % 2 == 0)) {
            int cursorOffset = text.isEmpty()
                    ? 0
                    : ui.textWidth(text.substring(0, cursorPos));
            int cursorX = textX + cursorOffset;
            int cursorY = textY - 3;
            ui.fill(cursorX, cursorY, 1, fontHeight, ui.theme.text);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Hiiri
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        boolean nowFocused = bounds.contains(mouseX, mouseY);

        if (nowFocused) {
            if (focusedField != null && focusedField != this) {
                focusedField.setFocused(false);
            }

            int textX = bounds.x + ui.theme.innerPadding;
            int clickX = (int) mouseX;
            int bestPos = 0;
            int bestDist = Integer.MAX_VALUE;

            for (int i = 0; i <= text.length(); i++) {
                String before = text.substring(0, i);
                int charX = textX + ui.textWidth(before);
                int dist = Math.abs(clickX - charX);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestPos = i;
                }
            }

            cursorPos = bestPos;
            focused = true;
            focusedField = this;
            blinkTimer = 0;
        } else {
            if (this.focused) {
                this.focused = false;
                if (focusedField == this) focusedField = null;
            }
        }
        return nowFocused;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }

    // ─────────────────────────────────────────────────────────────
    //  Näppäimistö — SDL scancodes
    // ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(UiContext ui, int scancode, int keycode, int modifiers) {
        if (!focused) return false;

        if (scancode == InputConstants.KEY_BACKSPACE) { backspace();    return true; }
        if (scancode == InputConstants.KEY_DELETE)    { delete();       return true; }
        if (scancode == InputConstants.KEY_LEFT)      { moveCursor(-1); return true; }
        if (scancode == InputConstants.KEY_RIGHT)     { moveCursor(1);  return true; }
        if (scancode == InputConstants.KEY_HOME)      { home();         return true; }
        if (scancode == InputConstants.KEY_END)       { end();          return true; }

        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            appendChar(chr);
            return true;
        }
        return false;
    }
}