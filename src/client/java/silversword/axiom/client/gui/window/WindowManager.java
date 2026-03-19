package silversword.axiom.client.gui.window;

import silversword.axiom.client.gui.components.ModuleListView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.DragState;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.main.AxiomMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WindowManager {

    private final List<Window> windows = new ArrayList<>();
    private final List<Window> overlayStack = new ArrayList<>();
    private final List<Window> toRemove = new ArrayList<>();

    private Window currentOverlay = null;
    private UiComponent overlayComponent = null;

    public void updateAnimations() {
        toRemove.clear();
        for (Window w : windows) {
            w.updateAnimation();
            if (w.isClosing() && w.getAnimProgress() == 0) {
                toRemove.add(w);
            }
        }

        // Remove finished windows and handle overlay stack
        for (Window w : toRemove) {
            windows.remove(w);
            if (w == currentOverlay) {
                // Current overlay has finished closing
                if (!overlayStack.isEmpty()) {
                    currentOverlay = overlayStack.remove(overlayStack.size() - 1);
                } else {
                    currentOverlay = null;
                }
            }
        }
        toRemove.clear();
    }

    public void clear() {
        windows.clear();
        currentOverlay = null;
        overlayComponent = null;
        overlayStack.clear();
    }

    public void openOverlay(Window w) {
        if (currentOverlay != null) {
            overlayStack.add(currentOverlay);
        }
        currentOverlay = w;
        // Ensure the window is in the main list (should already be)
        if (!windows.contains(w)) {
            windows.add(w);
        }
        overlayComponent = null;
    }

    public void addWindowAsOverlay(UiComponent c) {
        overlayComponent = c;
        currentOverlay = null;
    }

    public void closeOverlay() {
        if (currentOverlay != null) {
            currentOverlay.close(); // start closing animation
            // Do not remove yet; will be removed after animation in updateAnimations
        }
        // If there's a component overlay, just clear it immediately
        if (overlayComponent != null) {
            overlayComponent = null;
        }
    }

    public boolean isOverlayOpen() {
        return currentOverlay != null || overlayComponent != null;
    }

    public void add(Window window) {
        if (window == null) return;
        windows.add(window);
    }

    public List<Window> getWindows() {
        return Collections.unmodifiableList(windows);
    }

    public Window getWindowById(String id) {
        if (id == null) return null;
        for (Window w : windows) {
            if (id.equals(w.id)) return w;
        }
        return null;
    }

    public boolean removeWindow(Window window) {
        if (window == null) return false;
        window.close(); // start closing animation
        return true;
    }

    public boolean removeWindowById(String id) {
        Window w = getWindowById(id);
        if (w != null) {
            return removeWindow(w);
        }
        return false;
    }

    public void bringToFront(Window w) {
        if (w == null) return;
        int idx = windows.indexOf(w);
        if (idx < 0 || idx == windows.size() - 1) return;
        windows.remove(idx);
        windows.add(w);
    }

    public void render(UiContext ui, int mouseX, int mouseY) {
        if (currentOverlay != null) {
            currentOverlay.render(ui, mouseX, mouseY, ui.delta);
            return;
        }
        if (overlayComponent != null) {
            overlayComponent.render(ui, mouseX, mouseY, ui.delta);
            return;
        }
        for (Window w : windows) {
            w.render(ui, mouseX, mouseY, ui.delta);
        }
    }

    public void openOverlayAsWindow(UiComponent component) {
        Window w = new Window("overlay", "", 0, 0, 0, 0);
        w.setUserData(component);
        openOverlay(w);
    }

    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (currentOverlay != null) return currentOverlay.mouseClicked(ui, mouseX, mouseY, button);
        if (overlayComponent != null) return overlayComponent.mouseClicked(ui, mouseX, mouseY, button);

        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.getBounds().contains(mouseX, mouseY)) {
                bringToFront(w);
                return w.mouseClicked(ui, mouseX, mouseY, button);
            }
        }
        return false;
    }

    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        for (Window w : windows) {
            w.mouseReleased(ui, mouseX, mouseY, button);
        }
        if (button == 0 && DragState.isActive()) {
            handleDrop(mouseX, mouseY);
            DragState.stop();
        }
    }

    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (currentOverlay != null) return currentOverlay.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
        if (overlayComponent != null) return overlayComponent.mouseDragged(ui, mouseX, mouseY, button, dx, dy);

        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.mouseDragged(ui, mouseX, mouseY, button, dx, dy)) return true;
        }
        return false;
    }

    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (currentOverlay != null) return currentOverlay.mouseScrolled(ui, mouseX, mouseY, amount);
        if (overlayComponent != null) return overlayComponent.mouseScrolled(ui, mouseX, mouseY, amount);

        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.getBounds().contains(mouseX, mouseY)) return w.mouseScrolled(ui, mouseX, mouseY, amount);
        }
        return false;
    }

    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        if (currentOverlay != null) return currentOverlay.keyPressed(ui, keyCode, scanCode, modifiers);
        if (overlayComponent != null) return overlayComponent.keyPressed(ui, keyCode, scanCode, modifiers);

        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.isMinimized()) continue;
            if (w.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        if (currentOverlay != null) return currentOverlay.charTyped(ui, chr, modifiers);
        if (overlayComponent != null) return overlayComponent.charTyped(ui, chr, modifiers);

        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.isMinimized()) continue;
            if (w.charTyped(ui, chr, modifiers)) return true;
        }
        return false;
    }

    // --- Drop logic (unchanged) ---
    private void handleDrop(double mouseX, double mouseY) {
        if (!DragState.isActive()) return;

        AxiomMod module = DragState.getModule();
        String sourceId = DragState.getSourceWindowId();
        int sourceIndex = DragState.getSourceIndex();

        Window targetWindow = getTopmostWindowAt(mouseX, mouseY);
        if (targetWindow == null) return;

        Object userData = targetWindow.getUserData();
        if (!(userData instanceof ModuleWindowData targetData)) return;

        if (sourceId.equals(targetWindow.id)) {
            return;
        }

        Window sourceWindow = getWindowById(sourceId);
        if (sourceWindow != null && sourceWindow.getUserData() instanceof ModuleWindowData sourceData) {
            sourceData.remove(module.getId());
        }

        int dropIndex = getDropIndex(targetWindow, mouseX, mouseY);
        targetData.insertAt(module.getId(), dropIndex);
    }

    private Window getTopmostWindowAt(double x, double y) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            Window w = windows.get(i);
            if (w.getBounds().contains(x, y)) return w;
        }
        return null;
    }

    private int getDropIndex(Window target, double mouseX, double mouseY) {
        for (UiComponent comp : target.getChildren()) {
            if (comp instanceof ModuleListView listView) {
                return listView.getDropIndexAt(mouseX, mouseY);
            }
        }
        return 0;
    }

    public void applyWindowDefaults() {}
}