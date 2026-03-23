package silversword.axiom.client.render.rendersystem.utils.render;

public class DirectionUtil {
    public static final int WEST  = 1 << 0;
    public static final int EAST  = 1 << 1;
    public static final int NORTH = 1 << 2;
    public static final int SOUTH = 1 << 3;
    public static final int DOWN  = 1 << 4;
    public static final int UP    = 1 << 5;

    public static boolean isNot(int exclude, int dir) {
        return (exclude & dir) == 0;
    }
}