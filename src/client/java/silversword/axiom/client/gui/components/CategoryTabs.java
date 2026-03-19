package silversword.axiom.client.gui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CategoryTabs implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);

    private final List<String> tabs;
    private final Supplier<String> selected;
    private final Consumer<String> setSelected;

    private static final int OUTER_PAD_X = 6;
    private static final int OUTER_PAD_Y = 3;
    private static final int TAB_H = 16;
    private static final int TAB_GAP_X = 4;
    private static final int ROW_GAP_Y = 4;
    private static final int MAX_ROWS = 2;

    public CategoryTabs(List<String> tabs, Supplier<String> selected, Consumer<String> setSelected) {
        this.tabs = tabs;
        this.selected = selected;
        this.setSelected = setSelected;
    }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect bounds) { this.bounds = bounds; }

    @Override
    public int getPreferredHeight() {
        int rows = measureRowsNeeded(bounds.w);
        rows = Math.max(1, Math.min(MAX_ROWS, rows));
        return OUTER_PAD_Y + (rows * TAB_H) + ((rows - 1) * ROW_GAP_Y) + OUTER_PAD_Y;
    }

    private int measureRowsNeeded(int totalWidth) {
        int usable = Math.max(0, totalWidth - OUTER_PAD_X * 2);
        if (usable <= 0) return 1;

        // Käytetään vanilla tekstin leveyttä? Tässä lasketaan vain rivien määrää, joten vanilla riittää
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int x = 0;
        int rows = 1;

        for (String t : tabs) {
            int w = Math.max(30, tr.getWidth(t) + 10);
            if (x > 0 && x + w > usable) {
                rows++;
                x = 0;
                if (rows >= MAX_ROWS) return MAX_ROWS;
            }
            x += w + TAB_GAP_X;
        }
        return rows;
    }

    // CategoryTabs.java – render-metodi
    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        ui.fill(bounds, ui.theme.header);
        String sel = selected.get();
        if (sel == null) sel = "";
        int x0 = bounds.x + OUTER_PAD_X;
        int y0 = bounds.y + OUTER_PAD_Y;
        int x = x0;
        int y = y0;
        int row = 0;
        int rightLimit = bounds.right() - OUTER_PAD_X;

        for (String t : tabs) {
            int w = Math.max(30, ui.textWidth(t) + 10);
            if (x + w > rightLimit && x != x0) {
                row++;
                if (row >= MAX_ROWS) break;
                x = x0;
                y = y0 + row * (TAB_H + ROW_GAP_Y);
            }
            Rect r = new Rect(x, y, w, TAB_H);
            boolean isSel = t.equals(sel);
            boolean hover = r.contains(mouseX, mouseY);
            int bg;
            if (isSel) {
                int accent = ui.theme.accent;
                bg = (accent & 0x00FFFFFF) | 0x30000000; // 50% läpinäkyvyys
            } else {
                bg = hover ? ui.theme.buttonHover : ui.theme.button;
            }
            int fg = isSel ? ui.theme.text : ui.theme.textDim;
            ui.fill(r, bg);
            int textX = r.x + 5;
            int textY = r.y + (r.h - ui.fontHeight()) / 2 + ui.fontAscent();
            ui.text(t, textX, textY, fg);
            x += w + TAB_GAP_X;
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!bounds.contains(mouseX, mouseY)) return false;

        int x0 = bounds.x + OUTER_PAD_X;
        int y0 = bounds.y + OUTER_PAD_Y;
        int x = x0;
        int y = y0;
        int row = 0;
        int rightLimit = bounds.right() - OUTER_PAD_X;

        for (String t : tabs) {
            int w = Math.max(30, ui.textWidth(t) + 10);

            if (x + w > rightLimit && x != x0) {
                row++;
                if (row >= MAX_ROWS) break;
                x = x0;
                y = y0 + row * (TAB_H + ROW_GAP_Y);
            }

            Rect r = new Rect(x, y, w, TAB_H);
            if (r.contains(mouseX, mouseY)) {
                setSelected.accept(t);
                return true;
            }

            x += w + TAB_GAP_X;
        }

        return true;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}