package dev.nhack.client.module.modules.misc;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Skeleton module. Copy this class, rename it, register it in {@code ModuleManager.init()}.
 */
public final class ExampleModule extends Module {
	private final BoolSetting announce = addSetting(new BoolSetting("Announce", "Prints a chat line on enable", true));
	private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between demo actions", 40, 5, 200, 1));
	private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Demo mode switch", "A", "A", "B"));

	private int ticks;

	public ExampleModule() {
		super("Example", "Template module — replace this with your own logic", Category.MISC);
	}

	@Override
	protected void onEnable() {
		ticks = 0;
		if (announce.get() && Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(
				net.minecraft.network.chat.Component.literal("Example enabled (" + mode.get() + ")"),
				true
			);
		}
	}

	@Override
	public void onTick(TickEvent.Post event) {
		ticks++;
		if (ticks >= delay.getInt()) {
			ticks = 0;
		}
	}

	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		// Event-bus example. Prefer onTick() unless you need Pre/custom events.
	}
}
