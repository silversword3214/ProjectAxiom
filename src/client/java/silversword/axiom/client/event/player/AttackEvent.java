package silversword.axiom.client.event.player;

import net.minecraft.world.entity.Entity;
import silversword.axiom.client.eventbus.ICancellable;

public class AttackEvent implements ICancellable {
    private final Entity target;
    private boolean cancelled;

    public AttackEvent(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
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