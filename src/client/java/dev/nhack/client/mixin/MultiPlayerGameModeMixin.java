package dev.nhack.client.mixin;

import dev.nhack.client.event.EventBus;
import dev.nhack.client.event.events.AttackEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives client modules the same attack boundary used by vanilla and KillAura. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void nhack$attack(Player player, Entity target, CallbackInfo ci) {
		if (player instanceof LocalPlayer) {
			EventBus.post(new AttackEvent(player, target));
		}
	}
}
