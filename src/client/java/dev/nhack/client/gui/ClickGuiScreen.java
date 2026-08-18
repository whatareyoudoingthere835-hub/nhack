package dev.nhack.client.gui;

import dev.nhack.NHack;
import dev.nhack.client.config.ConfigManager;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.client.ClickGuiModule;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.setting.Setting;
import dev.nhack.client.util.ColorUtil;
import dev.nhack.client.util.KeyUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ClickGuiScreen extends Screen {
	private static final int WINDOW_W = 640;
	private static final int WINDOW_H = 392;
	private static final int HEADER = 30;
	private static final int SIDEBAR = 118;
	private static final int SETTINGS = 214;
	private static final int ROW = 22;
	private static final int PAD = 10;

	private int winX;
	private int winY;
	private boolean placed;
	private boolean dragging;
	private int dragOffX;
	private int dragOffY;

	private Category selected = Category.MOVEMENT;
	private Module focused;
	private Module binding;
	private NumberSetting sliding;

	private int moduleScroll;
	private int settingScroll;

	public ClickGuiScreen() {
		super(Component.literal(NHack.NAME));
	}

	@Override
	protected void init() {
		if (!placed) {
			winX = (this.width - WINDOW_W) / 2;
			winY = (this.height - WINDOW_H) / 2;
			placed = true;
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.minecraft != null && this.minecraft.level != null) {
			this.renderBlurredBackground(graphics);
		}
		graphics.fill(0, 0, this.width, this.height, 0x35000000);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Font font = this.font;
		int x = winX;
		int y = winY;
		int x2 = x + WINDOW_W;
		int y2 = y + WINDOW_H;

		fillGlass(graphics, x, y, x2, y2, ColorUtil.GLASS);
		graphics.fill(x, y, x2, y + HEADER, ColorUtil.GLASS_HEADER);
		graphics.fill(x, y + HEADER, x + SIDEBAR, y2, ColorUtil.GLASS_DARK);
		graphics.fill(x2 - SETTINGS, y + HEADER, x2, y2, ColorUtil.GLASS_DARK);
		outline(graphics, x, y, x2, y2, ColorUtil.ACCENT);
		graphics.fill(x, y, x2, y + 1, ColorUtil.ACCENT);

		graphics.fill(x + 10, y + 9, x + 16, y + 21, ColorUtil.ACCENT);
		graphics.drawString(font, NHack.NAME, x + 22, y + 11, ColorUtil.TEXT);
		String version = NHack.VERSION;
		graphics.drawString(font, version, x2 - 12 - font.width(version), y + 11, ColorUtil.TEXT_DIM);

		renderSidebar(graphics, font, mouseX, mouseY);
		renderModules(graphics, font, mouseX, mouseY);
		renderSettings(graphics, font, mouseX, mouseY);
	}

	private void renderSidebar(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int y = winY + HEADER + 8;
		for (Category category : Category.values()) {
			boolean hover = hovered(mouseX, mouseY, winX + 8, y, SIDEBAR - 16, 26);
			boolean on = category == selected;
			if (on) {
				graphics.fill(winX + 8, y, winX + SIDEBAR - 8, y + 26, ColorUtil.ROW_ACTIVE);
				graphics.fill(winX + 8, y, winX + 10, y + 26, ColorUtil.ACCENT);
			} else if (hover) {
				graphics.fill(winX + 8, y, winX + SIDEBAR - 8, y + 26, ColorUtil.ROW_HOVER);
			}
			graphics.drawString(font, category.getDisplayName(), winX + 18, y + 9, on ? ColorUtil.TEXT : ColorUtil.TEXT_DIM);
			y += 30;
		}
	}

	private void renderModules(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int listX = winX + SIDEBAR;
		int listY = winY + HEADER;
		int listW = WINDOW_W - SIDEBAR - SETTINGS;
		int listH = WINDOW_H - HEADER;
		int contentY = listY + PAD;

		enableScissor(graphics, listX, listY, listX + listW, listY + listH);

		List<Module> modules = ModuleManager.getByCategory(selected);
		if (modules.isEmpty()) {
			String empty = "Nothing here yet";
			graphics.drawString(font, empty, listX + (listW - font.width(empty)) / 2, listY + listH / 2 - 4, ColorUtil.TEXT_DIM);
			graphics.disableScissor();
			return;
		}

		int cursor = contentY - moduleScroll;
		for (Module module : modules) {
			if (cursor + ROW >= listY && cursor <= listY + listH) {
				boolean hover = hovered(mouseX, mouseY, listX + 8, cursor, listW - 16, ROW);
				boolean on = module == focused;
				int bg = on ? ColorUtil.ROW_ACTIVE : (hover ? ColorUtil.ROW_HOVER : ColorUtil.ROW);
				graphics.fill(listX + 8, cursor, listX + listW - 8, cursor + ROW, bg);
				if (module.isEnabled()) {
					graphics.fill(listX + 8, cursor, listX + 10, cursor + ROW, ColorUtil.ACCENT);
				}
				graphics.drawString(font, module.getName(), listX + 16, cursor + 7, module.isEnabled() ? ColorUtil.ENABLED : ColorUtil.TEXT_DIM);
				drawSwitch(graphics, listX + listW - 36, cursor + 6, module.isEnabled());
			}
			cursor += ROW + 4;
		}

		graphics.disableScissor();
	}

	private void renderSettings(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
		int sx = winX + WINDOW_W - SETTINGS;
		int sy = winY + HEADER;
		int sw = SETTINGS;
		int sh = WINDOW_H - HEADER;

		if (focused == null || focused.getCategory() != selected) {
			String hint = "Select a module";
			graphics.drawString(font, hint, sx + (sw - font.width(hint)) / 2, sy + sh / 2 - 4, ColorUtil.TEXT_DIM);
			return;
		}

		enableScissor(graphics, sx, sy, sx + sw, sy + sh);

		int cursor = sy + PAD - settingScroll;
		graphics.drawString(font, focused.getName(), sx + 12, cursor, ColorUtil.TEXT);
		cursor += 14;
		drawWrapped(graphics, font, focused.getDescription(), sx + 12, cursor, sw - 24, ColorUtil.TEXT_DIM);
		cursor += 28;

		cursor = drawSettingRow(graphics, font, mouseX, mouseY, sx, sw, cursor, "Bind", binding == focused ? "..." : KeyUtil.name(focused.getBind()), true);

		for (Setting<?> setting : focused.getSettings()) {
			if (setting instanceof BoolSetting bool) {
				cursor = drawSettingRow(graphics, font, mouseX, mouseY, sx, sw, cursor, setting.getName(), bool.get() ? "ON" : "OFF", bool.get());
			} else if (setting instanceof ModeSetting mode) {
				cursor = drawSettingRow(graphics, font, mouseX, mouseY, sx, sw, cursor, setting.getName(), mode.get(), false);
			} else if (setting instanceof NumberSetting number) {
				boolean hover = hovered(mouseX, mouseY, sx + 10, cursor, sw - 20, 34);
				graphics.fill(sx + 10, cursor, sx + sw - 10, cursor + 34, hover ? ColorUtil.ROW_HOVER : ColorUtil.ROW);
				graphics.drawString(font, setting.getName(), sx + 16, cursor + 5, ColorUtil.TEXT_DIM);
				String value = number.formatted();
				graphics.drawString(font, value, sx + sw - 16 - font.width(value), cursor + 5, ColorUtil.TEXT);
				int barX = sx + 16;
				int barW = sw - 32;
				int barY = cursor + 22;
				graphics.fill(barX, barY, barX + barW, barY + 3, 0x55000000);
				double progress = (number.get() - number.getMin()) / (number.getMax() - number.getMin());
				int filled = (int) (barW * progress);
				graphics.fill(barX, barY, barX + filled, barY + 3, ColorUtil.ACCENT);
				graphics.fill(barX + Math.max(0, filled - 1), barY - 2, barX + filled + 2, barY + 5, ColorUtil.ACCENT_HOVER);
				cursor += 40;
			}
		}

		graphics.disableScissor();
	}

	private int drawSettingRow(GuiGraphics graphics, Font font, int mouseX, int mouseY, int sx, int sw, int cursor, String name, String value, boolean accentValue) {
		boolean hover = hovered(mouseX, mouseY, sx + 10, cursor, sw - 20, ROW);
		graphics.fill(sx + 10, cursor, sx + sw - 10, cursor + ROW, hover ? ColorUtil.ROW_HOVER : ColorUtil.ROW);
		graphics.drawString(font, name, sx + 16, cursor + 7, ColorUtil.TEXT_DIM);
		graphics.drawString(font, value, sx + sw - 16 - font.width(value), cursor + 7, accentValue ? ColorUtil.ACCENT_HOVER : ColorUtil.TEXT);
		return cursor + ROW + 4;
	}

	private static void drawSwitch(GuiGraphics graphics, int x, int y, boolean on) {
		graphics.fill(x, y, x + 20, y + 10, on ? ColorUtil.ACCENT : 0x66000000);
		int knob = on ? x + 11 : x + 1;
		graphics.fill(knob, y + 1, knob + 8, y + 9, 0xFFF2F2F4);
	}

	private static void drawWrapped(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, int color) {
		String remaining = text;
		int line = 0;
		while (!remaining.isEmpty() && line < 2) {
			if (font.width(remaining) <= maxWidth) {
				graphics.drawString(font, remaining, x, y + line * 10, color);
				return;
			}
			int cut = remaining.length();
			while (cut > 0 && font.width(remaining.substring(0, cut)) > maxWidth) {
				cut--;
			}
			int space = remaining.lastIndexOf(' ', cut);
			if (space > 4) {
				cut = space;
			}
			graphics.drawString(font, remaining.substring(0, cut).trim(), x, y + line * 10, color);
			remaining = remaining.substring(cut).trim();
			line++;
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		int button = click.button();

		if (!hovered(mouseX, mouseY, winX, winY, WINDOW_W, WINDOW_H)) {
			return super.mouseClicked(click, doubled);
		}

		if (hovered(mouseX, mouseY, winX, winY, WINDOW_W, HEADER) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			dragging = true;
			dragOffX = mouseX - winX;
			dragOffY = mouseY - winY;
			return true;
		}

		int tabY = winY + HEADER + 8;
		for (Category category : Category.values()) {
			if (hovered(mouseX, mouseY, winX + 8, tabY, SIDEBAR - 16, 26)) {
				selected = category;
				if (focused != null && focused.getCategory() != selected) {
					focused = null;
				}
				moduleScroll = 0;
				settingScroll = 0;
				return true;
			}
			tabY += 30;
		}

		if (handleModuleClick(mouseX, mouseY, button)) {
			return true;
		}
		if (handleSettingClick(mouseX, mouseY, button)) {
			return true;
		}
		return true;
	}

	private boolean handleModuleClick(int mouseX, int mouseY, int button) {
		int listX = winX + SIDEBAR;
		int listY = winY + HEADER;
		int listW = WINDOW_W - SIDEBAR - SETTINGS;
		int listH = WINDOW_H - HEADER;
		if (!hovered(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		int cursor = listY + PAD - moduleScroll;
		for (Module module : ModuleManager.getByCategory(selected)) {
			if (hovered(mouseX, mouseY, listX + 8, cursor, listW - 16, ROW)) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					if (hovered(mouseX, mouseY, listX + listW - 40, cursor, 32, ROW)) {
						module.toggle();
					} else {
						focused = focused == module ? null : module;
						settingScroll = 0;
					}
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					module.toggle();
					focused = module;
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					focused = module;
					binding = module;
					return true;
				}
			}
			cursor += ROW + 4;
		}
		return false;
	}

	private boolean handleSettingClick(int mouseX, int mouseY, int button) {
		if (focused == null || focused.getCategory() != selected) {
			return false;
		}

		int sx = winX + WINDOW_W - SETTINGS;
		int sy = winY + HEADER;
		int sw = SETTINGS;
		int sh = WINDOW_H - HEADER;
		if (!hovered(mouseX, mouseY, sx, sy, sw, sh)) {
			return false;
		}

		int cursor = sy + PAD - settingScroll + 42;
		if (hovered(mouseX, mouseY, sx + 10, cursor, sw - 20, ROW)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				binding = focused;
				return true;
			}
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				focused.setBind(GLFW.GLFW_KEY_UNKNOWN);
				return true;
			}
		}
		cursor += ROW + 4;

		for (Setting<?> setting : focused.getSettings()) {
			if (setting instanceof NumberSetting number) {
				if (hovered(mouseX, mouseY, sx + 10, cursor, sw - 20, 34) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					sliding = number;
					slide(number, mouseX, sx);
					return true;
				}
				cursor += 40;
				continue;
			}

			if (hovered(mouseX, mouseY, sx + 10, cursor, sw - 20, ROW) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				if (setting instanceof BoolSetting bool) {
					bool.toggle();
					return true;
				}
				if (setting instanceof ModeSetting mode) {
					mode.cycle(1);
					return true;
				}
			}
			cursor += ROW + 4;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		dragging = false;
		sliding = null;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
		if (dragging) {
			winX = (int) click.x() - dragOffX;
			winY = (int) click.y() - dragOffY;
			winX = Math.max(4, Math.min(this.width - WINDOW_W - 4, winX));
			winY = Math.max(4, Math.min(this.height - WINDOW_H - 4, winY));
			return true;
		}
		if (sliding != null) {
			slide(sliding, click.x(), winX + WINDOW_W - SETTINGS);
			return true;
		}
		return super.mouseDragged(click, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int listX = winX + SIDEBAR;
		int listY = winY + HEADER;
		int listW = WINDOW_W - SIDEBAR - SETTINGS;
		int listH = WINDOW_H - HEADER;
		int delta = (int) (-scrollY * 14);

		if (hovered(mouseX, mouseY, listX, listY, listW, listH)) {
			int max = Math.max(0, ModuleManager.getByCategory(selected).size() * (ROW + 4) - listH + 20);
			moduleScroll = Math.max(0, Math.min(max, moduleScroll + delta));
			return true;
		}

		if (hovered(mouseX, mouseY, winX + WINDOW_W - SETTINGS, listY, SETTINGS, listH)) {
			settingScroll = Math.max(0, settingScroll + delta);
			return true;
		}

		if (focused != null && hovered(mouseX, mouseY, winX + WINDOW_W - SETTINGS, listY, SETTINGS, listH)) {
			for (Setting<?> setting : focused.getSettings()) {
				if (setting instanceof NumberSetting number) {
					number.increment(scrollY > 0 ? 1 : -1);
					return true;
				}
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (binding != null) {
			int key = input.key();
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
				binding.setBind(GLFW.GLFW_KEY_UNKNOWN);
			} else {
				binding.setBind(key);
			}
			binding = null;
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return binding == null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		ModuleManager.get(ClickGuiModule.class).ifPresent(module -> module.setEnabled(false, false));
		ConfigManager.save();
		super.onClose();
	}

	private void slide(NumberSetting setting, double mouseX, int panelX) {
		int barX = panelX + 16;
		int barW = SETTINGS - 32;
		double progress = (mouseX - barX) / barW;
		progress = Math.max(0.0, Math.min(1.0, progress));
		setting.set(setting.getMin() + (setting.getMax() - setting.getMin()) * progress);
	}

	private static void fillGlass(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		graphics.fill(x1, y1, x2, y2, color);
	}

	private static void outline(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		graphics.fill(x1, y1, x2, y1 + 1, color);
		graphics.fill(x1, y2 - 1, x2, y2, ColorUtil.withAlpha(color, 0x66));
		graphics.fill(x1, y1, x1 + 1, y2, ColorUtil.withAlpha(color, 0x88));
		graphics.fill(x2 - 1, y1, x2, y2, ColorUtil.withAlpha(color, 0x88));
	}

	private static void enableScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
		graphics.enableScissor(x1, y1, x2, y2);
	}

	private static boolean hovered(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
}
