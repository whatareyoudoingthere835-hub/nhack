package dev.nhack.client.util;

public final class Timer {
	private long last = System.currentTimeMillis();

	public void reset() {
		last = System.currentTimeMillis();
	}

	public boolean passedMs(long ms) {
		return System.currentTimeMillis() - last >= ms;
	}
}
