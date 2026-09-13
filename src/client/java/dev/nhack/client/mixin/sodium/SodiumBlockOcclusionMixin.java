package dev.nhack.client.mixin.sodium;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium does its own face culling in {@code AbstractBlockRenderContext#shouldDrawSide}. Forced to
 * {@code true} while xray is on so a visible ore renders all six faces even where it touches a block
 * that is no longer drawn — the vanilla equivalent lives in {@code ModelBlockRendererMixin}.
 *
 * <p>No need to look at which block is being meshed: everything that isn't an exposed diamond ore is
 * cancelled earlier in {@code BlockRenderer#renderModel} and never reaches this check.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public class SodiumBlockOcclusionMixin {
	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayFace(Direction facing, CallbackInfoReturnable<Boolean> cir) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("sodium-faces");
			cir.setReturnValue(true);
		}
	}
}
