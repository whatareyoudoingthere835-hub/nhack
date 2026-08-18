package dev.nhack.client.module.modules.client;

import dev.nhack.NHack;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

public final class HudModule extends Module {
	private final BoolSetting watermark = addSetting(new BoolSetting("Watermark", "Draws the client name", true));
	private final BoolSetting arraylist = addSetting(new BoolSetting("ArrayList", "Lists enabled modules", true));
	private final NumberSetting offset = addSetting(new NumberSetting("Offset", "Padding from the screen edge", 4, 0, 20, 1));

	public HudModule() {
		super("HUD", "In-game overlay", Category.CLIENT);
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) {
			return;
		}

		GuiGraphics graphics = event.graphics();
		Font font = mc.font;
		int pad = offset.getInt();

		if (watermark.get()) {
			String text = NHack.NAME + " " + NHack.VERSION;
			graphics.fill(pad - 2, pad - 2, pad + font.width(text) + 4, pad + 11, ColorUtil.PANEL);
			graphics.fill(pad - 2, pad - 2, pad, pad + 11, ColorUtil.ACCENT);
			graphics.drawString(font, text, pad + 2, pad, ColorUtil.TEXT);
		}

		if (!arraylist.get()) {
			return;
		}

		List<Module> enabled = ModuleManager.getEnabled().stream()
			.filter(module -> module != this)
			.sorted(Comparator.comparingInt((Module module) -> font.width(module.getName())).reversed())
			.toList();

		int y = pad;
		for (Module module : enabled) {
			String name = module.getName();
			int width = font.width(name);
			int x = graphics.guiWidth() - pad - width;
			graphics.fill(x - 4, y - 1, graphics.guiWidth() - pad + 2, y + 10, ColorUtil.PANEL);
			graphics.fill(graphics.guiWidth() - pad + 1, y - 1, graphics.guiWidth() - pad + 2, y + 10, ColorUtil.ACCENT);
			graphics.drawString(font, name, x, y, ColorUtil.ENABLED);
			y += 11;
		}
	}
}
