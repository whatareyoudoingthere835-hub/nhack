package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.PacketSendEvent;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code ClientPacketListener.send(Packet)} is inherited from this parent on 1.21.11.
 * Mixin cannot inject into inherited methods, so the hook lives here.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void nhack$onSend(Packet<?> packet, CallbackInfo ci) {
		PacketSendEvent event = EventBus.post(new PacketSendEvent(packet));
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
