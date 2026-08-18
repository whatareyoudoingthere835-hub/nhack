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
}
