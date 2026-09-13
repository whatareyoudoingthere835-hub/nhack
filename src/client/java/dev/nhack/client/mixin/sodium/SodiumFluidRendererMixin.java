package dev.nhack.client.mixin.sodium;

import dev.nhack.client.module.modules.skyegames.XrayModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium meshes fluids itself, so {@link dev.nhack.client.mixin.LiquidBlockRendererMixin} doesn't reach
 * them. Everything the fluid renderer is asked to draw is dropped while xray is on, matching the vanilla
 * behaviour of hiding water and lava so they can't cover ore.
 *
 * <p>The handler takes only the callback info — Sodium's own types ({@code LevelSlice}, {@code Material},
 * ...) stay out of the signature, so no compile time dependency on Sodium is needed.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public class SodiumFluidRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
	private void nhack$xrayFluid(CallbackInfo ci) {
		if (XrayModule.isActive()) {
			XrayModule.hookFired("sodium-fluids");
			ci.cancel();
		}
	}
}
