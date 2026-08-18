package dev.nhack.client.event;

public abstract class CancellableEvent extends Event {
	private boolean cancelled;

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		this.cancelled = true;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
