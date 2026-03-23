package silversword.axiom.client.render.rendersystem.utils.color;

public class RainbowColor extends Color {
    private float speed; // cycles per second (sama kuin SettingColor.speed)

    public RainbowColor() {
        super(0, 0, 0, 255);
        this.speed = 1.0f;
    }

    public RainbowColor set(SettingColor setting) {
        this.speed = setting.speed;
        return this;
    }

    public RainbowColor setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    @Override
    public int getARGB() {
        // Käytetään samaa kaavaa kuin SettingColor.getCurrentColor()
        // Hue vaihtelee jaksossa 5000 ms / speed
        long now = System.currentTimeMillis();
        float hue = (now % (long)(5000 / speed)) / (5000f / speed);
        int rgb = java.awt.Color.HSBtoRGB(hue, 1f, 1f);
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
        this.a = 255;
        return super.getARGB();
    }
}