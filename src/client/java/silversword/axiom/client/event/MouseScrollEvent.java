package silversword.axiom.client.event;

import silversword.axiom.client.eventbus.ICancellable;

public class MouseScrollEvent implements ICancellable {
    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();
    private boolean cancelled;

    public double value; // scroll amount (positive = up, negative = down)

    public static MouseScrollEvent get(double value) {
        INSTANCE.cancelled = false;
        INSTANCE.value = value;
        return INSTANCE;
    }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override
    public boolean isCancelled() { return cancelled; }
}