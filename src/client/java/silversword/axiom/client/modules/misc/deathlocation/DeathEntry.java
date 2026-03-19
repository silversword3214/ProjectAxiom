package silversword.axiom.client.modules.misc.deathlocation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeathEntry {
    public final double x, y, z;
    public final String timestamp;
    public final String world; // dimension nimi

    public DeathEntry(double x, double y, double z, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy"));
    }

    public String getShortLocation() {
        return String.format("%.0f %.0f %.0f", x, y, z);
    }
}