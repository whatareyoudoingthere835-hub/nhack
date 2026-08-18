package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.PacketReceiveEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
	@Inject(method = {"genericsFtw", "handlePacket"}, at = @At("HEAD"), cancellable = true)
	private static void nhack$receive(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
		if (!(listener instanceof ClientPacketListener)) {
			return;
		}
		PacketReceiveEvent event = EventBus.post(new PacketReceiveEvent(packet));
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
