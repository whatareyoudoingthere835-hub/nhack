package dev.nhack.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String description, String defaultValue, String... modes) {
		super(name, description, defaultValue);
		this.modes = List.copyOf(Arrays.asList(modes));
		if (!this.modes.contains(defaultValue)) {
			throw new IllegalArgumentException("Default mode must be one of: " + this.modes);
		}
	}

	public List<String> getModes() {
		return modes;
	}

	public void cycle(int direction) {
		if (modes.isEmpty()) {
			return;
		}
		int index = modes.indexOf(get());
		if (index < 0) {
			index = 0;
		}
		int next = Math.floorMod(index + direction, modes.size());
		set(modes.get(next));
	}

	public boolean is(String mode) {
		return get().equalsIgnoreCase(mode);
	}

	@Override
	protected String sanitize(String value) {
		if (value == null) {
			return getDefault();
		}
		for (String mode : modes) {
			if (mode.equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return getDefault();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			set(element.getAsString());
		}
	}
}
