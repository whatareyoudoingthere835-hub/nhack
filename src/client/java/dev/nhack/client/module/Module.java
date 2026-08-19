package dev.nhack.client.module;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.setting.Setting;
import dev.nhack.client.util.ChatUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private int bind = GLFW.GLFW_KEY_UNKNOWN;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getBind() {
		return bind;
	}

	public void setBind(int bind) {
		this.bind = bind;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	protected <S extends Setting<?>> S addSetting(S setting) {
		settings.add(setting);
		return setting;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		setEnabled(enabled, true);
	}

	public void setEnabled(boolean enabled, boolean notify) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;
		if (enabled) {
			EventBus.register(this);
			onEnable();
			if (notify) {
				ChatUtil.info(name + " enabled");
			}
		} else {
			onDisable();
			EventBus.unregister(this);
			if (notify) {
				ChatUtil.info(name + " disabled");
			}
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	public void onTick(TickEvent.Post event) {
	}

	public void onRenderHud(HudRenderEvent event) {
	}
}
