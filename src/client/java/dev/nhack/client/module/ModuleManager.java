package dev.nhack.client.module;

import dev.nhack.NHack;
import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.modules.client.ClickGuiModule;
import dev.nhack.client.module.modules.client.HudModule;
import dev.nhack.client.module.modules.combat.AuraModule;
import dev.nhack.client.module.modules.misc.ExampleModule;
import dev.nhack.client.module.modules.movement.SprintModule;
import dev.nhack.client.module.modules.render.FullbrightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static final boolean[] LAST_KEYS = new boolean[GLFW.GLFW_KEY_LAST + 1];

	public ModuleManager() {
	}

	public static void init() {
		register(new ClickGuiModule());
		register(new HudModule());
		register(new AuraModule());
		register(new SprintModule());
		register(new FullbrightModule());
		register(new ExampleModule());

		MODULES.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
		NHack.LOGGER.info("Registered {} modules", MODULES.size());
	}

	private static void register(Module module) {
		MODULES.add(module);
	}

	public static List<Module> getModules() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> getByCategory(Category category) {
		return MODULES.stream().filter(module -> module.getCategory() == category).toList();
	}

	public static List<Module> getEnabled() {
		return MODULES.stream().filter(Module::isEnabled).toList();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> Optional<T> get(Class<T> type) {
		for (Module module : MODULES) {
			if (type.isInstance(module)) {
				return Optional.of((T) module);
			}
		}
		return Optional.empty();
	}

	public static Optional<Module> get(String name) {
		for (Module module : MODULES) {
			if (module.getName().equalsIgnoreCase(name)) {
				return Optional.of(module);
			}
		}
		return Optional.empty();
	}

	@Subscribe
	public void onTick(TickEvent.Post event) {
		pollBinds();
		for (Module module : MODULES) {
			if (module.isEnabled()) {
				module.onTick(event);
			}
		}
	}

	@Subscribe
	public void onRenderHud(HudRenderEvent event) {
		for (Module module : MODULES) {
			if (module.isEnabled()) {
				module.onRenderHud(event);
			}
		}
	}

	private static void pollBinds() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() == null) {
			return;
		}

		long handle = mc.getWindow().handle();
		boolean skipToggle = mc.screen instanceof ChatScreen;

		for (Module module : MODULES) {
			int key = module.getBind();
			if (key <= GLFW.GLFW_KEY_UNKNOWN || key > GLFW.GLFW_KEY_LAST) {
				continue;
			}
			if (module instanceof ClickGuiModule) {
				continue;
			}

			boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
			if (!skipToggle && down && !LAST_KEYS[key]) {
				module.toggle();
			}
		}

		for (int key = 0; key <= GLFW.GLFW_KEY_LAST; key++) {
			LAST_KEYS[key] = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
		}
	}

	public static String suggest(String prefix) {
		String needle = prefix.toLowerCase(Locale.ROOT);
		return MODULES.stream()
			.map(Module::getName)
			.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(needle))
			.findFirst()
			.orElse(prefix);
	}
}
