package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.TickEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Example mixin hook. Tick events are also posted from Fabric's ClientTickEvents
 * as a fallback — this inject is the in-game tick used by modules.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void nhack$preTick(CallbackInfo ci) {
		EventBus.post(new TickEvent.Pre());
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void nhack$postTick(CallbackInfo ci) {
		EventBus.post(new TickEvent.Post());
	}
}
