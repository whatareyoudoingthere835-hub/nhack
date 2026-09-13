package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block entities (chests, furnaces, shulkers, beds, ...) are hidden together with their block,
 * otherwise they would float fully opaque inside the invisible world. Returning {@code null} here is
 * the same thing vanilla does when a block entity has no renderer. Full argument list, see
 * {@link ModelBlockRendererMixin}.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayBlockEntity(
		BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress,
		CallbackInfoReturnable<BlockEntityRenderState> cir
	) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("blockEntity");
			if (XrayModule.isBlocked(blockEntity.getBlockState(), blockEntity.getBlockPos())) {
				cir.setReturnValue(null);
			}
		}
	}
}
