package silversword.axiom.client.event.render;

public abstract class RenderEvent {
    public final float tickDelta;

    public RenderEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

