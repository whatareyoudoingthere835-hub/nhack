package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Xray core: alpha of every block model quad + face culling for the visible ores.
 * Chunk meshes are built on worker threads, hence the {@link ThreadLocal}.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Unique
	private static final ThreadLocal<Integer> NHACK_ALPHA = ThreadLocal.withInitial(() -> -1);

	@Inject(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At("HEAD"), cancellable = true)
	private void nhack$xrayAlpha(BlockAndTintGetter level, List<BlockModelPart> parts, BlockState state, BlockPos pos, CallbackInfo ci) {
		int alpha = XrayModule.alpha(state, pos);

		if (alpha == 0) {
			ci.cancel();
		} else {
			NHACK_ALPHA.set(alpha);
		}
	}

	@ModifyArg(
		method = "putQuadData",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"),
		index = 6
	)
	private float nhack$applyAlpha(float alpha) {
		int value = NHACK_ALPHA.get();
		return value == -1 ? alpha : value / 255.0F;
	}

	@Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
	private static void nhack$xrayFace(BlockAndTintGetter level, BlockState state, boolean cullEnabled, Direction direction, BlockPos neighborPos, CallbackInfoReturnable<Boolean> cir) {
		XrayModule xray = XrayModule.instance();
		if (xray == null || !xray.isEnabled()) {
			return;
		}

		// neighborPos is the checked neighbour — step back to get the block that is actually drawn
		cir.setReturnValue(xray.modifyDrawSide(state, level, neighborPos.relative(direction.getOpposite()), direction, cir.getReturnValue()));
	}
}
