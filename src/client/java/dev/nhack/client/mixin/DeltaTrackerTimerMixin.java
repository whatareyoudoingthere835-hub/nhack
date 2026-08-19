package dev.nhack.client.mixin;

import dev.nhack.client.util.TickManager;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class DeltaTrackerTimerMixin {
	@Shadow
	private float deltaTicks;

	@Inject(
		method = "advanceGameTime",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;deltaTicks:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER)
	)
	private void nhack$scaleTimer(long timeMillis, CallbackInfoReturnable<Integer> cir) {
		float speed = TickManager.speed();
		if (speed != 1.0F) {
			this.deltaTicks *= speed;
		}
	}
}
