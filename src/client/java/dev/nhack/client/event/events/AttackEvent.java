package dev.nhack.client.event.events;

import dev.nhack.client.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Posted immediately before the vanilla client sends an entity attack packet. */
public final class AttackEvent extends Event {
	private final Player attacker;
	private final Entity target;

	public AttackEvent(Player attacker, Entity target) {
		this.attacker = attacker;
		this.target = target;
	}

	public Player attacker() {
		return attacker;
	}

	public Entity target() {
		return target;
	}
}
