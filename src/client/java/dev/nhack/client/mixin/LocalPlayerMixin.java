package dev.nhack.client.mixin;

import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.movement.NoSlowModule;
import dev.nhack.client.util.RotationUtil;
import dev.nhack.client.util.TickManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Inject(method = "sendPosition", at = @At("HEAD"))
	private void nhack$applyAuraRotation(CallbackInfo ci) {
		if (!RotationUtil.active) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		player.setYRot(RotationUtil.yaw);
		player.setXRot(RotationUtil.pitch);
	}

	@Inject(method = "sendPosition", at = @At("TAIL"))
	private void nhack$restoreVisualRotation(CallbackInfo ci) {
		if (!RotationUtil.active || RotationUtil.clientLook) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		player.setYRot(RotationUtil.visualYaw);
		player.setXRot(RotationUtil.visualPitch);
	}

	@Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
	private boolean nhack$noSlowItem(LocalPlayer player) {
		if (NoSlowModule.shouldCancelItemSlow()) {
			return false;
		}
		return player.isUsingItem();
	}

	@Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
	private void nhack$noSlowSneak(CallbackInfoReturnable<Boolean> cir) {
		ModuleManager.get(NoSlowModule.class).ifPresent(module -> {
			if (module.shouldCancelSneakSlow()) {
				cir.setReturnValue(false);
			}
		});
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void nhack$timerShift(CallbackInfo ci) {
		int extra = TickManager.consumeShift();
		if (extra <= 0 || TickManager.shifting()) {
			return;
		}
		TickManager.beginShift();
		try {
			LocalPlayer player = (LocalPlayer) (Object) this;
			for (int i = 0; i < extra; i++) {
				player.tick();
			}
		} finally {
			TickManager.endShift();
		}
	}
}
