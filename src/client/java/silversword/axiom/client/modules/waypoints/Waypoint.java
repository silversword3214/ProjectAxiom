package silversword.axiom.client.modules.waypoints;

import java.util.UUID;

public class Waypoint {
    public final UUID id;
    public String name;
    public double x, y, z;
    public int color;          // pääväri
    public boolean enabled;

    // Uudet kentät
    public int bgColor;
    public int outlineColor;
    public boolean showBg;
    public boolean showOutline;
    public String shape;       // "Circle", "Square", "Rounded"
    public double scale;       // yksilöllinen skaala

    // Konstruktori uusille waypointeille (oletusarvot)
    public Waypoint(String name, double x, double y, double z, int color) {
        this(UUID.randomUUID(), name, x, y, z, color, true,
                0x64000000, 0xFFFFFFFF, true, true, "Rounded", 1.0);
    }

    // Täydellinen konstruktori (myös deserialisointiin)
    public Waypoint(UUID id, String name, double x, double y, double z, int color, boolean enabled,
                    int bgColor, int outlineColor, boolean showBg, boolean showOutline, String shape, double scale) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.enabled = enabled;
        this.bgColor = bgColor;
        this.outlineColor = outlineColor;
        this.showBg = showBg;
        this.showOutline = showOutline;
        this.shape = shape;
        this.scale = scale;
    }
}