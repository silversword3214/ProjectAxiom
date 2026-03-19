package silversword.axiom.client.eventbus.listeners;

public interface IListener {

    void call(Object event);


    Class<?> getTarget();


    int getPriority();

    @Deprecated
    boolean isStatic();
}
