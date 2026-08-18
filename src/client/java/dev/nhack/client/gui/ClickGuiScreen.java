package dev.nhack.client.gui;

import dev.nhack.NHack;
import dev.nhack.client.config.ConfigManager;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.modules.client.ClickGuiModule;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ClickGuiScreen extends Screen {
	private final List<Panel> panels = new ArrayList<>();

	public ClickGuiScreen() {
		super(Component.literal(NHack.NAME));
		int x = 12;
		for (Category category : Category.values()) {
			panels.add(new Panel(category, x, 24));
			x += Panel.WIDTH + 8;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x66000000);
		for (Panel panel : panels) {
			panel.render(graphics, mouseX, mouseY);
		}
		graphics.drawString(this.font, NHack.NAME + "  ·  rshift  ·  .help", 8, this.height - 12, ColorUtil.TEXT_DIM);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		for (int index = panels.size() - 1; index >= 0; index--) {
			if (panels.get(index).mouseClicked(click.x(), click.y(), click.button())) {
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		for (Panel panel : panels) {
			panel.mouseReleased();
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
		for (Panel panel : panels) {
			panel.mouseDragged(click.x(), click.y());
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (Panel panel : panels) {
			if (panel.mouseScrolled(mouseX, mouseY, scrollY)) {
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		for (Panel panel : panels) {
			if (panel.keyPressed(input.key())) {
				return true;
			}
		}
		return super.keyPressed(input);
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
}
