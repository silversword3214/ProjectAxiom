package silversword.axiom.client.utils.animation;

public class Animation {
    private float value;
    private float target;
    private float speed; // per second

    public Animation(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void update(float delta) {
        if (value < target) {
            value = Math.min(value + speed * delta, target);
        } else if (value > target) {
            value = Math.max(value - speed * delta, target);
        }
    }

    public float getValue() {
        return value;
    }

    public boolean isFinished() {
        return Math.abs(value - target) < 0.001f;
    }
}