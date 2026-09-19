package dev.nhack.client.mixin;

import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.combat.KillAuraModule;
import dev.nhack.client.module.modules.movement.NoSlowModule;
import dev.nhack.client.util.CombatUtil;
import dev.nhack.client.util.RotationUtil;
import dev.nhack.client.util.TickManager;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	/** Original input kept while vanilla converts WASD into world movement. */
	private Vec2 nhack$moveFixOriginal;

	/**
	 * When KillAura is silent, rotate WASD by server-yaw minus camera-yaw. Vanilla still moves
	 * relative to the visible camera, but the resulting world vector points where the aura aims.
	 */
	@Inject(method = "applyInput", at = @At("HEAD"))
	private void nhack$applyMoveFix(CallbackInfo ci) {
		nhack$moveFixOriginal = null;
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (!ModuleManager.get(KillAuraModule.class).map(KillAuraModule::shouldMoveFix).orElse(false)
			|| player.input == null) {
			return;
		}

		ClientInputAccessor input = (ClientInputAccessor) player.input;
		Vec2 original = input.nhack$getMoveVector();
		if (original == null || original.lengthSquared() == 0.0F) {
			return;
		}

		float delta = Mth.wrapDegrees(RotationUtil.yaw - player.getYRot()) * Mth.DEG_TO_RAD;
		float sin = Mth.sin(delta);
		float cos = Mth.cos(delta);
		Vec2 corrected = new Vec2(
			original.x * cos - original.y * sin,
			original.y * cos + original.x * sin
		);
		nhack$moveFixOriginal = original;
		input.nhack$setMoveVector(corrected);
	}

	@Inject(method = "applyInput", at = @At("TAIL"))
	private void nhack$restoreMoveFix(CallbackInfo ci) {
		if (nhack$moveFixOriginal == null) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (player.input != null) {
			((ClientInputAccessor) player.input).nhack$setMoveVector(nhack$moveFixOriginal);
		}
		nhack$moveFixOriginal = null;
	}

	/**
	 * Сброс спринта руками античит ловит: пакет должен уйти из ванильного места. Первая строка
	 * {@code sendPosition} — это {@code sendIsSprintingIfNeeded()}, поэтому снимаем флаг на HEAD
	 * и ваниль сама отправляет {@code STOP_SPRINTING} ровно там, где отправляет его обычный игрок.
	 */
	@Inject(method = "sendPosition", at = @At("HEAD"))
	private void nhack$sprintReset(CallbackInfo ci) {
		if (CombatUtil.consumeSprintDrop()) {
			((LocalPlayer) (Object) this).setSprinting(false);
		}
	}

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
		if (!RotationUtil.active) {
			return;
		}
		// Пакет уже ушёл — запоминаем, какой поворот теперь знает сервер.
		RotationUtil.markSent();
		if (RotationUtil.clientLook) {
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
