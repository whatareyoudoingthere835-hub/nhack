package dev.nhack.client.event.events;

import dev.nhack.client.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public final class HudRenderEvent extends Event {
	private final GuiGraphics graphics;
	private final DeltaTracker deltaTracker;

	public HudRenderEvent(GuiGraphics graphics, DeltaTracker deltaTracker) {
		this.graphics = graphics;
		this.deltaTracker = deltaTracker;
	}

	public GuiGraphics graphics() {
		return graphics;
	}

	public DeltaTracker deltaTracker() {
		return deltaTracker;
	}
}
