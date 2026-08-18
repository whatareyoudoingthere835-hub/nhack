package dev.nhack.client.event.events;

import dev.nhack.client.event.CancellableEvent;

public final class KeyInputEvent extends CancellableEvent {
	private final int key;
	private final int scancode;
	private final int action;
	private final int modifiers;

	public KeyInputEvent(int key, int scancode, int action, int modifiers) {
		this.key = key;
		this.scancode = scancode;
		this.action = action;
		this.modifiers = modifiers;
	}

	public int key() {
		return key;
	}

	public int scancode() {
		return scancode;
	}

	public int action() {
		return action;
	}

	public int modifiers() {
		return modifiers;
	}
}
