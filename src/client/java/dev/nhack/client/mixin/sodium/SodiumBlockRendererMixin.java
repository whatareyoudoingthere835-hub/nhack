package dev.nhack.client.mixin.sodium;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium replaces the whole vanilla chunk pipeline, so {@link dev.nhack.client.mixin.ModelBlockRendererMixin}
 * never runs for terrain while Sodium is loaded. This is the same decision one level down: Sodium asks its
 * {@code BlockRenderer} to mesh a block model, and blocked blocks simply produce nothing.
 *
 * <p>{@code remap = false} because Sodium is not obfuscated, {@link Pseudo} plus {@code require = 0} because
 * the whole config is skipped when Sodium isn't installed (or a future Sodium renames the method).
 * Only Minecraft types appear in the handler, so nhack needs no compile time dependency on Sodium —
 * Sodium ships against intermediary names and nhack is remapped to them at build time.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class SodiumBlockRendererMixin {
	@Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayHide(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("sodium-blocks");
			if (XrayModule.isBlocked(state, pos)) {
				ci.cancel();
			}
		}
	}
}
