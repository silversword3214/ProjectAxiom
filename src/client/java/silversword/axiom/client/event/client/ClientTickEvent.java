package silversword.axiom.client.event.client;

import silversword.axiom.client.eventbus.ICancellable;

public class ClientTickEvent implements ICancellable {
    private boolean cancelled;

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}