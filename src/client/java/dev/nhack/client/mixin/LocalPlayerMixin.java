package dev.nhack.client.mixin;

import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.movement.NoSlowModule;
import dev.nhack.client.util.RotationUtil;
import dev.nhack.client.util.TickManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void nhack$noSlowItem(CallbackInfo ci) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (!NoSlowModule.shouldCancelItemSlow() || !player.isUsingItem() || player.input == null) {
			return;
		}

		ClientInput input = player.input;
		Input keys = input.keyPresses;
		float forward = (keys.forward() ? 1.0F : 0.0F) - (keys.backward() ? 1.0F : 0.0F);
		float left = (keys.left() ? 1.0F : 0.0F) - (keys.right() ? 1.0F : 0.0F);
		((ClientInputAccessor) input).nhack$setMoveVector(new Vec2(left, forward));
	}

	@Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true, require = 0)
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
