package dev.nhack.client.module.modules.movement;

import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SprintModule extends Module {
	private final ModeSetting mode = addSetting(new ModeSetting(
		"Mode",
		"Legit only sprints when moving forward",
		"Legit",
		"Legit", "Rage"
	));

	public SprintModule() {
		super("Sprint", "Keeps sprint toggled while you move", Category.MOVEMENT);
	}

	@Override
	public void onTick(TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isShiftKeyDown() || player.horizontalCollision) {
			return;
		}
		if (player.isSprinting()) {
			return;
		}

		if (mode.is("Legit")) {
			if (player.input != null && player.input.hasForwardImpulse()) {
				player.setSprinting(true);
			}
			return;
		}

		if (player.getFoodData().getFoodLevel() > 6) {
			player.setSprinting(true);
		}
	}
}
