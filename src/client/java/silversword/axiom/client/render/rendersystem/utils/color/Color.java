package silversword.axiom.client.render.rendersystem.utils.color;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a color with red, green, blue, and alpha components (0-255).
 */
public class Color {

    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color BLACK = new Color(0, 0, 0, 255);
    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color GREEN = new Color(0, 255, 0, 255);
    public static final Color BLUE = new Color(0, 0, 255, 255);
    public static final Color YELLOW = new Color(255, 255, 0, 255);
    public static final Color CYAN = new Color(0, 255, 255, 255);
    public static final Color MAGENTA = new Color(255, 0, 255, 255);
    public static final Color ORANGE = new Color(255, 165, 0, 255);
    public static final Color GRAY = new Color(128, 128, 128, 255);
    public static final Color DARK_GRAY = new Color(64, 64, 64, 255);
    public static final Color LIGHT_GRAY = new Color(192, 192, 192, 255);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public int r, g, b, a;

    public Color(int r, int g, int b, int a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int argb) {
        this.a = (argb >> 24) & 0xFF;
        this.r = (argb >> 16) & 0xFF;
        this.g = (argb >> 8) & 0xFF;
        this.b = argb & 0xFF;
    }

    public Color(float r, float g, float b, float a) {
        this((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255));
    }

    public Color(float r, float g, float b) {
        this(r, g, b, 1.0f);
    }

    public Color(Color other) {
        this(other.r, other.g, other.b, other.a);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Sets all components and returns this (for chaining).
     */
    public Color set(int r, int g, int b, int a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
        return this;
    }

    /**
     * Creates a copy of this color.
     */
    public Color copy() {
        return new Color(this);
    }

    /**
     * Saves this color to an NBT compound tag.
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("r", r);
        tag.putInt("g", g);
        tag.putInt("b", b);
        tag.putInt("a", a);
        return tag;
    }

    /**
     * Loads this color from an NBT compound tag and returns this.
     */
    public Color fromTag(CompoundTag tag) {
        this.r = tag.getInt("r").orElse(0);
        this.g = tag.getInt("g").orElse(0);
        this.b = tag.getInt("b").orElse(0);
        this.a = tag.getInt("a").orElse(255);
        return this;
    }

    /**
     * Converts this color to HSV components.
     * @return float array: [hue (0-360), saturation (0-1), value (0-1)]
     */
    public float[] toHsv() {
        float rNorm = r / 255f;
        float gNorm = g / 255f;
        float bNorm = b / 255f;
        float cmax = Math.max(rNorm, Math.max(gNorm, bNorm));
        float cmin = Math.min(rNorm, Math.min(gNorm, bNorm));
        float delta = cmax - cmin;
        float hue = 0;
        if (delta != 0) {
            if (cmax == rNorm) {
                hue = 60 * ((gNorm - bNorm) / delta % 6);
            } else if (cmax == gNorm) {
                hue = 60 * ((bNorm - rNorm) / delta + 2);
            } else {
                hue = 60 * ((rNorm - gNorm) / delta + 4);
            }
        }
        if (hue < 0) hue += 360;
        float saturation = cmax == 0 ? 0 : delta / cmax;
        float value = cmax;
        return new float[]{hue, saturation, value};
    }

    public int getARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getRGB() {
        return (r << 16) | (g << 8) | b;
    }


    public int getAlpha() {
        return a;
    }

    public Color setARGB(int argb) {
        this.a = (argb >> 24) & 0xFF;
        this.r = (argb >> 16) & 0xFF;
        this.g = (argb >> 8) & 0xFF;
        this.b = argb & 0xFF;
        return this;
    }

    public Color setRGB(int rgb) {
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
        return this;
    }

    public Color withAlpha(int alpha) {
        return new Color(r, g, b, alpha);
    }

    public Color withRed(int red) {
        return new Color(red, g, b, a);
    }

    public Color withGreen(int green) {
        return new Color(r, green, b, a);
    }

    public Color withBlue(int blue) {
        return new Color(r, g, blue, a);
    }

    public Color multiply(float factor) {
        return new Color((int)(r * factor), (int)(g * factor), (int)(b * factor), a);
    }

    public Color multiply(float factor, boolean multiplyAlpha) {
        int newAlpha = multiplyAlpha ? (int)(a * factor) : a;
        return new Color((int)(r * factor), (int)(g * factor), (int)(b * factor), newAlpha);
    }

    public Color lerp(Color other, float t) {
        return new Color(
                (int)(r + (other.r - r) * t),
                (int)(g + (other.g - g) * t),
                (int)(b + (other.b - b) * t),
                (int)(a + (other.a - a) * t)
        );
    }

    /**
     * Converts HSV to RGB color.
     * @param hue       0..360
     * @param saturation 0..1
     * @param value      0..1
     * @return new Color (alpha = 255)
     */
    public static Color fromHsv(float hue, float saturation, float value) {
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);
        return new Color(rgb);
    }

    @Override
    public String toString() {
        return String.format("Color(r=%d, g=%d, b=%d, a=%d)", r, g, b, a);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Color)) return false;
        Color other = (Color) obj;
        return r == other.r && g == other.g && b == other.b && a == other.a;
    }

    @Override
    public int hashCode() {
        return getARGB();
    }
}