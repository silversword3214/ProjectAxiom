package silversword.axiom.client.utils.misc;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

public class Pool<T> {
    private final Queue<T> items = new ArrayDeque<>();
    private final Supplier<T> factory;

    public Pool(Supplier<T> factory) {
        this.factory = factory;
    }

    public T get() {
        T item = items.poll();
        return item != null ? item : factory.get();
    }

    public void free(T item) {
        items.offer(item);
    }
}