package silversword.axiom.client.event.player;

import silversword.axiom.client.eventbus.ICancellable;
import net.minecraft.world.InteractionHand;

public class SwingEvent implements ICancellable {
    private final InteractionHand hand;
    private boolean cancelled;

    public SwingEvent(InteractionHand hand) {
        this.hand = hand;
    }

    public InteractionHand getHand() {
        return hand;
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