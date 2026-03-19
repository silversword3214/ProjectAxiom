package silversword.axiom.client.gui.core;

import silversword.axiom.client.main.AxiomMod;

/**
 * Global drag state for GUI.
 * One active drag at a time.
 */
public final class DragState {

    private static boolean active = false;

    private static AxiomMod module;
    private static String sourceWindowId;
    private static int sourceIndex;

    private static double startX;
    private static double startY;

    private DragState() {}

    public static void start(AxiomMod mod, String windowId, int index, double mouseX, double mouseY) {
        active = true;
        module = mod;
        sourceWindowId = windowId;
        sourceIndex = index;
        startX = mouseX;
        startY = mouseY;
    }

    public static void stop() {
        active = false;
        module = null;
        sourceWindowId = null;
        sourceIndex = -1;
        startX = 0;
        startY = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static AxiomMod getModule() {
        return module;
    }

    public static String getSourceWindowId() {
        return sourceWindowId;
    }

    public static int getSourceIndex() {
        return sourceIndex;
    }

    public static double getStartX() {
        return startX;
    }

    public static double getStartY() {
        return startY;
    }
}
