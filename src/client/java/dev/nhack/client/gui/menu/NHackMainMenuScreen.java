package dev.nhack.client.gui.menu;

import dev.nhack.NHack;
import dev.nhack.client.gui.ClickGuiScreen;
import dev.nhack.client.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class NHackMainMenuScreen extends Screen {
	private static NHackMainMenuScreen instance;
	public static boolean allowVanilla;

	private static final List<String> CHANGELOG = List.of(
		"[+] Fabric 1.21.11 template",
		"[+] Unified glass ClickGUI",
		"[+] Aura / Timer / NoSlow / ESP",
		"[*] Official Mojang mappings",
		"[/] Starfield title screen"
	);

	private final List<MainMenuButton> buttons = new ArrayList<>();
	private final List<Star> stars = new ArrayList<>();
	private final Random random = new Random();
	private int ticksActive;

	private NHackMainMenuScreen() {
		super(Component.literal(NHack.NAME));
		rebuildButtons();
		rebuildStars();
	}

	public static NHackMainMenuScreen getInstance() {
		if (instance == null) {
			instance = new NHackMainMenuScreen();
		}
		instance.ticksActive = 0;
		return instance;
	}

	private void rebuildButtons() {
		buttons.clear();
		Minecraft mc = Minecraft.getInstance();
		buttons.add(new MainMenuButton(-110, -70, I18n.get("menu.singleplayer").toUpperCase(Locale.ROOT),
			() -> mc.setScreen(new SelectWorldScreen(this))));
		buttons.add(new MainMenuButton(4, -70, I18n.get("menu.multiplayer").toUpperCase(Locale.ROOT),
			() -> mc.setScreen(new JoinMultiplayerScreen(this))));
		buttons.add(new MainMenuButton(-110, -29, I18n.get("menu.options").toUpperCase(Locale.ROOT).replace(".", ""),
			() -> mc.setScreen(new OptionsScreen(this, mc.options))));
		buttons.add(new MainMenuButton(4, -29, "CLICKGUI", () -> mc.setScreen(new ClickGuiScreen())));
		buttons.add(new MainMenuButton(-110, 12, I18n.get("menu.quit").toUpperCase(Locale.ROOT), mc::stop, true));
	}

	private void rebuildStars() {
		stars.clear();
		for (int i = 0; i < 500; i++) {
			stars.add(new Star(random));
		}
	}

	@Override
	protected void init() {
		rebuildButtons();
	}

	@Override
	public void tick() {
		ticksActive++;
		if (ticksActive > 400) {
			ticksActive = 0;
		}
		stars.forEach(Star::update);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Font font = this.font;
		int width = this.width;
		int height = this.height;
		float halfW = width / 2.0F;
		float halfH = height / 2.0F;

		renderStarfield(graphics, width, height);

		int panelX = Math.round(halfW - 120);
		int panelY = Math.round(halfH - 80);
		graphics.fill(panelX, panelY, panelX + 240, panelY + 140, ColorUtil.GLASS);
		graphics.fill(panelX, panelY, panelX + 240, panelY + 1, ColorUtil.ACCENT);
		graphics.fill(panelX, panelY, panelX + 1, panelY + 140, ColorUtil.withAlpha(ColorUtil.ACCENT, 0x66));
		graphics.fill(panelX + 239, panelY, panelX + 240, panelY + 140, ColorUtil.withAlpha(ColorUtil.ACCENT, 0x66));

		buttons.forEach(button -> button.render(graphics, mouseX, mouseY));

		boolean logoHover = hovered(mouseX, mouseY, (int) halfW - 120, (int) halfH - 130, 240, 40);
		String title = NHack.NAME;
		graphics.drawString(font, title, (int) halfW - font.width(title) / 2, (int) halfH - 118, logoHover ? 0xE6FFFFFF : 0xB4FFFFFF);
		String version = NHack.VERSION;
		graphics.drawString(font, version, (int) halfW - font.width(version) / 2, (int) halfH - 106, ColorUtil.TEXT_DIM);

		boolean backHover = hovered(mouseX, mouseY, (int) halfW - 50, (int) halfH + 70, 100, 12);
		String back = "<-- Back to default menu";
		graphics.drawString(font, back, (int) halfW - font.width(back) / 2, (int) halfH + 70, backHover ? ColorUtil.TEXT : ColorUtil.withAlpha(ColorUtil.TEXT, 0x99));

		int lineY = 10;
		int start = Math.max(0, CHANGELOG.size() - 5);
		for (int i = start; i < CHANGELOG.size(); i++) {
			graphics.drawString(font, prefix(CHANGELOG.get(i)), 10, lineY, ColorUtil.withAlpha(ColorUtil.TEXT, 0x66));
			lineY += 10;
		}

		String hint = "Right Shift · ClickGUI";
		graphics.drawString(font, hint, width - 10 - font.width(hint), height - 14, ColorUtil.TEXT_DIM);
	}

	private void renderStarfield(GuiGraphics graphics, int width, int height) {
		graphics.fillGradient(0, 0, width, height, 0xFF05050F, 0xFF12060C);
		long time = System.currentTimeMillis();
		for (Star star : stars) {
			float base = (float) (0.5 + 0.5 * Math.sin(time / 1000.0 * star.speed + star.phase));
			float twinkle = (float) (0.8 + 0.2 * Math.sin(time / 200.0 * star.twinkleSpeed + star.twinklePhase));
			float flash = star.size > 1.5F && random.nextFloat() < 0.008F ? 1.45F : 1.0F;
			float brightness = Math.min(1.0F, base * twinkle * flash);
			int alpha = (int) (brightness * 255);
			int sx = (int) (star.x * width);
			int sy = (int) (star.y * height);
			int size = Math.max(1, Math.round(star.size));
			int color = ColorUtil.rgba(255, 255, 255, alpha);
			graphics.fill(sx, sy, sx + size, sy + size, color);
			if (star.size > 1.6F && brightness > 0.75F) {
				graphics.fill(sx - 1, sy - 1, sx + size + 1, sy + size + 1, ColorUtil.rgba(255, 200, 210, alpha / 3));
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			buttons.forEach(button -> button.click(mouseX, mouseY));
			int halfW = this.width / 2;
			int halfH = this.height / 2;
			if (hovered(mouseX, mouseY, halfW - 50, halfH + 70, 100, 12)) {
				allowVanilla = true;
				Minecraft.getInstance().setScreen(new TitleScreen());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private static String prefix(String change) {
		if (change.contains("[+]")) {
			return ChatFormatting.GREEN + "[+] " + ChatFormatting.RESET + change.replace("[+] ", "");
		}
		if (change.contains("[-]")) {
			return ChatFormatting.RED + "[-] " + ChatFormatting.RESET + change.replace("[-] ", "");
		}
		if (change.contains("[/]")) {
			return ChatFormatting.LIGHT_PURPLE + "[/] " + ChatFormatting.RESET + change.replace("[/] ", "");
		}
		if (change.contains("[*]")) {
			return ChatFormatting.GOLD + "[*] " + ChatFormatting.RESET + change.replace("[*] ", "");
		}
		return change;
	}

	private static boolean hovered(int mouseX, int mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	private static final class Star {
		float x;
		float y;
		final float size;
		final float speed;
		final float phase;
		final float twinkleSpeed;
		final float twinklePhase;
		final float driftX;
		final float driftY;

		private Star(Random random) {
			this.x = random.nextFloat();
			this.y = random.nextFloat();
			this.size = 0.5F + random.nextFloat() * 1.8F;
			this.speed = 0.3F + random.nextFloat() * 1.2F;
			this.phase = random.nextFloat() * (float) Math.PI * 2.0F;
			this.twinkleSpeed = 0.5F + random.nextFloat() * 2.5F;
			this.twinklePhase = random.nextFloat() * (float) Math.PI * 2.0F;
			float depth = size / 2.0F;
			this.driftX = (random.nextFloat() - 0.5F) * 0.0008F * depth;
			this.driftY = (random.nextFloat() - 0.5F) * 0.0008F * depth - 0.0003F;
		}

		private void update() {
			x += driftX;
			y += driftY;
			if (x > 1.0F) {
				x -= 1.0F;
			}
			if (x < 0.0F) {
				x += 1.0F;
			}
			if (y > 1.0F) {
				y -= 1.0F;
			}
			if (y < 0.0F) {
				y += 1.0F;
			}
		}
	}
}
