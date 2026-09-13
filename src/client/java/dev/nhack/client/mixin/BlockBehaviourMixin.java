package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ambient occlusion shade — forced to full brightness while xray is on
 * (Meteor's {@code AmbientOcclusionEvent}), so the exposed diamonds pop out of the ghost world.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
	@Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayShade(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("shade");
			cir.setReturnValue(1.0F);
		}
	}
}
