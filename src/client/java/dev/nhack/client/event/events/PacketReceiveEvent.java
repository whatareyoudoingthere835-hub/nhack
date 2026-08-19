package dev.nhack.client.event.events;

import dev.nhack.client.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

public final class PacketReceiveEvent extends CancellableEvent {
	private final Packet<?> packet;

	public PacketReceiveEvent(Packet<?> packet) {
		this.packet = packet;
	}

	public Packet<?> packet() {
		return packet;
	}
}
