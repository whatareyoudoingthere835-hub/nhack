package dev.nhack.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water and lava are hidden together with the blocks, otherwise a lake would cover the exposed
 * diamond ore underneath it. Full argument list, see {@link ModelBlockRendererMixin}.
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
	@Inject(method = "tesselate", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayFluid(
		BlockAndTintGetter level, BlockPos pos, VertexConsumer builder, BlockState blockState, FluidState fluidState,
		CallbackInfo ci
	) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("fluids");
			ci.cancel();
		}
	}
}
