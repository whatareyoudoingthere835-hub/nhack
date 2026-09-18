package dev.nhack.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Рисование интерфейса с учётом RGB-переливания (модуль «1.12.2?»).
 *
 * <p>Когда модуль выключен, оба метода рисуют ровно то же и тем же цветом, что и обычный
 * {@code graphics.fill} / {@code graphics.drawString} — то есть подмена ничем не отличается.
 * Когда включён, цвет берётся из {@link ColorUtil#rgb(float, float)} по экранной координате X,
 * поэтому спектр едет слева направо по всем окнам сразу.
 */
public final class RgbUtil {
	private RgbUtil() {
	}

	/**
	 * Полоса, рамка или подложка. Широкие области красятся вертикальными полосками по пикселю —
	 * иначе на линии в пол-окна был бы один сплошной цвет вместо градиента. Альфа берётся из
	 * исходного цвета, так что полупрозрачные рамки остаются полупрозрачными.
	 */
	public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int fallback) {
		if (!ColorUtil.rgbEnabled()) {
			graphics.fill(x1, y1, x2, y2, fallback);
			return;
		}

		int alpha = fallback >>> 24;
		if (alpha == 0) {
			alpha = 0xFF;
		}

		int from = Math.min(x1, x2);
		int to = Math.max(x1, x2);
		int top = Math.min(y1, y2);
		int bottom = Math.max(y1, y2);
		if (to - from <= 2) {
			// узкую полоску дешевле покрасить одним оттенком
			graphics.fill(from, top, to, bottom, ColorUtil.withAlpha(ColorUtil.rgb(from, top), alpha));
			return;
		}

		for (int x = from; x < to; x++) {
			graphics.fill(x, top, x + 1, bottom, ColorUtil.withAlpha(ColorUtil.rgb(x, top), alpha));
		}
	}

	public static void text(GuiGraphics graphics, Font font, String text, int x, int y, int fallback) {
		text(graphics, font, text, x, y, fallback, true);
	}

	/**
	 * Текст: с включённым {@code PerCharText} каждая буква своего оттенка (так это выглядело
	 * в клиентах 1.12.2), иначе вся строка красится оттенком своей левой границы.
	 */
	public static void text(GuiGraphics graphics, Font font, String text, int x, int y, int fallback, boolean shadow) {
		if (text == null || text.isEmpty() || !ColorUtil.rgbEnabled() || !ColorUtil.rgbPerCharText()) {
			int color = ColorUtil.rgbEnabled() && !ColorUtil.rgbPerCharText()
				? ColorUtil.withAlpha(ColorUtil.rgb(x, y), alphaOf(fallback))
				: fallback;
			graphics.drawString(font, text, x, y, color, shadow);
			return;
		}

		int alpha = alphaOf(fallback);
		int cursor = x;
		for (int i = 0; i < text.length(); i++) {
			String character = String.valueOf(text.charAt(i));
			graphics.drawString(font, character, cursor, y, ColorUtil.withAlpha(ColorUtil.rgb(cursor, y), alpha), shadow);
			cursor += font.width(character);
		}
	}

	private static int alphaOf(int color) {
		int alpha = color >>> 24;
		return alpha == 0 ? 0xFF : alpha;
	}
}
