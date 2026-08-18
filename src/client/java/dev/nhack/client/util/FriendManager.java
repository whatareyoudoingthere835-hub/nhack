package dev.nhack.client.util;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FriendManager {
	private static final Set<String> FRIENDS = ConcurrentHashMap.newKeySet();

	private FriendManager() {
	}

	public static boolean add(String name) {
		return FRIENDS.add(normalize(name));
	}

	public static boolean remove(String name) {
		return FRIENDS.remove(normalize(name));
	}

	public static boolean isFriend(String name) {
		return FRIENDS.contains(normalize(name));
	}

	public static Set<String> all() {
		return Collections.unmodifiableSet(FRIENDS);
	}

	private static String normalize(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}
}
