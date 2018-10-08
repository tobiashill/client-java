package io.serialized.client.aggregates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class StateBuilder<T extends State> {

  private final Class<T> stateClass;
  private final Map<String, EventHandler<T, ?>> handlers;

  private StateBuilder(Class<T> stateClass, Map<String, EventHandler<T, ?>> handlers) {
    this.stateClass = stateClass;
    this.handlers = handlers;
  }

  public static <T extends State> StateBuilder<T> stateBuilder(Class<T> stateClass) {
    return new StateBuilder<>(stateClass, new HashMap<>());
  }

  public static <T extends State> StateBuilder<T> stateBuilder(Class<T> stateClass, Map<String, EventHandler<T, ?>> handlers) {
    return new StateBuilder<>(stateClass, handlers);
  }

  public <E> StateBuilder<T> withHandler(Class<E> eventClass, EventHandler<T, E> handler) {
    this.handlers.put(eventClass.getSimpleName(), handler);
    return this;
  }

  public T buildState(List<? extends Event> events) {
    try {
      AtomicReference<T> currentState = new AtomicReference<>(stateClass.newInstance());
      events.forEach(e -> {
            String simpleName = e.data().getClass().getSimpleName();
            EventHandler handler = handlers.get(simpleName);
        if (handler == null) {
          throw new IllegalStateException("No matching handler for event type: " + simpleName);
        }
            T handle = (T) handler.handle(currentState.get(), e);
            currentState.set(handle);
          }
      );
      return currentState.get();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to build State", e);
    }
  }
}
