package rtx.nv.api.events;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import rtx.nv.NV;
import rtx.nv.api.events.CancellableEvent;
import rtx.nv.api.events.Event;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.Priority;

public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, CopyOnWriteArrayList<EventBus.Binding>> bindings = new ConcurrentHashMap();
    private final AtomicInteger insertionCounter = new AtomicInteger();
    private final Set<Object> subscribers = Collections.newSetFromMap(new IdentityHashMap<>());

    private EventBus() {
    }

    public static EventBus get() {
        return INSTANCE;
    }

    public <E extends Event> E post(E e) {
        List<EventBus.Binding> list = this.bindings.get(e.getClass());
        if (list == null || list.isEmpty()) {
            return e;
        }
        boolean bl = e instanceof CancellableEvent;
        for (EventBus.Binding binding : list) {
            CancellableEvent cancellableEvent;
            if (bl && (!(cancellableEvent = (CancellableEvent)e).isCancelled() || binding.priority() != Priority.MONITOR) && cancellableEvent.isCancelled()) continue;
            binding.invoke(e);
        }
        return e;
    }

    private void resort(Class<?> clazz) {
        CopyOnWriteArrayList<EventBus.Binding> copyOnWriteArrayList = this.bindings.get(clazz);
        if (copyOnWriteArrayList == null) {
            return;
        }
        copyOnWriteArrayList.sort(Comparator.comparingInt((EventBus.Binding binding) -> binding.priority().ordinal()).thenComparingInt(EventBus.Binding::order));
    }

    private MethodHandle createHandle(Object object, Method method) throws Exception {
        method.setAccessible(true);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        return lookup.unreflect(method).bindTo(object);
    }

    public synchronized void subscribe(Object object) {
        if (object == null || !this.subscribers.add(object)) {
            return;
        }
        for (Class<?> clazz2 = object.getClass(); clazz2 != null && clazz2 != Object.class; clazz2 = clazz2.getSuperclass()) {
            for (Method method : clazz2.getDeclaredMethods()) {
                Class<?> clazz3;
                EventHandler eventHandler = method.getAnnotation(EventHandler.class);
                if (eventHandler == null || method.getParameterCount() != 1 || !Event.class.isAssignableFrom(clazz3 = method.getParameterTypes()[0])) continue;
                try {
                    MethodHandle methodHandle = this.createHandle(object, method);
                    EventBus.Binding binding = new EventBus.Binding(object, methodHandle, eventHandler.value(), this.insertionCounter.getAndIncrement());
                    this.bindings.computeIfAbsent(clazz3, clazz -> new CopyOnWriteArrayList()).add(binding);
                    this.resort(clazz3);
                }
                catch (Exception exception) {
                    NV.LOGGER.error("[EventBus] Failed to register handler {}.{}", new Object[]{clazz2.getSimpleName(), method.getName(), exception});
                }
            }
        }
    }

    public synchronized void unsubscribe(Object object) {
        this.subscribers.remove(object);
        for (CopyOnWriteArrayList<EventBus.Binding> copyOnWriteArrayList : this.bindings.values()) {
            copyOnWriteArrayList.removeIf(binding -> binding.owner() == object);
        }
    }

    public boolean hasListeners(Class<? extends Event> clazz) {
        List list = this.bindings.get(clazz);
        return list != null && !list.isEmpty();
    }


    public record Binding(Object owner, MethodHandle handle, Priority priority, int order) {
        public void invoke(Event event) {
            try {
                this.handle.invoke(event);
            } catch (Throwable throwable) {
                String string = this.owner.getClass().getName() + "|" + event.getClass().getName() + "|" + throwable.getClass().getName();
                if (EventBus.REPORTED_FAILURES.add(string)) {
                    NV.LOGGER.error("[EventBus] Exception in event handler {} for {} (further identical errors suppressed)", this.owner.getClass().getSimpleName(), event.getClass().getSimpleName(), throwable);
                }
            }
        }
    }
}
