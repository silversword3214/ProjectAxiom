package silversword.axiom.client.rendersystem.utils.misc;

public final class AxiomHudBlocker {
    private static volatile boolean screenOpen = false;

    private AxiomHudBlocker() {}

    public static void setScreenOpen(boolean open) { screenOpen = open; }
    public static boolean isScreenOpen() { return screenOpen; }
}