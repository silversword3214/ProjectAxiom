package silversword.axiom.client.eventbus;

public interface ICancellable {
    void setCancelled(boolean cancelled);
    boolean isCancelled();

    default void cancel() {
        setCancelled(true);
    }
}