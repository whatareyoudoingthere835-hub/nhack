package dev.nhack.client.util;

public final class TickManager {
	private static float speed = 1.0F;
	private static int shiftIterations;
	private static boolean shifting;

	private TickManager() {
	}

	public static float speed() {
		return speed <= 0.0F ? 1.0F : speed;
	}

	public static void setSpeed(float value) {
		speed = value <= 0.0F ? 1.0F : value;
	}

	public static void reset() {
		speed = 1.0F;
		shiftIterations = 0;
	}

	public static void requestShift(int iterations) {
		shiftIterations = Math.max(0, iterations);
	}

	public static int consumeShift() {
		if (shifting) {
			return 0;
		}
		int value = shiftIterations;
		shiftIterations = 0;
		return value;
	}

	public static void beginShift() {
		shifting = true;
	}

	public static void endShift() {
		shifting = false;
	}

	public static boolean shifting() {
		return shifting;
	}
}
