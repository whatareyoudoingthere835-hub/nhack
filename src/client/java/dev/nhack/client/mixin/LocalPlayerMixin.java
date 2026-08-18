package dev.nhack.client.mixin;

import dev.nhack.client.util.RotationUtil;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
