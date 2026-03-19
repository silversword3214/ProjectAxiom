package silversword.axiom.client.utils.animation;

public class Ease {
    public static float easeOutQuad(float t) {
        return t * (2 - t);
    }
}