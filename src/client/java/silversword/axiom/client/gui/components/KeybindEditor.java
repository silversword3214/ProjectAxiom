package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.utils.KeyNames;
import silversword.axiom.client.utils.render.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class KeybindEditor implements UiComponent {
    private final List<SettingKeybind> keybinds;
    private Rect bounds = new Rect(0, 0, 300, 200);
    private final List<Boolean> waiting = new ArrayList<>();
    private static final int ROW_HEIGHT = 30;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_Y_OFFSET = 30;
    private static final int BOX_HEIGHT = 18; // kiinteä korkeus, vastaa TextUtils.FONT_HEIGHT * 2

    public KeybindEditor(List<SettingKeybind> keybinds) {
        this.keybinds = keybinds;
        for (int i = 0; i < keybinds.size(); i++) waiting.add(false);
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }
    @Override public int getPreferredHeight() { return keybinds.size() * ROW_HEIGHT + 60; }

    private String getKeyText(SettingKeybind kb) {
        int key = kb.get();
        if (key == 0) return "N/A";
        return KeyNames.get(key);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {

        int y = bounds.y + 10;
        int radius = ui.theme.radius;

        for (int i = 0; i < keybinds.size(); i++) {
            SettingKeybind kb = keybinds.get(i);
            boolean w = waiting.get(i);

            // Koko rivin tausta
            Rect rowRect = new Rect(bounds.x + 5, y, bounds.w - 10, ROW_HEIGHT - 2);
            ui.fillRounded(rowRect, ui.theme.button, radius);

            // Nimen teksti
            String name = kb.getName();
            int textY = rowRect.y + (rowRect.h - TextUtils.FONT_HEIGHT) / 2;
            ui.text(name, rowRect.x + 10, textY, ui.theme.text);

            // Keybind-laatikko (korkeus kiinteä, leveys tekstin mukaan)
            String keyText = getKeyText(kb);
            int keyWidth = keyText.length() * TextUtils.CHAR_UNIT; // arvio
            int boxWidth = keyWidth + 12; // 6px padding molemmin puolin
            int boxHeight = BOX_HEIGHT;
            int boxX = bounds.right() - boxWidth - 10; // 10px marginaali oikeasta reunasta
            int boxY = rowRect.y + (rowRect.h - boxHeight) / 2;
            Rect boxRect = new Rect(boxX, boxY, boxWidth, boxHeight);

            // Tausta
            int boxBg;
            if (w) {
                // Odotustilassa korostus
                boxBg = (ui.theme.accent & 0x00FFFFFF) | 0x30000000;
            } else {
                // Normaalisti tummempi
                boxBg = (ui.theme.buttonHover & 0x00FFFFFF) | 0x80000000;
            }
            ui.fill(boxRect, boxBg);

            ui.drawRectOutline(boxRect, 0xFFFFFFFF, 1.0);

            // Teksti laatikon sisällä
            int textX = boxRect.x + (boxRect.w - keyWidth) / 2;
            int textY2 = boxRect.y + (boxRect.h - TextUtils.FONT_HEIGHT) / 2 ;
            int textColor = w ? ui.theme.accent : ui.theme.text;
            ui.text(keyText, textX, textY2, textColor);

            y += ROW_HEIGHT;
        }

        // Reset-nappi
        int btnX = bounds.x + (bounds.w - BUTTON_WIDTH) / 2;
        int btnY = bounds.y + bounds.h - BUTTON_Y_OFFSET;
        Rect btnRect = new Rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean hover = btnRect.contains(mouseX, mouseY);
        int bgColor = hover ? ui.theme.buttonHover : ui.theme.button;


        ui.fillRounded(btnRect.x, btnRect.y, btnRect.w, btnRect.h, ui.theme.border, radius);
        ui.fillRounded(btnRect.x + 1, btnRect.y + 1, btnRect.w - 2, btnRect.h - 2, bgColor, Math.max(0, radius - 1));

        int textY = btnY + BUTTON_HEIGHT / 2 - TextUtils.FONT_HEIGHT / 2;
        ui.text("Reset", btnX + 10, textY, ui.theme.text);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY) || button != 0) return false;

        int btnX = bounds.x + (bounds.w - BUTTON_WIDTH) / 2;
        int btnY = bounds.y + bounds.h - BUTTON_Y_OFFSET;
        Rect btnRect = new Rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT);
        if (btnRect.contains(mouseX, mouseY)) {
            for (SettingKeybind kb : keybinds) kb.set(0);
            return true;
        }

        int y = bounds.y + 10;
        for (int i = 0; i < keybinds.size(); i++) {
            Rect rowRect = new Rect(bounds.x + 5, y, bounds.w - 10, ROW_HEIGHT - 2);
            if (rowRect.contains(mouseX, mouseY)) {
                if (waiting.get(i)) {
                    // Jos klikataan uudelleen odotustilassa, peruutetaan odotus
                    waiting.set(i, false);
                } else {
                    for (int j = 0; j < waiting.size(); j++) waiting.set(j, false);
                    waiting.set(i, true);
                }
                return true;
            }
            y += ROW_HEIGHT;
        }
        return true;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i)) {
                if (keyCode == 256) waiting.set(i, false);
                else {
                    keybinds.get(i).set(keyCode);
                    waiting.set(i, false);
                }
                return true;
            }
        }
        return false;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}