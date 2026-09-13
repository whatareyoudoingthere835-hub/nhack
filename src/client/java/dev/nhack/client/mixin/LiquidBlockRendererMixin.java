package dev.nhack.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water and lava get the same ghost treatment as blocks, otherwise a lava lake would hide the
 * exposed diamond ore underneath it.
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
	@Unique
	private static final ThreadLocal<Integer> NHACK_ALPHA = ThreadLocal.withInitial(() -> -1);

	@Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
	private void nhack$xrayFluid(BlockAndTintGetter level, BlockPos pos, VertexConsumer builder, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
		int alpha = XrayModule.alpha(fluidState.createLegacyBlock(), pos);

		if (alpha == 0) {
			ci.cancel();
		} else {
			NHACK_ALPHA.set(alpha);
		}
	}

	@ModifyArg(
		method = "vertex",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
		index = 3
	)
	private float nhack$xrayFluidAlpha(float alpha) {
		int value = NHACK_ALPHA.get();
		return value == -1 ? alpha : value / 255.0F;
	}
}
