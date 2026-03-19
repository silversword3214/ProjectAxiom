package silversword.axiom.client.eventbus;

import silversword.axiom.client.eventbus.listeners.IListener;
import silversword.axiom.client.eventbus.listeners.LambdaListener;

public interface IEventBus {

    void registerLambdaFactory(String packagePrefix, LambdaListener.Factory factory);

    boolean isListening(Class<?> eventClass);
    <T> T post(T event);
    <T extends ICancellable> T post(T event);
    void subscribe(Object object);
    void subscribe(Class<?> axiomClass);
    void subscribe(IListener listener);
    void unsubscribe(Object object);
    void unsubscribe(Class<?> axiomClass);
    void unsubscribe(IListener listener);
}
