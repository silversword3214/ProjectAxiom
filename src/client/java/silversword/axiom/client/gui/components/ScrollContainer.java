package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.List;

public final class ScrollContainer implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final List<UiComponent> children = new ArrayList<>();
    private int scrollY = 0;
    private int contentHeight = 0;
    private int gap = 4;
    private int innerPadding = 4;

    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private int scrollStartY = 0;

    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_PADDING = 1;

    private boolean hovered = false;
    private boolean drawBackground = true;

    public ScrollContainer() {}

    public void setGap(int gap) { this.gap = Math.max(0, gap); }
    public void setInnerPadding(int innerPadding) { this.innerPadding = Math.max(0, innerPadding); }

    public void add(UiComponent c) {
        children.add(c);
        updateContentHeight();
    }

    public void clear() {
        children.clear();
        scrollY = 0;
        updateContentHeight();
    }

    public List<UiComponent> getChildren() { return children; }

    @Override public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        updateContentHeight();
    }

    @Override public int getPreferredHeight() { return 120; }

    private int maxScroll() {
        int viewH = Math.max(0, bounds.h - innerPadding * 2);
        return Math.max(0, contentHeight - viewH);
    }

    private void clampScroll() {
        int max = maxScroll();
        if (scrollY < 0) scrollY = 0;
        if (scrollY > max) scrollY = max;
    }

    private void updateContentHeight() {
        int total = innerPadding;
        for (UiComponent c : children) {
            total += c.getPreferredHeight() + gap;
        }
        total += innerPadding;
        contentHeight = total;
        clampScroll();
    }
    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }

    private void layoutChildren() {
        int x = bounds.x + innerPadding;
        int y = bounds.y + innerPadding - scrollY;
        int w = bounds.w - innerPadding * 2;

        for (UiComponent c : children) {
            int h = c.getPreferredHeight();
            c.setBounds(new Rect(x, y, w, h));
            y += h + gap;
        }
    }

    private Rect getScrollbarTrackRect() {
        int trackX = bounds.right() - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        int trackY = bounds.y + SCROLLBAR_PADDING;
        int trackH = bounds.h - SCROLLBAR_PADDING * 2;
        return new Rect(trackX, trackY, SCROLLBAR_WIDTH, trackH);
    }

    private Rect getScrollbarThumbRect() {
        int max = maxScroll();
        if (max <= 0) return new Rect(0, 0, 0, 0);

        Rect track = getScrollbarTrackRect();
        int thumbH = Math.max(14, (int) ((track.h * (float) bounds.h / contentHeight)));
        int thumbY = track.y + (int) ((track.h - thumbH) * (scrollY / (float) max));
        return new Rect(track.x, thumbY, track.w, thumbH);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {

        layoutChildren();

        ui.draw.enableScissor(bounds.x, bounds.y, bounds.right(), bounds.bottom());

        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r == null) continue;
            if (r.y >= bounds.y && r.bottom() <= bounds.bottom()) {
                c.render(ui, mouseX, mouseY, delta);
            }
        }

        ui.draw.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            boolean hover = bounds.contains(mouseX, mouseY);
            if (hover) {
                Rect thumb = getScrollbarThumbRect();
                ui.fillRounded(thumb.x, thumb.y, thumb.w, thumb.h, ui.theme.accent, thumb.w / 2.0);
            }
        }
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        updateContentHeight();

        int step = 18;
        int oldScroll = scrollY;

        scrollY -= (int) Math.signum(amount) * step;
        clampScroll();

        if (scrollY != oldScroll) {
            snapToNearestRow();
        }
        return true;
    }

    private void snapToNearestRow() {
        if (children.isEmpty()) return;

        int viewTop = bounds.y + innerPadding;
        int viewBottom = bounds.bottom() - innerPadding;

        int bestScroll = scrollY;
        int minDist = Integer.MAX_VALUE;

        int y = bounds.y + innerPadding;
        for (UiComponent c : children) {
            int h = c.getPreferredHeight();
            int childTop = y;
            int childBottom = y + h;

            if (childTop >= viewTop && childBottom <= viewBottom) {
                return;
            }

            int scrollForTop = scrollY + (childTop - viewTop);
            int scrollForBottom = scrollY + (childBottom - viewBottom);

            int max = maxScroll();
            if (scrollForTop >= 0 && scrollForTop <= max) {
                int dist = Math.abs(scrollForTop - scrollY);
                if (dist < minDist) {
                    minDist = dist;
                    bestScroll = scrollForTop;
                }
            }
            if (scrollForBottom >= 0 && scrollForBottom <= max) {
                int dist = Math.abs(scrollForBottom - scrollY);
                if (dist < minDist) {
                    minDist = dist;
                    bestScroll = scrollForBottom;
                }
            }

            y += h + gap;
        }

        if (minDist != Integer.MAX_VALUE) {
            scrollY = bestScroll;
        }
    }
    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (!bounds.contains(mouseX, mouseY)) return false;

        Rect thumb = getScrollbarThumbRect();
        if (thumb.contains(mouseX, mouseY)) {
            draggingScrollbar = true;
            dragStartY = (int) mouseY;
            scrollStartY = scrollY;
            return true;
        }

        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r == null) continue;
            if (r.y >= bounds.y && r.bottom() <= bounds.bottom() && r.contains(mouseX, mouseY)) {
                if (c.mouseClicked(ui, mouseX, mouseY, button)) return true;
            }
        }
        return true;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0) draggingScrollbar = false;
        for (UiComponent c : children) c.mouseReleased(ui, mouseX, mouseY, button);
    }


    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) return false;

        if (draggingScrollbar) {
            updateContentHeight();
            int max = maxScroll();
            if (max > 0) {
                Rect track = getScrollbarTrackRect();
                int thumbH = getScrollbarThumbRect().h;
                int available = track.h - thumbH;
                if (available > 0) {
                    int deltaY = (int) mouseY - dragStartY;
                    float pct = deltaY / (float) available;
                    int newScroll = scrollStartY + (int) (pct * max);
                    scrollY = Math.max(0, Math.min(max, newScroll));
                }
            }
            return true;
        }

        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r == null) continue;
            if (r.y >= bounds.y && r.bottom() <= bounds.bottom() && r.contains(mouseX, mouseY)) {
                if (c.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        for (UiComponent c : children) if (c.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        for (UiComponent c : children) if (c.charTyped(ui, chr, modifiers)) return true;
        return false;
    }
}