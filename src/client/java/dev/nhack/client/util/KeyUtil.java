package dev.nhack.client.util;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class KeyUtil {
	private KeyUtil() {
	}

	public static String name(int key) {
		if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) {
			return "NONE";
		}
		String glfw = GLFW.glfwGetKeyName(key, 0);
		if (glfw != null && !glfw.isBlank()) {
			return glfw.toUpperCase(Locale.ROOT);
		}
		return switch (key) {
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
			case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
			case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_INSERT -> "INSERT";
			case GLFW.GLFW_KEY_DELETE -> "DELETE";
			default -> "KEY" + key;
		};
	}

	public static int fromName(String raw) {
		if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("unbind")) {
			return GLFW.GLFW_KEY_UNKNOWN;
		}

		String name = raw.trim().toUpperCase(Locale.ROOT);
		return switch (name) {
			case "RSHIFT", "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
			case "LSHIFT", "LEFT_SHIFT", "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
			case "RCTRL", "RIGHT_CTRL", "RCONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
			case "LCTRL", "LEFT_CTRL", "CTRL", "CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
			case "RALT", "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
			case "LALT", "LEFT_ALT", "ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
			case "TAB" -> GLFW.GLFW_KEY_TAB;
			case "SPACE" -> GLFW.GLFW_KEY_SPACE;
			case "INSERT" -> GLFW.GLFW_KEY_INSERT;
			case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
			default -> {
				if (name.length() == 1) {
					char character = name.charAt(0);
					if (character >= 'A' && character <= 'Z') {
						yield GLFW.GLFW_KEY_A + (character - 'A');
					}
					if (character >= '0' && character <= '9') {
						yield GLFW.GLFW_KEY_0 + (character - '0');
					}
				}
				yield GLFW.GLFW_KEY_UNKNOWN;
			}
		};
	}
}
