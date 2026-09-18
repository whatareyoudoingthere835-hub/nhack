package dev.nhack.client.util;

import dev.nhack.NHack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

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

		MutableComponent prefix = name()
			.append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
		mc.player.displayClientMessage(prefix.append(Component.literal(message).withStyle(color)), false);
	}

	/**
	 * Имя клиента в чате. С включённым RGB и настройкой {@code Chat} каждая буква своего оттенка —
	 * spectrum идёт слева направо, как и во всех окнах. Цвет запекается в момент отправки,
	 * поэтому в истории сообщение остаётся таким, каким ушло.
	 */
	private static MutableComponent name() {
		if (!ColorUtil.rgbEnabled() || !ColorUtil.rgbChat()) {
			return Component.literal(NHack.NAME).withStyle(ChatFormatting.RED);
		}

		MutableComponent name = Component.empty();
		for (int i = 0; i < NHack.NAME.length(); i++) {
			int rgb = ColorUtil.rgb(i * 7.0F) & 0x00FFFFFF;
			name.append(Component.literal(String.valueOf(NHack.NAME.charAt(i)))
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
		}
		return name;
	}
}
