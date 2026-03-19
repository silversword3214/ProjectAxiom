package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.utils.render.TextUtils;
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

    private static TextField focusedField = null; // globaali fokusoitu kenttä

    @Override
    public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override
    public Rect getBounds() { return bounds; }
    public String getText() { return text; }
    public void setText(String text) {
        this.text = text == null ? "" : text;
        clampCursor();
        if (onChange != null) onChange.accept(this.text);
    }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder == null ? "" : placeholder; }
    public void setOnChange(Consumer<String> onChange) { this.onChange = onChange; }

    private void clampCursor() {
        if (cursorPos < 0) cursorPos = 0;
        if (cursorPos > text.length()) cursorPos = text.length();
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused && focusedField == this) focusedField = null;
    }

    @Override
    public int getPreferredHeight() { return 16; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        blinkTimer += delta;
        boolean hover = bounds.contains(mouseX, mouseY);
        int bgColor = focused ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
        ui.fill(bounds, bgColor);

        int textX = bounds.x + 4;
        int textY = bounds.y + (bounds.h - TextUtils.FONT_HEIGHT) / 2;
        String displayText = text;
        boolean usePlaceholder = displayText.isEmpty() && !focused;
        if (usePlaceholder) {
            displayText = placeholder;
            ui.text(displayText, textX, textY, ui.theme.textDim);
        } else {
            ui.text(displayText, textX, textY, ui.theme.text);
        }

        // Kursori
        if (focused && !usePlaceholder && ((int)(blinkTimer / BLINK_INTERVAL) % 2 == 0)) {
            int realTextX = text.length() * TextUtils.CHAR_UNIT;


            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = textX + realTextX;
            ui.fill(cursorX, textY, 1, TextUtils.FONT_HEIGHT, ui.theme.text);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        boolean nowFocused = bounds.contains(mouseX, mouseY);
        if (nowFocused) {
            // Poista fokus edelliseltä
            if (focusedField != null && focusedField != this) {
                focusedField.setFocused(false);
            }
            // Laske kursorin paikka klikkauksen perusteella (pikselitarkkuus)
            int textX = bounds.x + 4;
            String currentText = text;
            int clickX = (int) mouseX;
            int bestPos = 0;
            int bestDist = Integer.MAX_VALUE;
            for (int i = 0; i <= currentText.length(); i++) {
                String before = currentText.substring(0, i);
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

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}

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
        if (!focused) return false;
        int oldCursor = cursorPos;
        switch (keyCode) {
            case 259: // BACKSPACE
                if (cursorPos > 0) {
                    text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                    cursorPos--;
                    if (onChange != null) onChange.accept(text);
                }
                return true;
            case 261: // DELETE
                if (cursorPos < text.length()) {
                    text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                    if (onChange != null) onChange.accept(text);
                }
                return true;
            case 263: // LEFT
                if (cursorPos > 0) cursorPos--;
                break;
            case 262: // RIGHT
                if (cursorPos < text.length()) cursorPos++;
                break;
            case 268: // HOME
                cursorPos = 0;
                break;
            case 269: // END
                cursorPos = text.length();
                break;
            default:
                return false;
        }
        if (cursorPos != oldCursor) {
            blinkTimer = 0;
        }
        return true;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            text = text.substring(0, cursorPos) + chr + text.substring(cursorPos);
            cursorPos++;
            if (onChange != null) onChange.accept(text);
            return true;
        }
        return false;
    }
}