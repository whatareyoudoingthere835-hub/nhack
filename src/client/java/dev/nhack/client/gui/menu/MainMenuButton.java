package dev.nhack.client.gui.menu;

import dev.nhack.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class MainMenuButton {
	private final float posX;
	private final float posY;
	private final float width;
	private final float height;
	private final String name;
	private final Runnable action;

	public MainMenuButton(float posX, float posY, String name, Runnable action, boolean exit) {
		this.posX = posX;
		this.posY = posY;
		this.name = name;
		this.action = action;
		this.width = exit ? 222.0F : 107.0F;
		this.height = 38.0F;
	}

	public MainMenuButton(float posX, float posY, String name, Runnable action) {
		this(posX, posY, name, action, false);
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getInstance();
		float cx = mc.getWindow().getGuiScaledWidth() / 2.0F;
		float cy = mc.getWindow().getGuiScaledHeight() / 2.0F;
		int x = Math.round(cx + posX);
		int y = Math.round(cy + posY);
		int x2 = Math.round(x + width);
		int y2 = Math.round(y + height);
		boolean hovered = hovered(mouseX, mouseY, x, y, x2, y2);

		graphics.fill(x, y, x2, y2, hovered ? ColorUtil.withAlpha(ColorUtil.ACCENT, 0x55) : ColorUtil.GLASS);
		graphics.fill(x, y, x2, y + 1, hovered ? ColorUtil.ACCENT_HOVER : ColorUtil.ACCENT);
		graphics.fill(x, y, x + 1, y2, ColorUtil.withAlpha(ColorUtil.ACCENT, 0x66));
		graphics.fill(x2 - 1, y, x2, y2, ColorUtil.withAlpha(ColorUtil.ACCENT, 0x66));

		Font font = mc.font;
		int textX = x + (Math.round(width) - font.width(name)) / 2;
		int textY = y + Math.round(height) / 2 - 4;
		graphics.drawString(font, name, textX, textY, hovered ? ColorUtil.TEXT : ColorUtil.TEXT_DIM);
	}

	public void click(int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getInstance();
		float cx = mc.getWindow().getGuiScaledWidth() / 2.0F;
		float cy = mc.getWindow().getGuiScaledHeight() / 2.0F;
		int x = Math.round(cx + posX);
		int y = Math.round(cy + posY);
		if (hovered(mouseX, mouseY, x, y, Math.round(x + width), Math.round(y + height))) {
			action.run();
		}
	}

	private static boolean hovered(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
		return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
	}
}
