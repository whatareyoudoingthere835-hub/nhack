package dev.nhack.client.event;

import dev.nhack.NHack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
	private static final Map<Class<?>, List<Listener>> LISTENERS = new ConcurrentHashMap<>();
	private static final Map<Object, List<Listener>> SUBSCRIBERS = new ConcurrentHashMap<>();

	private EventBus() {
	}

	public static void register(Object subscriber) {
		if (SUBSCRIBERS.containsKey(subscriber)) {
			return;
		}

		List<Listener> owned = new ArrayList<>();
		Class<?> type = subscriber.getClass();
		while (type != null && type != Object.class) {
			for (Method method : type.getDeclaredMethods()) {
				if (!method.isAnnotationPresent(Subscribe.class)) {
					continue;
				}
				if (method.getParameterCount() != 1) {
					NHack.LOGGER.warn("Skipping @Subscribe {}.{} — expected 1 parameter", type.getSimpleName(), method.getName());
					continue;
				}

				Class<?> eventType = method.getParameterTypes()[0];
				if (!Event.class.isAssignableFrom(eventType)) {
					NHack.LOGGER.warn("Skipping @Subscribe {}.{} — parameter must extend Event", type.getSimpleName(), method.getName());
					continue;
				}

				method.setAccessible(true);
				Listener listener = new Listener(subscriber, method, eventType);
				owned.add(listener);
				LISTENERS.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(listener);
			}
			type = type.getSuperclass();
		}

		SUBSCRIBERS.put(subscriber, owned);
	}

	public static void unregister(Object subscriber) {
		List<Listener> owned = SUBSCRIBERS.remove(subscriber);
		if (owned == null) {
			return;
		}

		for (Listener listener : owned) {
			List<Listener> bucket = LISTENERS.get(listener.eventType);
			if (bucket != null) {
				bucket.remove(listener);
			}
		}
	}

	public static <T extends Event> T post(T event) {
		List<Listener> bucket = LISTENERS.get(event.getClass());
		if (bucket == null || bucket.isEmpty()) {
			return event;
		}

		for (Listener listener : bucket) {
			if (event instanceof CancellableEvent cancellable && cancellable.isCancelled()) {
				break;
			}
			listener.invoke(event);
		}
		return event;
	}

	private record Listener(Object owner, Method method, Class<?> eventType) {
		private void invoke(Event event) {
			try {
				method.invoke(owner, event);
			} catch (ReflectiveOperationException exception) {
				NHack.LOGGER.error("Failed to dispatch {} to {}.{}",
					event.getClass().getSimpleName(),
					owner.getClass().getSimpleName(),
					method.getName(),
					exception);
			}
		}
	}
}
