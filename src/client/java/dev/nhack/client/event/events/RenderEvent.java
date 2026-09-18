package dev.nhack.client.event.events;

import dev.nhack.client.event.Event;
import net.minecraft.client.DeltaTracker;

/**
 * Событие кадра. Постится из {@code GameRenderer.render} один раз на кадр, <b>до</b> настройки
 * камеры, поэтому всё, что здесь меняет поворот/позицию, попадает ещё в текущий кадр.
 *
 * <p>Нужно для всего, что должно двигаться плавно: тиковые события дают лишь 20 обновлений в
 * секунду, из-за чего наводка ауры выглядит рывками.
 */
public final class RenderEvent extends Event {
	private final DeltaTracker deltaTracker;
	private final boolean renderLevel;

	public RenderEvent(DeltaTracker deltaTracker, boolean renderLevel) {
		this.deltaTracker = deltaTracker;
		this.renderLevel = renderLevel;
	}

	public DeltaTracker deltaTracker() {
		return deltaTracker;
	}

	/** {@code false}, когда рендерится только GUI/меню. */
	public boolean renderLevel() {
		return renderLevel;
	}

	/**
	 * Сколько игрового времени прошло с прошлого кадра, в тиках: {@code 0.5} при 40 FPS,
	 * {@code 0.33} при 60 FPS. Учитывает множитель таймера, так что скорость не зависит от FPS.
	 */
	public float deltaTime() {
		return deltaTracker.getGameTimeDeltaTicks();
	}

	/** Частичный тик (0..1) для интерполяции позиций и поворота между тиками. */
	public float partialTick() {
		return deltaTracker.getGameTimeDeltaPartialTick(true);
	}
}
