package silversword.axiom.client.event;

import silversword.axiom.client.eventbus.ICancellable;

public class KeyboardEvent implements ICancellable {
    private static final KeyboardEvent INSTANCE = new KeyboardEvent();
    private boolean cancelled;

    public int key;
    public KeyboardAction action;
    public int modifiers;

    public static KeyboardEvent get(int key, KeyboardAction action, int modifiers) {
        INSTANCE.cancelled = false;
        INSTANCE.key = key;
        INSTANCE.action = action;
        INSTANCE.modifiers = modifiers;
        return INSTANCE;
    }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override
    public boolean isCancelled() { return cancelled; }
}