package dev.nhack.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nhack.NHack;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.config.ConfigManager;
import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.gui.menu.NHackMainMenuScreen;
import dev.nhack.client.module.modules.client.ClickGuiModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

public final class NHackClient implements ClientModInitializer {
	public static KeyMapping clickGuiKey;
	private static final ModuleManager MODULE_DISPATCH = new ModuleManager();

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		CommandManager.init();
		EventBus.register(MODULE_DISPATCH);
		ConfigManager.init();

		KeyMapping.Category category = KeyMapping.Category.register(NHack.id("client"));
		clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.nhack.clickgui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (clickGuiKey.consumeClick()) {
				ModuleManager.get(ClickGuiModule.class).ifPresent(ClickGuiModule::toggle);
			}
			if (client.screen instanceof TitleScreen) {
				if (!NHackMainMenuScreen.allowVanilla) {
					client.setScreen(NHackMainMenuScreen.getInstance());
				}
			} else if (!(client.screen instanceof NHackMainMenuScreen)) {
				NHackMainMenuScreen.allowVanilla = false;
			}
		});

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.CHAT,
			NHack.id("overlay"),
			(graphics, deltaTracker) -> EventBus.post(new HudRenderEvent(graphics, deltaTracker))
		);

		ClientSendMessageEvents.ALLOW_CHAT.register(message -> !CommandManager.handle(message));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

		NHack.LOGGER.info("{} client ready — Right Shift opens ClickGUI, .help for commands", NHack.NAME);
	}
}
