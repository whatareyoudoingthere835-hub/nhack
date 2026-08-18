package dev.nhack.client.util;

import dev.nhack.NHack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatUtil {
	private ChatUtil() {
	}

	public static void info(String message) {
		send(ChatFormatting.GRAY, message);
	}

	public static void error(String message) {
		send(ChatFormatting.RED, message);
	}

	public static void send(ChatFormatting color, String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			NHack.LOGGER.info("[chat] {}", message);
			return;
		}

		MutableComponent prefix = Component.literal(NHack.NAME)
			.withStyle(ChatFormatting.RED)
			.append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
		mc.player.displayClientMessage(prefix.append(Component.literal(message).withStyle(color)), false);
	}
}
