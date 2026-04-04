package silversword.axiom.client.render.rendersystem.utils.color.rainbow;

import silversword.axiom.client.render.rendersystem.utils.color.Color;

public class RainbowPalette {
    private final String name;
    private final int[] colors;

    public RainbowPalette(String name, int[] colors) {
        this.name = name;
        this.colors = colors;
    }

    public String getName() { return name; }
    public int[] getColors() { return colors; }
    public int getColorCount() { return colors.length; }

    public int getColor(int index) {
        if (colors.length == 0) return 0xFFFFFFFF;
        return colors[Math.abs(index) % colors.length];
    }

    public int getColorInterpolatedLoop(float t) {
        if (colors.length == 0) return 0xFFFFFFFF;
        if (t <= 0) return colors[0];
        if (t >= 1) return colors[0];

        float scaled = t * colors.length;
        int idx = (int) scaled;
        float frac = scaled - idx;

        if (idx >= colors.length - 1) {
            int color1 = colors[colors.length - 1];
            int color2 = colors[0];
            return interpolateColor(color1, color2, frac);
        } else {
            return interpolateColor(colors[idx], colors[idx + 1], frac);
        }
    }

    public int getColorForPosition(long time, float speed, int charIndex, int rowIndex, float xPos, float yPos) {
        float period = 5000f / speed; // ms per täysi sykli
        float timePos = (time % (long)period) / period; // 0..1
        float xOffset = charIndex * 12f / 360f;
        float yOffset = rowIndex * 18f / 360f;
        float t = (timePos + xOffset + yOffset) % 1.0f;
        return getColorInterpolatedLoop(t);
    }

    private int interpolateColor(int color1, int color2, float frac) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * frac);
        int g = (int)(g1 + (g2 - g1) * frac);
        int b = (int)(b1 + (b2 - b1) * frac);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public Color getColorObject(int index) {
        return new Color(getColor(index));
    }
}