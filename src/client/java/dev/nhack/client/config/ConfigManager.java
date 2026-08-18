package dev.nhack.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nhack.NHack;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path configFile;

	private ConfigManager() {
	}

	public static void init() {
		Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(NHack.MOD_ID);
		try {
			Files.createDirectories(folder);
		} catch (IOException exception) {
			NHack.LOGGER.error("Could not create config directory", exception);
		}
		configFile = folder.resolve("client.json");
		load();
	}

	public static void load() {
		if (configFile == null || !Files.exists(configFile)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configFile)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null) {
				return;
			}

			if (root.has("prefix")) {
				CommandManager.setPrefix(root.get("prefix").getAsString());
			}

			JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			for (Module module : ModuleManager.getModules()) {
				if (!modules.has(module.getName())) {
					continue;
				}

				JsonObject data = modules.getAsJsonObject(module.getName());
				if (data.has("bind")) {
					module.setBind(data.get("bind").getAsInt());
				}
				if (data.has("settings")) {
					JsonObject settings = data.getAsJsonObject("settings");
					for (Setting<?> setting : module.getSettings()) {
						JsonElement element = settings.get(setting.getName());
						if (element != null) {
							setting.fromJson(element);
						}
					}
				}
				if (data.has("enabled")) {
					module.setEnabled(data.get("enabled").getAsBoolean(), false);
				}
			}
			NHack.LOGGER.info("Loaded config from {}", configFile);
		} catch (Exception exception) {
			NHack.LOGGER.error("Failed to load config", exception);
		}
	}

	public static void save() {
		if (configFile == null) {
			return;
		}

		JsonObject root = new JsonObject();
		root.addProperty("prefix", CommandManager.getPrefix());

		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.getModules()) {
			JsonObject data = new JsonObject();
			data.addProperty("enabled", module.isEnabled());
			data.addProperty("bind", module.getBind() == 0 ? GLFW.GLFW_KEY_UNKNOWN : module.getBind());

			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				settings.add(setting.getName(), setting.toJson());
			}
			data.add("settings", settings);
			modules.add(module.getName(), data);
		}
		root.add("modules", modules);

		try (Writer writer = Files.newBufferedWriter(configFile)) {
			GSON.toJson(root, writer);
		} catch (IOException exception) {
			NHack.LOGGER.error("Failed to save config", exception);
		}
	}
}
