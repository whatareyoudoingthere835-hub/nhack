package dev.nhack.client.util;

/**
 * Боевое состояние, которое нужно миксинам, а не только модулям.
 *
 * <p>Сброс спринта обязан уходить на сервер так, как это делает ваниль: флаг снимается перед
 * {@code LocalPlayer.sendPosition()}, и первая же строка метода ({@code sendIsSprintingIfNeeded})
 * сама отправляет {@code STOP_SPRINTING}. Ручной пакет из модуля попадает в очередь не туда —
 * а боевые пакеты вне ванильного порядка ловит Post-проверка Grim/Polar.
 */
public final class CombatUtil {
	private static boolean sprintDrop;
	private static boolean sendingAttack;

	private CombatUtil() {
	}

	/** Просит снять спринт в ближайшем {@code sendPosition} (то есть в этом же тике). */
	public static void requestSprintDrop() {
		sprintDrop = true;
	}

	/** Забирает запрос: {@code true} ровно один раз, дальше флаг чист. */
	public static boolean consumeSprintDrop() {
		boolean value = sprintDrop;
		sprintDrop = false;
		return value;
	}

	public static void clearSprintDrop() {
		sprintDrop = false;
	}

	/**
	 * Помечает, что боевой пакет отправляет одна из аур. Нужно, чтобы модули не глушили пакеты
	 * друг друга: обработчик {@code PacketSendEvent} одной ауры отменял {@code ServerboundInteractPacket}
	 * второй, и удары просто не доходили до сервера.
	 */
	public static void beginAttack() {
		sendingAttack = true;
	}

	public static void endAttack() {
		sendingAttack = false;
	}

	public static boolean isSendingAttack() {
		return sendingAttack;
	}
}
