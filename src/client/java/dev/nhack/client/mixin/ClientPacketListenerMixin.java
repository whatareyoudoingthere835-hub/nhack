package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.PacketSendEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void nhack$onSend(Packet<?> packet, CallbackInfo ci) {
		PacketSendEvent event = EventBus.post(new PacketSendEvent(packet));
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
