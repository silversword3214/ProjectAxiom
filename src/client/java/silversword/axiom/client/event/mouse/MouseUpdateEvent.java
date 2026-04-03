package silversword.axiom.client.event.mouse;

import silversword.axiom.client.eventbus.ICancellable;

public class MouseUpdateEvent implements ICancellable {
    private double deltaX;
    private double deltaY;
    private final double defaultDeltaX;
    private final double defaultDeltaY;
    private boolean cancelled;

    public MouseUpdateEvent(double deltaX, double deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.defaultDeltaX = deltaX;
        this.defaultDeltaY = deltaY;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }

    public double getDefaultDeltaX() {
        return defaultDeltaX;
    }

    public double getDefaultDeltaY() {
        return defaultDeltaY;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}