package dev.nhack.client.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {
	private final String name;
	private final String description;
	private T value;
	private final T defaultValue;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = sanitize(value);
	}

	public T getDefault() {
		return defaultValue;
	}

	public void reset() {
		this.value = defaultValue;
	}

	protected T sanitize(T value) {
		return value;
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
