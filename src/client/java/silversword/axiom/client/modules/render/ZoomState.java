package silversword.axiom.client.modules.render;

public final class ZoomState {
    private ZoomState() {}

    // Onko zoom parhaillaan aktiivinen (näppäin pohjassa)
    public static volatile boolean zooming = false;

    // Kerroin jolla FOV jaetaan (1.0 = ei zoomia, 4.0 = 4× zoom)
    public static volatile double currentMultiplier = 1.0;
    public static volatile double targetMultiplier  = 1.0;

    // Scrollilla säädetty lisäkerroin (1.0 = ei muutosta)
    public static volatile double scrollZoom = 1.0;

    // Pehmeä siirtymä
    public static volatile boolean smoothCamera = true;
    public static volatile double  smoothSpeed  = 15.0;

    // Herkkyys
    public static volatile boolean reduceSensitivity = true;
    public static volatile double  sensitivityFactor = 0.5;
}