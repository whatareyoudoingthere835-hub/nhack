package dev.nhack.client.module.modules.client;

import dev.nhack.client.gui.ClickGuiScreen;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ClickGuiModule extends Module {
	public ClickGuiModule() {
		super("ClickGUI", "Opens the client interface", Category.CLIENT);
		setBind(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof ClickGuiScreen) {
			return;
		}
		mc.setScreen(new ClickGuiScreen());
	}

	@Override
	protected void onDisable() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof ClickGuiScreen) {
			mc.setScreen(null);
		}
	}
}
