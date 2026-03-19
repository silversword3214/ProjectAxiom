package silversword.axiom.client.event;

import net.minecraft.client.gui.Click;
import silversword.axiom.client.eventbus.ICancellable;

public class MouseClickEvent implements ICancellable {
    private static final MouseClickEvent INSTANCE = new MouseClickEvent();
    private boolean cancelled;

    public Click click;
    public KeyboardAction action;

    public static MouseClickEvent get(Click click, KeyboardAction action) {
        INSTANCE.cancelled = false;
        INSTANCE.click = click;
        INSTANCE.action = action;
        return INSTANCE;

    }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override
    public boolean isCancelled() { return cancelled; }
}