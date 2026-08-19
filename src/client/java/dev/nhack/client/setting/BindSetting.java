package dev.nhack.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.lwjgl.glfw.GLFW;

public final class BindSetting extends Setting<Integer> {
	public BindSetting(String name, String description, int defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean isDown(long window) {
		int key = get();
		return key > GLFW.GLFW_KEY_UNKNOWN && key <= GLFW.GLFW_KEY_LAST
			&& GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
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
