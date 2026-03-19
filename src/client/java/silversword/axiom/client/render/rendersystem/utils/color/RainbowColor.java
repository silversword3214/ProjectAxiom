package silversword.axiom.client.render.rendersystem.utils.color;

public class RainbowColor extends Color {
    private double speed;
    private static final float[] hsb = new float[3];

    public RainbowColor() {
        super();
    }

    public double getSpeed() {
        return speed;
    }

    public RainbowColor setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public RainbowColor getNext() {
        return getNext(1);
    }

    public RainbowColor getNext(double delta) {
        if (speed > 0) {
            java.awt.Color.RGBtoHSB(r, g, b, hsb);
            int c = java.awt.Color.HSBtoRGB(hsb[0] + (float) (speed * delta), 1, 1);

            r = toRGBAR(c);
            g = toRGBAG(c);
            b = toRGBAB(c);
        }
        return this;
    }

    // Korjataan set-metodi palauttamaan RainbowColor
    @Override
    public RainbowColor set(Color value) {
        super.set(value);
        return this;
    }

    // Lisätään myös set-metodi joka ottaa RainbowColor-parametrin
    public RainbowColor set(RainbowColor color) {
        super.set(color);
        this.speed = color.speed;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        return Double.compare(((RainbowColor) o).speed, speed) == 0;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(speed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}