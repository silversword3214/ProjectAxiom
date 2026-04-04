package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

import java.util.ArrayList;
import java.util.List;

public final class ScrollContainer implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final List<UiComponent> children = new ArrayList<>();

    // Interpoloiva vieritys
    private double scrollY = 0;        // Nykyinen näkyvä sijainti
    private double targetScrollY = 0;  // Kohdesijainti, jota kohti liu'utaan
    private float scrollSpeed = 0.3f;   // Säädä tätä (pienempi = hitaampi/pehmeämpi)

    private int contentHeight = 0;
    private int gap = 4;
    private int innerPadding = 4;

    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private double scrollStartY = 0;

    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_PADDING = 1;
    private boolean showScrollBar = true;

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
        targetScrollY = 0;
        updateContentHeight();
    }

    public void setShowScrollBar(boolean show) {
        this.showScrollBar = show;
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
        if (targetScrollY < 0) targetScrollY = 0;
        if (targetScrollY > max) targetScrollY = max;

        // Jos ollaan hyvin lähellä kohdetta, hypätään suoraan siihen
        if (Math.abs(scrollY - targetScrollY) < 0.1) {
            scrollY = targetScrollY;
        }
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
        // Käytetään visuaalista scrollY:tä asettelussa
        int y = bounds.y + innerPadding - (int) scrollY;
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
        // Palkki seuraa visuaalista scrollY:tä
        int thumbY = track.y + (int) ((track.h - thumbH) * (scrollY / (float) max));
        return new Rect(track.x, thumbY, track.w, thumbH);
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        // Interpolointi logiikka
        if (scrollY != targetScrollY) {
            // Lineaarinen interpolaatio (lerp) delta-ajalla
            double difference = targetScrollY - scrollY;
            scrollY += difference * (delta * scrollSpeed);

            // Estetään "ylilyönnit" tai ikuinen hidas liuku
            if (Math.abs(targetScrollY - scrollY) < 0.1) {
                scrollY = targetScrollY;
            }
        }

        updateContentHeight();
        layoutChildren();

        ui.enableScissor(bounds.x, bounds.y, bounds.w, bounds.h);

        for (UiComponent c : children) {
            // Piirretään vain jos on näkyvissä (optimointi)
            Rect r = c.getBounds();
            if (r.y + r.h > bounds.y && r.y < bounds.y + bounds.h) {
                c.render(ui, mouseX, mouseY, delta);
            }
        }

        ui.disableScissor();

        int max = maxScroll();
        if (showScrollBar && max > 0 && (bounds.contains(mouseX, mouseY) || draggingScrollbar)) {
            Rect thumb = getScrollbarThumbRect();
            ui.fillRounded(thumb.x, thumb.y, thumb.w, thumb.h, ui.theme.accent, thumb.w / 2.0);
        }
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        updateContentHeight();

        int step = 40; // Suurempi askel tuntuu paremmalta interpoloinnin kanssa
        targetScrollY -= (int) Math.signum(amount) * step;
        clampScroll();

        return true;
    }

    // Snap-logiikka päivitetty käyttämään targetScrollY:tä
    private void snapToNearestRow() {
        if (children.isEmpty()) return;

        int viewTop = bounds.y + innerPadding;
        int viewBottom = bounds.bottom() - innerPadding;

        double bestTarget = targetScrollY;
        int minDist = Integer.MAX_VALUE;

        int y = bounds.y + innerPadding;
        for (UiComponent c : children) {
            int h = c.getPreferredHeight();
            int childTop = y;
            int childBottom = y + h;

            // Jos lapsi on jo kokonaan näkyvissä, ei tarvitse snapata
            if (childTop >= viewTop && childBottom <= viewBottom) {
                return;
            }

            double scrollForTop = targetScrollY + (childTop - viewTop);
            double scrollForBottom = targetScrollY + (childBottom - viewBottom);

            int max = maxScroll();
            if (scrollForTop >= 0 && scrollForTop <= max) {
                int dist = (int) Math.abs(scrollForTop - targetScrollY);
                if (dist < minDist) {
                    minDist = dist;
                    bestTarget = scrollForTop;
                }
            }
            if (scrollForBottom >= 0 && scrollForBottom <= max) {
                int dist = (int) Math.abs(scrollForBottom - targetScrollY);
                if (dist < minDist) {
                    minDist = dist;
                    bestTarget = scrollForBottom;
                }
            }

            y += h + gap;
        }

        if (minDist != Integer.MAX_VALUE) {
            targetScrollY = bestTarget;
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
            scrollStartY = targetScrollY; // Lähtökohta kohdesijainnista
            return true;
        }

        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r != null && r.contains(mouseX, mouseY)) {
                if (c.mouseClicked(ui, mouseX, mouseY, button)) return true;
            }
        }
        return true;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
            // Valinnainen: snapataan kun päästetään irti hiirestä
            // snapToNearestRow();
        }
        for (UiComponent c : children) {
            c.mouseReleased(ui, mouseX, mouseY, button);
        }
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
                    targetScrollY = scrollStartY + (pct * max);
                    clampScroll();
                    // Vierityspalkilla vedettäessä halutaan yleensä välitön vaste
                    scrollY = targetScrollY;
                }
            }
            return true;
        }

        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r != null && r.contains(mouseX, mouseY)) {
                if (c.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        for (UiComponent c : children) {
            if (c.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        for (UiComponent c : children) {
            if (c.charTyped(ui, chr, modifiers)) return true;
        }
        return false;
    }

    public void scrollTo(int y) {
        this.targetScrollY = y;
        clampScroll();
        // Jos haluat välittömän hypön (ei interpolointia):
        // this.scrollY = this.targetScrollY;
    }
}