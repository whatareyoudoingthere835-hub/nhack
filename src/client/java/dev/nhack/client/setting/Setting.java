package dev.nhack.client.setting;

import com.google.gson.JsonElement;

import java.util.Objects;

public abstract class Setting<T> {
	private final String name;
	private final String description;
	private T value;
	private final T defaultValue;
	private Runnable onChanged;

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
		T next = sanitize(value);
		if (Objects.equals(this.value, next)) {
			return;
		}

		this.value = next;
		if (onChanged != null) {
			onChanged.run();
		}
	}

	/** Runs whenever {@link #set(Object)} actually changes the value (GUI, commands and config all go through it). */
	public Setting<T> onChanged(Runnable action) {
		this.onChanged = action;
		return this;
	}

	public T getDefault() {
		return defaultValue;
	}

	public void reset() {
		set(defaultValue);
	}

	protected T sanitize(T value) {
		return value;
	}

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
