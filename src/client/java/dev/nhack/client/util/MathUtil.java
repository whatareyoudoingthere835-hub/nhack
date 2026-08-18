package dev.nhack.client.util;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtil {
	private MathUtil() {
	}

	public static float random(float min, float max) {
		if (max <= min) {
			return min;
		}
		return ThreadLocalRandom.current().nextFloat(min, max);
	}

	public static double random(double min, double max) {
		if (max <= min) {
			return min;
		}
		return ThreadLocalRandom.current().nextDouble(min, max);
	}
}
