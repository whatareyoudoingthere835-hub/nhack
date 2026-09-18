package dev.nhack.client.util;

public final class ColorUtil {
	public static final int ACCENT = 0xFFC81E3A;
	public static final int ACCENT_HOVER = 0xFFE03A54;
	public static final int ACCENT_SOFT = 0x55C81E3A;
	public static final int GLASS = 0x99101014;
	public static final int GLASS_DARK = 0xBB0B0B10;
	public static final int GLASS_HEADER = 0xCC0A0A0E;
	public static final int ROW = 0x33000000;
	public static final int ROW_HOVER = 0x44FFFFFF;
	public static final int ROW_ACTIVE = 0x33C81E3A;
	public static final int STROKE = 0x55FFFFFF;
	public static final int TEXT = 0xFFF2F2F4;
	public static final int TEXT_DIM = 0xB3A8A8B3;
	public static final int ENABLED = 0xFFFFFFFF;
	public static final int PANEL = 0x99101014;
	public static final int PANEL_HEADER = 0xCC0A0A0E;
	public static final int MODULE = 0x33000000;
	public static final int MODULE_HOVER = 0x44FFFFFF;
	public static final int OUTLINE = 0x55FFFFFF;

	private ColorUtil() {
	}

	public static int rgba(int red, int green, int blue, int alpha) {
		return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
	}

	public static int withAlpha(int color, int alpha) {
		return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
	}

	// ---------------------------------------------------------------------
	// RGB-переливание (модуль «1.12.2?»). Состояние пишет модуль, читают все
	// интерфейсы: оттенок зависит от X на экране, поэтому спектр едет слева
	// направо и постоянно сдвигается со временем.
	// ---------------------------------------------------------------------

	private static boolean rgb;
	private static boolean rgbPerCharText = true;
	private static boolean rgbChat = true;
	private static float rgbSpeed = 0.8F;
	private static float rgbSpread = 200.0F;
	private static float rgbSaturation = 0.9F;
	private static float rgbBrightness = 1.0F;
	private static float rgbTilt;

	/** Пишется из модуля при включении/выключении и при изменении любой его настройки. */
	public static void configureRgb(boolean enabled, float speed, float spread, float saturation, float brightness, float tilt) {
		rgb = enabled;
		rgbSpeed = speed;
		rgbSpread = spread;
		rgbSaturation = saturation;
		rgbBrightness = brightness;
		rgbTilt = tilt;
	}

	public static void configureRgbExtras(boolean perCharText, boolean chat) {
		rgbPerCharText = perCharText;
		rgbChat = chat;
	}

	public static boolean rgbEnabled() {
		return rgb;
	}

	/** Красить ли текст побуквенно (иначе одна строка — один оттенок). */
	public static boolean rgbPerCharText() {
		return rgbPerCharText;
	}

	public static boolean rgbChat() {
		return rgbChat;
	}

	/** Оттенок в точке экрана, {@code 0xFFrrggbb}. */
	public static int rgb(float x, float y) {
		float cycles = (x + y * rgbTilt) / Math.max(8.0F, rgbSpread) + seconds() * rgbSpeed;
		float hue = cycles - (float) Math.floor(cycles);
		return hsv(hue, clamp01(rgbSaturation), clamp01(rgbBrightness)) | 0xFF000000;
	}

	public static int rgb(float x) {
		return rgb(x, 0.0F);
	}

	/** {@link #ACCENT}, но переливается, если модуль включён. */
	public static int accent(float x, float y) {
		return rgb ? rgb(x, y) : ACCENT;
	}

	/** {@link #ACCENT_HOVER} — тот же оттенок, но осветлённый, как hover у статики. */
	public static int accentHover(float x, float y) {
		return rgb ? lighten(rgb(x, y), 0.25F) : ACCENT_HOVER;
	}

	/** Акцент с нужной альфой (замена {@code withAlpha(ACCENT, ...)} и {@link #ACCENT_SOFT}). */
	public static int accent(float x, float y, int alpha) {
		return withAlpha(accent(x, y), alpha);
	}

	/** Подсветка активной строки. */
	public static int rowActive(float x, float y) {
		return rgb ? accent(x, y, 0x33) : ROW_ACTIVE;
	}

	/** Цвет включённого модуля. */
	public static int enabled(float x, float y) {
		return rgb ? rgb(x, y) : ENABLED;
	}

	/** Тонкая обводка/рамка. */
	public static int outline(float x, float y) {
		return rgb ? accent(x, y, 0x55) : OUTLINE;
	}

	public static int stroke(float x, float y) {
		return rgb ? accent(x, y, 0x55) : STROKE;
	}

	private static float seconds() {
		// Часовой остаток, чтобы float не терял точность на длинной сессии.
		return (System.currentTimeMillis() % 3_600_000L) / 1000.0F;
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
	}

	private static int lighten(int color, float amount) {
		int red = color >> 16 & 0xFF;
		int green = color >> 8 & 0xFF;
		int blue = color & 0xFF;
		red = (int) (red + (255 - red) * amount);
		green = (int) (green + (255 - green) * amount);
		blue = (int) (blue + (255 - blue) * amount);
		return color & 0xFF000000 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
	}

	/** HSV в ARGB без java.awt: альфа выставляется снаружи. */
	private static int hsv(float hue, float saturation, float value) {
		float sector = hue * 6.0F;
		int index = (int) sector % 6;
		float fraction = sector - (float) Math.floor(sector);
		float p = value * (1.0F - saturation);
		float q = value * (1.0F - fraction * saturation);
		float t = value * (1.0F - (1.0F - fraction) * saturation);

		float red;
		float green;
		float blue;
		switch (index) {
			case 0 -> {
				red = value;
				green = t;
				blue = p;
			}
			case 1 -> {
				red = q;
				green = value;
				blue = p;
			}
			case 2 -> {
				red = p;
				green = value;
				blue = t;
			}
			case 3 -> {
				red = p;
				green = q;
				blue = value;
			}
			case 4 -> {
				red = t;
				green = p;
				blue = value;
			}
			default -> {
				red = value;
				green = p;
				blue = q;
			}
		}

		return ((int) (red * 255.0F) & 0xFF) << 16
			| ((int) (green * 255.0F) & 0xFF) << 8
			| (int) (blue * 255.0F) & 0xFF;
	}
}
