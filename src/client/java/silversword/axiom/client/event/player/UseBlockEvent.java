package silversword.axiom.client.event.player;

import net.minecraft.util.hit.BlockHitResult;
import silversword.axiom.client.eventbus.ICancellable;

/**
 * Laukaistaan kun pelaaja yrittää käyttää (klikata) lohkoa.
 * Peruuttamalla tapahtuma estetään alkuperäinen toiminta.
 */
public class UseBlockEvent implements ICancellable {
    public final BlockHitResult hitResult;
    private boolean cancelled;

    public UseBlockEvent(BlockHitResult hitResult) {
        this.hitResult = hitResult;
    }

    public BlockHitResult getHitResult() {
        return hitResult;
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