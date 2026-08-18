package dev.nhack.client.gui;

import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.setting.Setting;
import dev.nhack.client.util.ColorUtil;
import dev.nhack.client.util.KeyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class Panel {
	public static final int WIDTH = 108;
	public static final int HEADER = 16;
	public static final int ROW = 14;

	private final Category category;
	private int x;
	private int y;
	private boolean open = true;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private Module expanded;
	private Module binding;
	private NumberSetting sliding;

	public Panel(Category category, int x, int y) {
		this.category = category;
		this.x = x;
		this.y = y;
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		List<Module> modules = ModuleManager.getByCategory(category);

		graphics.fill(x, y, x + WIDTH, y + HEADER, ColorUtil.PANEL_HEADER);
		graphics.fill(x, y, x + 2, y + HEADER, ColorUtil.ACCENT);
		graphics.drawString(font, category.getDisplayName(), x + 6, y + 4, ColorUtil.TEXT);
		graphics.drawString(font, open ? "-" : "+", x + WIDTH - 10, y + 4, ColorUtil.TEXT_DIM);

		if (!open) {
			return;
		}

		int cursor = y + HEADER;
		for (Module module : modules) {
			boolean hover = hovered(mouseX, mouseY, x, cursor, WIDTH, ROW);
			int background = module.isEnabled() ? ColorUtil.withAlpha(ColorUtil.ACCENT, hover ? 0xBB : 0x99)
				: (hover ? ColorUtil.MODULE_HOVER : ColorUtil.MODULE);
			graphics.fill(x, cursor, x + WIDTH, cursor + ROW, background);
			graphics.drawString(font, module.getName(), x + 6, cursor + 3, module.isEnabled() ? ColorUtil.ENABLED : ColorUtil.TEXT_DIM);
			cursor += ROW;

			if (expanded != module) {
				continue;
			}

			cursor = renderBindRow(graphics, font, module, mouseX, mouseY, cursor);
			for (Setting<?> setting : module.getSettings()) {
				cursor = renderSetting(graphics, font, setting, mouseX, mouseY, cursor);
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (hovered(mouseX, mouseY, x, y, WIDTH, HEADER)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				dragging = true;
				dragOffsetX = (int) mouseX - x;
				dragOffsetY = (int) mouseY - y;
				return true;
			}
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				open = !open;
				return true;
			}
		}

		if (!open) {
			return false;
		}

		int cursor = y + HEADER;
		for (Module module : ModuleManager.getByCategory(category)) {
			if (hovered(mouseX, mouseY, x, cursor, WIDTH, ROW)) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					module.toggle();
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					expanded = expanded == module ? null : module;
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					binding = module;
					return true;
				}
			}
			cursor += ROW;

			if (expanded != module) {
				continue;
			}

			if (hovered(mouseX, mouseY, x, cursor, WIDTH, ROW)) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
					binding = module;
					return true;
				}
				if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					module.setBind(GLFW.GLFW_KEY_UNKNOWN);
					return true;
				}
			}
			cursor += ROW;

			for (Setting<?> setting : module.getSettings()) {
				if (hovered(mouseX, mouseY, x, cursor, WIDTH, ROW)) {
					if (setting instanceof BoolSetting bool && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
						bool.toggle();
						return true;
					}
					if (setting instanceof ModeSetting mode && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
						mode.cycle(1);
						return true;
					}
					if (setting instanceof NumberSetting number && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
						sliding = number;
						slide(number, mouseX);
						return true;
					}
				}
				cursor += ROW;
			}
		}
		return false;
	}

	public void mouseReleased() {
		dragging = false;
		sliding = null;
	}

	public void mouseDragged(double mouseX, double mouseY) {
		if (dragging) {
			x = (int) mouseX - dragOffsetX;
			y = (int) mouseY - dragOffsetY;
		}
		if (sliding != null) {
			slide(sliding, mouseX);
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!open) {
			return false;
		}

		int cursor = y + HEADER;
		for (Module module : ModuleManager.getByCategory(category)) {
			cursor += ROW;
			if (expanded != module) {
				continue;
			}
			cursor += ROW;
			for (Setting<?> setting : module.getSettings()) {
				if (setting instanceof NumberSetting number && hovered(mouseX, mouseY, x, cursor, WIDTH, ROW)) {
					number.increment(amount > 0 ? 1 : -1);
					return true;
				}
				cursor += ROW;
			}
		}
		return false;
	}

	public boolean keyPressed(int key) {
		if (binding == null) {
			return false;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
			binding.setBind(GLFW.GLFW_KEY_UNKNOWN);
		} else {
			binding.setBind(key);
		}
		binding = null;
		return true;
	}

	private int renderBindRow(GuiGraphics graphics, Font font, Module module, int mouseX, int mouseY, int cursor) {
		boolean hover = hovered(mouseX, mouseY, x, cursor, WIDTH, ROW);
		graphics.fill(x, cursor, x + WIDTH, cursor + ROW, hover ? ColorUtil.MODULE_HOVER : 0xD0101014);
		String value = binding == module ? "..." : KeyUtil.name(module.getBind());
		graphics.drawString(font, "Bind", x + 8, cursor + 3, ColorUtil.TEXT_DIM);
		graphics.drawString(font, value, x + WIDTH - 6 - font.width(value), cursor + 3, ColorUtil.TEXT);
		return cursor + ROW;
	}

	private int renderSetting(GuiGraphics graphics, Font font, Setting<?> setting, int mouseX, int mouseY, int cursor) {
		boolean hover = hovered(mouseX, mouseY, x, cursor, WIDTH, ROW);
		graphics.fill(x, cursor, x + WIDTH, cursor + ROW, hover ? ColorUtil.MODULE_HOVER : 0xD0101014);
		graphics.drawString(font, setting.getName(), x + 8, cursor + 3, ColorUtil.TEXT_DIM);

		if (setting instanceof BoolSetting bool) {
			String value = bool.get() ? "ON" : "OFF";
			graphics.drawString(font, value, x + WIDTH - 6 - font.width(value), cursor + 3, bool.get() ? ColorUtil.ACCENT_HOVER : ColorUtil.TEXT_DIM);
		} else if (setting instanceof ModeSetting mode) {
			String value = mode.get();
			graphics.drawString(font, value, x + WIDTH - 6 - font.width(value), cursor + 3, ColorUtil.TEXT);
		} else if (setting instanceof NumberSetting number) {
			String value = number.formatted();
			graphics.drawString(font, value, x + WIDTH - 6 - font.width(value), cursor + 3, ColorUtil.TEXT);
			double progress = (number.get() - number.getMin()) / (number.getMax() - number.getMin());
			int bar = (int) ((WIDTH - 16) * progress);
			graphics.fill(x + 8, cursor + ROW - 2, x + 8 + bar, cursor + ROW - 1, ColorUtil.ACCENT);
		}
		return cursor + ROW;
	}

	private void slide(NumberSetting setting, double mouseX) {
		double progress = (mouseX - (x + 8)) / (WIDTH - 16);
		progress = Math.max(0.0, Math.min(1.0, progress));
		setting.set(setting.getMin() + (setting.getMax() - setting.getMin()) * progress);
	}

	private static boolean hovered(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
}
