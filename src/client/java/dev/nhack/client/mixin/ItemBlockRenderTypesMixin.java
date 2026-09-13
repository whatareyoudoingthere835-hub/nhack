package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Semi-transparent blocks have to be compiled into the translucent section layer,
 * otherwise the alpha written by {@link ModelBlockRendererMixin} is simply ignored.
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
	@Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
	private static void nhack$xrayLayer(BlockState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
		int alpha = XrayModule.alpha(state, null);
		if (alpha > 0 && alpha < 255) {
			cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
		}
	}

	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
	private static void nhack$xrayFluidLayer(FluidState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
		int alpha = XrayModule.alpha(state.createLegacyBlock(), null);
		if (alpha > 0 && alpha < 255) {
			cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
		}
	}
}
