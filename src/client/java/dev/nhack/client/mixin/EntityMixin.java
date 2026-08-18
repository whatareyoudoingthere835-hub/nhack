package dev.nhack.client.mixin;

import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.movement.NoSlowModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
	private void nhack$blockSpeed(CallbackInfoReturnable<Float> cir) {
		if (!((Object) this instanceof LocalPlayer)) {
			return;
		}
		ModuleManager.get(NoSlowModule.class).ifPresent(module -> {
			if (module.shouldCancelBlockSlow((Entity) (Object) this)) {
				cir.setReturnValue(1.0F);
			}
		});
	}

	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
	private void nhack$stuck(BlockState state, Vec3 motion, CallbackInfo ci) {
		if (!((Object) this instanceof LocalPlayer)) {
			return;
		}
		ModuleManager.get(NoSlowModule.class).ifPresent(module -> {
			if (module.shouldCancelBerrySlow()) {
				ci.cancel();
			}
		});
	}
}
