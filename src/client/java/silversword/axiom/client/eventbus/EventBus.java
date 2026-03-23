package silversword.axiom.client.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);
    private final Map<Class<?>, List<HandlerEntry>> handlers = new ConcurrentHashMap<>();

    public void register(Object listener) {
        Class<?> clazz = listener.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                if (method.getParameterCount() != 1) {
                    LOGGER.warn("Event handler {} in {} must have exactly one parameter", method.getName(), clazz.getName());
                    continue;
                }
                Class<?> eventType = method.getParameterTypes()[0];
                EventPriority priority = method.getAnnotation(Subscribe.class).priority();
                method.setAccessible(true);

                handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                        .add(new HandlerEntry(listener, method, priority));
                LOGGER.debug("Registered handler for {} in {}", eventType.getSimpleName(), clazz.getSimpleName());
            }
        }
    }

    public void unregister(Object listener) {
        for (List<HandlerEntry> list : handlers.values()) {
            list.removeIf(entry -> entry.listener == listener);
        }
    }

    public void post(Object event) {
        List<HandlerEntry> entries = handlers.get(event.getClass());
        if (entries == null || entries.isEmpty()) return;

        // Lajitellaan prioriteetin mukaan (korkein ensin)
        List<HandlerEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(e -> -e.priority.ordinal()));

        for (HandlerEntry entry : sorted) {
            try {
                entry.method.invoke(entry.listener, event);
            } catch (Exception e) {
                LOGGER.error("Error invoking event handler {} in {}", entry.method.getName(), entry.listener.getClass().getName(), e);
            }
        }
    }

    private static class HandlerEntry {
        final Object listener;
        final Method method;
        final EventPriority priority;

        HandlerEntry(Object listener, Method method, EventPriority priority) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
        }
    }
}