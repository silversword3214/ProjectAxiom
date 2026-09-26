package silversword.axiom.client.rendersystem.axiomrenderer.api.event;

public abstract class RenderEvent {
    public final float tickDelta;

    public RenderEvent(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

