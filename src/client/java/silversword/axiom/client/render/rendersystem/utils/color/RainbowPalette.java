package silversword.axiom.client.render.rendersystem.utils.color;

import silversword.axiom.client.render.rendersystem.utils.color.Color;

public class RainbowPalette {
    private final String name;
    private final int[] colors; // ARGB-värit

    public RainbowPalette(String name, int[] colors) {
        this.name = name;
        this.colors = colors;
    }

    public String getName() { return name; }
    public int[] getColors() { return colors; }
    public int getColorCount() { return colors.length; }

    // Hae väri indeksillä (moduloidaan pituudella)
    public int getColor(int index) {
        if (colors.length == 0) return 0xFFFFFFFF;
        return colors[Math.abs(index) % colors.length];
    }

    // Hae väri tietyn ajanhetken ja paikan perusteella
    public int getColorForPosition(long time, float speed, int charIndex, int rowIndex, float xPos, float yPos) {
        // Lasketaan indeksi: aika + merkkipaikka + rivi
        long period = (long)(2000 / speed); // nopeus vaikuttaa vaihteluväliin
        int idx = (int)((time / period) + charIndex * 2 + rowIndex * 3);
        return getColor(idx);
    }

    public Color getColorObject(int index) {
        return new Color(getColor(index));
    }
}