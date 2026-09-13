package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block entities (chests, furnaces, shulkers, beds, ...) are hidden together with their block,
 * otherwise they would float fully opaque inside the ghost world.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void nhack$xrayBlockEntity(BlockEntity blockEntity, CallbackInfoReturnable<BlockEntityRenderState> cir) {
		XrayModule xray = XrayModule.instance();
		if (xray == null || !xray.isEnabled()) {
			return;
		}

		if (xray.isBlocked(blockEntity.getBlockState().getBlock(), blockEntity.getBlockPos())) {
			cir.setReturnValue(null);
		}
	}
}
