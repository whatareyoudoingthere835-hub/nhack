package dev.nhack.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ColorSetting extends Setting<Integer> {
	private static final int[] PALETTE = {
		0xFFFFFFFF, 0xFFFF9200, 0xFF30FF00, 0xFF00BBFF,
		0xFFFF0000, 0xFF7B00FF, 0xFFFF0062, 0xFFC81E3A,
		0xFF2FFF00, 0xFFB300F1
	};

	public ColorSetting(String name, String description, int defaultValue) {
		super(name, description, defaultValue);
	}

	public int argb() {
		return get();
	}

	public void cycle() {
		int current = get();
		int index = 0;
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == current) {
				index = i;
				break;
			}
		}
		set(PALETTE[(index + 1) % PALETTE.length]);
	}

	public String hex() {
		return String.format("#%06X", get() & 0xFFFFFF);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			set(element.getAsInt());
		}
	}
}
