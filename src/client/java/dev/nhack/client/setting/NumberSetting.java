package dev.nhack.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double increment;
	private final int decimals;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double increment) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.increment = increment;
		this.decimals = decimalsOf(increment);
		set(defaultValue);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getIncrement() {
		return increment;
	}

	public int getInt() {
		return get().intValue();
	}

	public float getFloat() {
		return get().floatValue();
	}

	public void increment(int steps) {
		set(get() + increment * steps);
	}

	public String formatted() {
		return String.format("%." + decimals + "f", get());
	}

	@Override
	protected Double sanitize(Double value) {
		if (value == null) {
			return getDefault();
		}
		double snapped = Math.round(value / increment) * increment;
		snapped = Math.max(min, Math.min(max, snapped));
		double scale = Math.pow(10, decimals);
		return Math.round(snapped * scale) / scale;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			set(element.getAsDouble());
		}
	}

	private static int decimalsOf(double increment) {
		String text = Double.toString(increment);
		int dot = text.indexOf('.');
		if (dot < 0) {
			return 0;
		}
		return Math.min(4, text.length() - dot - 1);
	}
}
