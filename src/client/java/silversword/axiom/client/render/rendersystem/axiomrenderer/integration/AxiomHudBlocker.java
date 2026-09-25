package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

/**
 * Yksi totuuden lähde: onko jokin Axiom-screen auki?
 * Mixin päivittää tämän automaattisesti kun Gui.setScreen() kutsutaan.
 */
public final class AxiomHudBlocker {
    private static volatile boolean screenOpen = false;

    private AxiomHudBlocker() {}

    public static void setScreenOpen(boolean open) { screenOpen = open; }
    public static boolean isScreenOpen() { return screenOpen; }
}