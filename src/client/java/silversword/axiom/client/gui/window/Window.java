package silversword.axiom.client.gui.window;

import net.minecraft.util.Identifier;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.utils.render.DrawTexture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Window {

    public final String id;
    private String title;

    public int x, y, width, height;

    private int restoreHeight = -1;
    private boolean minimized = false;
    private boolean dragging = false;
    private boolean resizing = false;
    private int dragOffX = 0;
    private int dragOffY = 0;

    private boolean minimizable = true;
    private boolean closable = true;
    private Runnable onClose = null;

    private static final int RESIZE_HANDLE = 10;

    private boolean autoLayoutVertical = true;
    private int padding = 6;
    private int gap = 6;
    private boolean hasKeybind = false;
    private Runnable onKeybindClick = null;
    private int headerH = 18;

    private final List<UiComponent> children = new ArrayList<>();
    private Object userData = null;
    private UiComponent draggingChild = null;

    // Animation fields
    public enum AnimState { NONE, OPENING, CLOSING }
    private AnimState animState = AnimState.NONE;
    private float animProgress = 1.0f;
    private float animTarget = 1.0f;
    private long animStartTime = 0;

    private boolean minimizeAnimating = false;
    private float minimizeAnimProgress = 0f;
    private long minimizeAnimStartTime = 0;

    private static final long ANIM_DURATION = 700; // milliseconds
    private static final long MINIMIZE_ANIM_DURATION = 500;
    private boolean ignoreScale = false;

    private static final Identifier KEYBIND_TEXTURE = Identifier.of("projectaxiom", "textures/icons/keybind.png");

    public void setIgnoreScale(boolean ignore) {
        this.ignoreScale = ignore;
    }




    public Window(String id, String title, int x, int y, int width, int height) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.x = x;
        this.y = y;
        this.width = Math.max(120, width);
        this.height = Math.max(40, height);
    }

    public void open() {
        animState = AnimState.OPENING;
        animProgress = 0.0f;
        animTarget = 1.0f;
        animStartTime = System.currentTimeMillis();
    }

    public void close() {
        animState = AnimState.CLOSING;
        animTarget = 0.0f;
        animStartTime = System.currentTimeMillis();
    }


    public boolean isAnimating() { return animState != AnimState.NONE; }
    public float getAnimProgress() { return animProgress; }
    public boolean isClosing() { return animState == AnimState.CLOSING; }

    // ---------------------------
    // Public API
    // ---------------------------

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }
    public Object getUserData() { return userData; }
    public void setUserData(Object userData) { this.userData = userData; }
    public boolean isMinimized() { return minimized; }
    public boolean isMinimizable() { return minimizable; }
    public void setMinimizable(boolean minimizable) { this.minimizable = minimizable; }
    public boolean isClosable() { return closable; }
    public void setClosable(boolean closable) { this.closable = closable; }
    public void setOnClose(Runnable onClose) { this.onClose = onClose; }
    public void setMinimized(boolean value) { if (!minimizable) return; if (this.minimized == value) return; toggleMinimize(); }
    public void setHasKeybind(boolean hasKeybind) { this.hasKeybind = hasKeybind; }
    public void setOnKeybindClick(Runnable onKeybindClick) { this.onKeybindClick = onKeybindClick; }
    public void setAutoLayoutVertical(boolean enabled) { this.autoLayoutVertical = enabled; }
    public void setPadding(int padding) { this.padding = Math.max(0, padding); }
    public void setGap(int gap) { this.gap = Math.max(0, gap); }
    public void setHeaderHeight(int headerH) { this.headerH = Math.max(14, headerH); if (minimized) height = this.headerH + 2; }
    public void add(UiComponent component) { if (component != null) children.add(component); }
    public void clearChildren() { children.clear(); }
    public List<UiComponent> getChildren() { return Collections.unmodifiableList(children); }
    public Rect getBounds() { return new Rect(x, y, width, height); }

    public Rect getContentBounds() {
        int cx = x + padding;
        int cy = y + headerH + padding;
        int cw = width - padding * 2;
        int ch = height - headerH - padding * 2;
        return new Rect(cx, cy, Math.max(1, cw), Math.max(1, ch));



    }

    public void updateAnimation() {
        if (animState == AnimState.NONE) return;
        long elapsed = System.currentTimeMillis() - animStartTime;
        float t = Math.min(1.0f, (float) elapsed / ANIM_DURATION);
        // Ease out quadratic
        float eased = 1 - (1 - t) * (1 - t);
        animProgress = animProgress + (animTarget - animProgress) * eased;
        if (elapsed >= ANIM_DURATION) {
            animProgress = animTarget;
            animState = AnimState.NONE;
        }
    }

    public void toggleMinimize() {
        if (!minimizable) return;
        if (minimizeAnimating) return;

        // Tallenna nykyinen korkeus ennen tilan vaihtoa
        if (!minimized) {
            // menossa minimoituun
            restoreHeight = height;
        } else {
            // menossa auki - restoreHeight on jo tallennettu
        }

        minimized = !minimized;
        minimizeAnimating = true;
        minimizeAnimStartTime = System.currentTimeMillis();
    }
    private void updateMinimizeAnimation() {
        if (!minimizeAnimating) return;

        long elapsed = System.currentTimeMillis() - minimizeAnimStartTime;
        float t = Math.min(1.0f, (float) elapsed / MINIMIZE_ANIM_DURATION);
        float eased = 1 - (1 - t) * (1 - t); // ease out quadratic

        int startH, targetH;
        if (minimized) {
            // Oltiin auki, menossa kiinni: start = restoreHeight, target = headerH+2
            startH = restoreHeight;
            targetH = headerH + 2;
        } else {
            // Oltiin kiinni, menossa auki: start = headerH+2, target = restoreHeight
            startH = headerH + 2;
            targetH = restoreHeight;
        }

        height = (int) (startH + (targetH - startH) * eased);

        if (elapsed >= MINIMIZE_ANIM_DURATION) {
            minimizeAnimating = false;
            height = targetH; // varmistetaan loppuarvo
        }
    }


    // ---------------------------
    // Rendering with animation
    // ---------------------------

    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        updateAnimation();
        updateMinimizeAnimation(); // <-- lisää tämä

        if (animProgress == 0 && animState == AnimState.NONE) return;

        float progress = getAnimProgress();
        int screenHeight = ui.mc.getWindow().getScaledHeight();

        int renderY;
        if (animState == AnimState.OPENING) {
            renderY = (int) (y + (screenHeight - y) * (1 - progress));
        } else if (animState == AnimState.CLOSING) {
            renderY = (int) (y - (y + height) * (1 - progress));
        } else {
            renderY = y;
        }

        width = Math.max(140, width);
        height = Math.max(minimized ? (headerH + 2) : 60, height); // tämä varmistaa minimikorkeuden

        Rect drawWin = new Rect(x, renderY, width, height);
        ui.fillRounded(drawWin, ui.theme.panel, ui.theme.radius);

        Rect header = new Rect(x + 1, renderY + 1, width - 2, headerH);
        int transparentHeader = (ui.theme.header & 0x00FFFFFF) | 0x20000000;
        ui.fillRoundedCustom(header, transparentHeader, ui.theme.radius, true, true, false, false);

        // Piirrä erotinviiva vain jos ikkuna on täysin auki tai animoitumassa auki
        if (height > headerH + 4) { // jos korkeus on suurempi kuin minimi + pieni toleranssi
            int sepY = header.bottom();
            int sepX = x;
            int sepW = width;
            ui.fill(sepX, sepY, sepW, 2, ui.theme.accent);
        }

        int titleY = header.y + header.h / 2 - ui.fontHeight() / 2 + 4;
        ui.text(title, header.x + ui.theme.innerPadding, titleY, ui.theme.text);

        int btnIndex = 0;
        if (closable) {
            Rect xBtn = headerButtonRect(btnIndex++, header);
            boolean hover = xBtn.contains(mouseX, mouseY);
            ui.fillRounded(xBtn, hover ? ui.theme.buttonHover : ui.theme.button, 3);
            ui.text("x", xBtn.x + 5, xBtn.y + 4, ui.theme.textDim);
        }
        if (hasKeybind) {
            Rect keyBtn = headerButtonRect(btnIndex++, header);
            boolean hover = keyBtn.contains(mouseX, mouseY);
            ui.fillRounded(keyBtn, hover ? ui.theme.buttonHover : ui.theme.button, 3);

            // Piirrä tekstuuri napin keskelle
            int iconSize = 10;
            int iconX = keyBtn.x + (keyBtn.w - iconSize) / 2;
            int iconY = keyBtn.y + (keyBtn.h - iconSize) / 2;
            DrawTexture.add(KEYBIND_TEXTURE, iconX, iconY, iconSize, iconSize, new Color(255, 255, 255, 255));
        }
        if (minimizable) {
            Rect minBtn = headerButtonRect(btnIndex++, header);
            boolean hover = minBtn.contains(mouseX, mouseY);
            ui.fillRounded(minBtn, hover ? ui.theme.buttonHover : ui.theme.button, 3);
            ui.text(minimized ? "+" : "-", minBtn.x + 6, minBtn.y + 4, ui.theme.textDim);
        }

        // Piirrä resize-kahva vain jos ikkuna on täysin auki
        if (height > headerH + 4) {
            Rect rh = resizeRect(renderY);
            boolean rHover = rh.contains(mouseX, mouseY);
            ui.fillRounded(rh, rHover ? ui.theme.buttonHover : ui.theme.border, 3);
        }

        // Piirrä children vain jos ikkuna on auki (korkeus > minimi)
        if (height > headerH + 4) {
            layoutChildren(ui, renderY);
            for (UiComponent c : children) {
                c.render(ui, mouseX, mouseY, delta);
            }
        }
    }

    private Rect headerButtonRect(int indexFromRight, Rect header) {
        int bw = 16, bh = 14, rightPad = 4, spacing = 2;
        int bx = header.right() - rightPad - bw - (indexFromRight * (bw + spacing));
        int by = header.y + 2;
        return new Rect(bx, by, bw, bh);
    }

    private Rect resizeRect(int renderY) {
        return new Rect(x + width - RESIZE_HANDLE, renderY + height - RESIZE_HANDLE, RESIZE_HANDLE, RESIZE_HANDLE);
    }

    private void layoutChildren(UiContext ui, int renderY) {
        if (children.isEmpty()) return;
        int contentX = x + padding;
        int contentY = renderY + headerH + padding;
        int contentW = width - padding * 2;
        int contentH = height - headerH - padding * 2;
        Rect content = new Rect(contentX, contentY, Math.max(1, contentW), Math.max(1, contentH));
        int yy = content.y;
        for (int i = 0; i < children.size(); i++) {
            UiComponent c = children.get(i);
            int pref = Math.max(1, c.getPreferredHeight());
            boolean last = (i == children.size() - 1);
            int hh = last ? Math.max(1, content.bottom() - yy) : pref;
            c.setBounds(new Rect(content.x, yy, content.w, hh));
            yy += hh + gap;
        }
    }

    // ---------------------------
    // Input handling (unchanged)
    // ---------------------------
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        Rect win = getBounds();
        if (!win.contains(mouseX, mouseY)) return false;
        if (button == 0) {
            int btnIndex = 0;
            if (closable && headerButtonRect(btnIndex++, headerRect()).contains(mouseX, mouseY)) {
                if (onClose != null) onClose.run();
                return true;
            }
            if (hasKeybind && headerButtonRect(btnIndex++, headerRect()).contains(mouseX, mouseY)) {
                if (onKeybindClick != null) onKeybindClick.run();
                return true;
            }
            if (minimizable && headerButtonRect(btnIndex++, headerRect()).contains(mouseX, mouseY)) {
                toggleMinimize();
                return true;
            }
        }
        if (headerRect().contains(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragOffX = (int) mouseX - x;
            dragOffY = (int) mouseY - y;
            return true;
        }
        if (!minimized && resizeRect(y).contains(mouseX, mouseY) && button == 0) {
            resizing = true;
            return true;
        }
        if (minimized) return true;
        if (autoLayoutVertical) layoutChildren(ui, y);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiComponent c = children.get(i);
            if (c.getBounds().contains(mouseX, mouseY)) {
                if (c.mouseClicked(ui, mouseX, mouseY, button)) {
                    draggingChild = c;
                    return true;
                }
            }
        }
        return true;
    }

    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        dragging = false; resizing = false;
        if (draggingChild != null) {
            draggingChild.mouseReleased(ui, mouseX, mouseY, button);
            draggingChild = null;
        }
        if (minimized) return;
        if (autoLayoutVertical) layoutChildren(ui, y);
        for (UiComponent c : children) c.mouseReleased(ui, mouseX, mouseY, button);
    }

    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) { x = (int) mouseX - dragOffX; y = (int) mouseY - dragOffY; return true; }
        if (resizing && !minimized) {
            int newW = (int) mouseX - x, newH = (int) mouseY - y;
            width = Math.max(140, newW); height = Math.max(60, newH);
            return true;
        }
        if (minimized) return false;
        if (autoLayoutVertical) layoutChildren(ui, y);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiComponent c = children.get(i);
            if (c.getBounds().contains(mouseX, mouseY) && c.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        }
        return false;
    }

    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (minimized) return false;
        Rect win = getBounds();
        if (!win.contains(mouseX, mouseY)) return false;
        if (autoLayoutVertical) layoutChildren(ui, y);
        for (int i = children.size() - 1; i >= 0; i--) {
            UiComponent c = children.get(i);
            if (c.getBounds().contains(mouseX, mouseY) && c.mouseScrolled(ui, mouseX, mouseY, amount)) return true;
        }
        return true;
    }

    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (minimized) return false;
        for (UiComponent c : children) if (c.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        return false;
    }

    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (minimized) return false;
        for (UiComponent c : children) if (c.charTyped(ui, chr, modifiers)) return true;
        return false;
    }

    // Geometry helpers
    private Rect headerRect() { return new Rect(x + 1, y + 1, width - 2, headerH); }
    public void setBounds(Rect rect) {
        this.x = rect.x; this.y = rect.y;
        this.width = Math.max(140, rect.w);
        this.height = Math.max(minimized ? (headerH + 2) : 60, rect.h);
    }
}