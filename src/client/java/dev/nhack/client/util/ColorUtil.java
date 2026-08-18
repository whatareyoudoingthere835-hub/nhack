package dev.nhack.client.util;

public final class ColorUtil {
	public static final int ACCENT = 0xFFC81E3A;
	public static final int ACCENT_HOVER = 0xFFE03A54;
	public static final int PANEL = 0xE0141418;
	public static final int PANEL_HEADER = 0xF00C0C10;
	public static final int MODULE = 0xD018181C;
	public static final int MODULE_HOVER = 0xD024242A;
	public static final int TEXT = 0xFFEDEDED;
	public static final int TEXT_DIM = 0xFF9A9AA3;
	public static final int ENABLED = 0xFFF2F2F2;
	public static final int OUTLINE = 0xFF2A2A32;

	private ColorUtil() {
	}

	public static int rgba(int red, int green, int blue, int alpha) {
		return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
	}

	public static int withAlpha(int color, int alpha) {
		return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
	}
}
