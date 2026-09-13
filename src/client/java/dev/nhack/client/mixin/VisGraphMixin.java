package dev.nhack.client.mixin;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chunk occlusion graph: with xray on no block counts as opaque, so every section stays visible through
 * every other section (Meteor's {@code ChunkOcclusionEvent}). Sodium builds its own meshes but still fills
 * this vanilla graph, so the hook covers both renderers.
 */
@Mixin(VisGraph.class)
public class VisGraphMixin {
	@Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayOcclusion(BlockPos pos, CallbackInfo ci) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("occlusion");
			ci.cancel();
		}
	}
}
