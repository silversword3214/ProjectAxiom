package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Momentum-pohjainen pehmeä vieritys + motion blur + hard stop reunoilla.
 * Motion bluria säädetään yhdellä luvulla: setMotionBlurIntensity(0..1)
 */
public final class ScrollContainer implements UiComponent {

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final List<UiComponent> children = new ArrayList<>();

    // ===== Fysiikka =====
    private double scrollY  = 0.0;
    private double velocity = 0.0;

    private double friction       = 3.8;
    private double impulsePerTick = 340.0;
    private static final double VELOCITY_EPSILON = 1.0;

    // ===== Tween =====
    private Double tweenTarget = null;
    private double tweenRate   = 11.0;

    // ============================================================
    //  MOTION BLUR – vain yksi luku (0..1)
    // ============================================================
    /** Ainoa blur-säätö. 0 = pois, 0.65 = oletus, 1 = voimakas. */
    private float motionBlurIntensity = 0f;

    // Johdetut arvot – päivittyvät setMotionBlurIntensity-kutsusta
    private int    blurSamples  = 3;
    private double blurStretch  = 2.3;
    private float  blurStrength = 0.39f;
    private static final double BLUR_MIN_DELTA = 0.5;

    private double prevScrollY = 0.0;

    // ===== Layout =====
    private int contentHeight = 0;
    private int gap = 4;
    private int innerPadding = 4;

    // ===== Scrollbar =====
    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private double scrollStartY = 0;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_PADDING = 1;
    private boolean showScrollBar = true;
    private boolean drawBackground = true;

    public ScrollContainer() {
        applyBlurIntensity(); // alusta johdetut arvot
    }

    // ================================================================
    //  Konfiguraatio
    // ================================================================
    public void setGap(int g)               { this.gap = Math.max(0, g); }
    public void setInnerPadding(int p)      { this.innerPadding = Math.max(0, p); }
    public void setShowScrollBar(boolean s) { this.showScrollBar = s; }
    public void setDrawBackground(boolean b){ this.drawBackground = b; }
    public void setFriction(double f)       { this.friction = Math.max(0.1, f); }
    public void setImpulsePerTick(double p) { this.impulsePerTick = Math.max(1.0, p); }
    public void setTweenRate(double r)      { this.tweenRate = Math.max(0.1, r); }

    /** Johtaa samples / stretch / strength yhdestä luvusta. */
    private void applyBlurIntensity() {
        if (motionBlurIntensity <= 0.01f) {
            blurSamples  = 0;
            blurStretch  = 0.0;
            blurStrength = 0f;
            return;
        }
        blurSamples  = Math.max(1, Math.round(motionBlurIntensity * 5f)); // 1..5
        blurStretch  = 1.0 + motionBlurIntensity * 2.0;                    // 1.0..3.0
        blurStrength = 0.10f + motionBlurIntensity * 0.45f;                // 0.10..0.55
    }

    // -------- Lapset --------
    public void add(UiComponent c) { children.add(c); updateContentHeight(); }
    public void clear() {
        children.clear();
        scrollY = velocity = prevScrollY = 0;
        tweenTarget = null;
        updateContentHeight();
    }
    public List<UiComponent> getChildren() { return children; }

    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect b) { this.bounds = b; updateContentHeight(); }
    @Override public int getPreferredHeight() { return 120; }

    // ================================================================
    //  Layout
    // ================================================================
    private int maxScroll() {
        int viewH = Math.max(0, bounds.h - innerPadding * 2);
        return Math.max(0, contentHeight - viewH);
    }
    private void updateContentHeight() {
        int total = innerPadding;
        for (UiComponent c : children) total += c.getPreferredHeight() + gap;
        total += innerPadding;
        contentHeight = total;
    }
    private void layoutChildrenAt(double extraOffset) {
        int x = bounds.x + innerPadding;
        int y = bounds.y + innerPadding - (int) Math.round(scrollY + extraOffset);
        int w = Math.max(1, bounds.w - innerPadding * 2);
        for (UiComponent c : children) {
            int h = c.getPreferredHeight();
            c.setBounds(new Rect(x, y, w, h));
            y += h + gap;
        }
    }

    private Rect getScrollbarTrackRect() {
        int tx = bounds.right() - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        int ty = bounds.y + SCROLLBAR_PADDING;
        int th = bounds.h - SCROLLBAR_PADDING * 2;
        return new Rect(tx, ty, SCROLLBAR_WIDTH, th);
    }
    private Rect getScrollbarThumbRect() {
        int max = maxScroll();
        if (max <= 0) return new Rect(0, 0, 0, 0);
        Rect t = getScrollbarTrackRect();
        int thumbH = Math.max(14, (int) ((t.h * (float) bounds.h / contentHeight)));
        int thumbY = t.y + (int) ((t.h - thumbH) * (scrollY / (float) max));
        return new Rect(t.x, thumbY, t.w, thumbH);
    }

    // ================================================================
    //  Fysiikka – momentum + HARD STOP
    // ================================================================
    private void stepPhysics(float delta) {
        float dt = delta * 0.05f;
        if (dt <= 0f)  dt = 1f / 60f;
        if (dt > 0.1f) dt = 0.1f;

        int max = maxScroll();

        if (tweenTarget != null) {
            double clampedTarget = Math.max(0, Math.min(max, tweenTarget));
            double diff = clampedTarget - scrollY;
            if (Math.abs(diff) < 0.25 && Math.abs(velocity) < 1.0) {
                scrollY = clampedTarget;
                velocity = 0;
                tweenTarget = null;
            } else {
                double t = 1.0 - Math.exp(-tweenRate * dt);
                scrollY += diff * t;
                velocity = 0;
            }
            if (scrollY < 0)   { scrollY = 0;   tweenTarget = null; }
            if (scrollY > max) { scrollY = max; tweenTarget = null; }
            return;
        }

        scrollY += velocity * dt;

        if (scrollY <= 0) {
            scrollY = 0;
            if (velocity < 0) velocity = 0;
        } else if (scrollY >= max) {
            scrollY = max;
            if (velocity > 0) velocity = 0;
        } else {
            velocity *= Math.exp(-friction * dt);
            if (Math.abs(velocity) < VELOCITY_EPSILON) velocity = 0;
        }
    }

    // ================================================================
    //  Render
    // ================================================================
    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        updateContentHeight();
        stepPhysics(delta);

        double deltaY = scrollY - prevScrollY;
        double motion = Math.abs(deltaY);

        ui.enableScissor(bounds.x, bounds.y, bounds.w, bounds.h);

        if (blurSamples > 0 && motion > BLUR_MIN_DELTA) {
            renderWithMotionBlur(ui, mouseX, mouseY, delta, deltaY);
        } else {
            layoutChildrenAt(0.0);
            renderVisibleChildren(ui, mouseX, mouseY, delta);
        }

        ui.disableScissor();

        prevScrollY = scrollY;

        int max = maxScroll();
        if (showScrollBar && max > 0 && (bounds.contains(mouseX, mouseY) || draggingScrollbar)) {
            Rect thumb = getScrollbarThumbRect();
            ui.fillRounded(thumb.x, thumb.y, thumb.w, thumb.h,
                    ui.theme.accent, thumb.w / 2.0);
        }
    }

    private void renderWithMotionBlur(UiContext ui, int mouseX, int mouseY,
                                      float delta, double deltaY) {
        int panelRgb  = ui.theme.panel & 0x00FFFFFF;
        int fadeAlpha = (int) (blurStrength * 255f);
        int fadeArgb  = new Color(panelRgb).withAlpha(fadeAlpha).getARGB();

        for (int i = blurSamples; i >= 1; i--) {
            double t = (double) i / blurSamples;
            double ghostOffset = -deltaY * blurStretch * t;

            layoutChildrenAt(ghostOffset);
            renderVisibleChildren(ui, mouseX, mouseY, delta);
            ui.fill(bounds.x, bounds.y, bounds.w, bounds.h, fadeArgb);
        }

        layoutChildrenAt(0.0);
        renderVisibleChildren(ui, mouseX, mouseY, delta);
    }

    private void renderVisibleChildren(UiContext ui, int mouseX, int mouseY, float delta) {
        for (UiComponent c : children) {
            Rect r = c.getBounds();
            if (r.y + r.h > bounds.y && r.y < bounds.y + bounds.h) {
                c.render(ui, mouseX, mouseY, delta);
            }
        }
    }

    // ================================================================
    //  Input
    // ================================================================
    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (!bounds.contains(mouseX, mouseY)) return false;

        int max = maxScroll();
        if (max <= 0) return true;

        if (scrollY <= 0 && amount > 0) return true;
        if (scrollY >= max && amount < 0) return true;

        tweenTarget = null;
        velocity -= amount * impulsePerTick;
        return true;
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0 || !bounds.contains(mouseX, mouseY)) return false;

        Rect thumb = getScrollbarThumbRect();
        if (thumb.contains(mouseX, mouseY)) {
            draggingScrollbar = true;
            dragStartY   = (int) mouseY;
            scrollStartY = scrollY;
            velocity = 0;
            tweenTarget = null;
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
        if (button == 0) draggingScrollbar = false;
        for (UiComponent c : children) c.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY,
                                int button, double dx, double dy) {
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
                    scrollY = scrollStartY + (pct * max);
                    if (scrollY < 0)   scrollY = 0;
                    if (scrollY > max) scrollY = max;
                    prevScrollY = scrollY;
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

    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int mods) {
        for (UiComponent c : children) if (c.keyPressed(ui, keyCode, scanCode, mods)) return true;
        return false;
    }
    @Override public boolean charTyped(UiContext ui, char chr, int mods) {
        for (UiComponent c : children) if (c.charTyped(ui, chr, mods)) return true;
        return false;
    }

    // ================================================================
    //  Julkinen API
    // ================================================================
    public void scrollTo(int y) {
        int max = maxScroll();
        tweenTarget = (double) Math.max(0, Math.min(max, y));
        velocity = 0;
    }

    public void scrollToImmediate(int y) {
        int max = maxScroll();
        scrollY = Math.max(0, Math.min(max, y));
        prevScrollY = scrollY;
        velocity = 0;
        tweenTarget = null;
    }
}