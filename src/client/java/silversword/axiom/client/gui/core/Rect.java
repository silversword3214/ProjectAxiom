package silversword.axiom.client.gui.core;

public final class Rect {
    public int x, y, w, h;

    public Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < (x + w) && py < (y + h);
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < (x + w) && py < (y + h);
    }

    public Rect inset(int px) {
        return new Rect(x + px, y + px, w - px * 2, h - px * 2);
    }

    public Rect copy() {
        return new Rect(x, y, w, h);
    }

    @Override
    public String toString() {
        return "Rect{x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "}";
    }
}
