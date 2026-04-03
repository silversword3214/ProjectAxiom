package silversword.axiom.client.event.player;

import silversword.axiom.client.eventbus.ICancellable;

public class PreMotionEvent implements ICancellable {
    private boolean cancelled;
    private float forward;
    private float strafe;

    public PreMotionEvent() {
        this.forward = 0;
        this.strafe = 0;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public float getForward() {
        return forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }
}