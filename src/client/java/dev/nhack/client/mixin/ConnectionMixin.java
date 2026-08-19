package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.PacketReceiveEvent;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
	/**
	 * Official Mojang name is {@code genericsFtw}; Yarn calls the same method {@code handlePacket}.
	 */
	@Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
	private static void nhack$receive(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
		if (!(listener instanceof ClientCommonPacketListenerImpl)) {
			return;
		}
		PacketReceiveEvent event = EventBus.post(new PacketReceiveEvent(packet));
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
