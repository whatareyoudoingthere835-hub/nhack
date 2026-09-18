package dev.nhack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NHack implements ModInitializer {
	public static final String MOD_ID = "nhack";
	public static final String NAME = "nhack";
	/** Версия из fabric.mod.json — не расходится с номером jar'а в логах и crash-репортах. */
	public static final String VERSION = resolveVersion();
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	@Override
	public void onInitialize() {
		LOGGER.info("{} {} common init", NAME, VERSION);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static String resolveVersion() {
		try {
			return FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
		} catch (Throwable throwable) {
			return "unknown";
		}
	}
}
