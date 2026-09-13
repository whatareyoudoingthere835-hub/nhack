package dev.nhack.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Xray core for the vanilla chunk pipeline: drop the geometry of every blocked block and stop culling
 * the faces of the ore that stays visible.
 *
 * <p>Every callback declares the full target argument list — Mixin only accepts a handler that takes
 * either every argument of the target method or none at all; a truncated list injects nothing and then
 * fails with a confusing "(0/1) succeeded. Scanned 0 target(s)". The two tesselate methods get separate
 * injectors (instead of {@code method = {a, b}}) so a selector that fails to resolve is reported on its
 * own instead of being masked by the other one.
 *
 * <p>{@code require = 0} keeps a renamed target from crashing the game at startup; the module reports
 * hooks that never fired instead.
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
	@Inject(method = "tesselateWithAO", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayHideAO(
		BlockAndTintGetter level, List<BlockModelPart> parts, BlockState state, BlockPos pos,
		PoseStack poseStack, VertexConsumer builder, boolean cull, int overlayCoords, CallbackInfo ci
	) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("blocks");
			if (XrayModule.isBlocked(state, pos)) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "tesselateWithoutAO", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayHideFlat(
		BlockAndTintGetter level, List<BlockModelPart> parts, BlockState state, BlockPos pos,
		PoseStack poseStack, VertexConsumer builder, boolean cull, int overlayCoords, CallbackInfo ci
	) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("blocks");
			if (XrayModule.isBlocked(state, pos)) {
				ci.cancel();
			}
		}
	}

	/**
	 * A visible ore is usually surrounded by blocks that are no longer drawn, and vanilla culls the faces
	 * pointing at them — the ore would render as a hollow shell. Force all six faces instead.
	 */
	@Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
	private static void nhack$xrayFace(
		BlockAndTintGetter level, BlockState state, boolean cullEnabled, Direction direction, BlockPos neighborPos,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (XrayModule.isActive() && XrayModule.isOre(state)) {
			XrayModule.hookFired("faces");
			cir.setReturnValue(true);
		}
	}
}
