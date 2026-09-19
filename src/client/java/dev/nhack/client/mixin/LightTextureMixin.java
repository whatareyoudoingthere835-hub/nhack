package dev.nhack.client.mixin;

import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.render.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
	@Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
	private static void nhack$brightness(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
		float level = fullbrightLevel();
		if (level > 0.0F) {
			cir.setReturnValue(level);
		}
	}

	@Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("RETURN"), cancellable = true)
	private static void nhack$dimensionBrightness(DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
		float level = fullbrightLevel();
		if (level > 0.0F) {
			cir.setReturnValue(level);
		}
	}

	/**
	 * 1.21.11 fills the lightmap UBO from DimensionType.ambientLight(). Redirecting this call
	 * makes the actual world renderer bright; the old getBrightness hooks alone do not affect
	 * the GPU lightmap anymore.
	 */
	@Redirect(
		method = "updateLightTexture",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F")
	)
	private static float nhack$ambientLight(DimensionType dimensionType) {
		float level = fullbrightLevel();
		return level > 0.0F ? level : dimensionType.ambientLight();
	}

	private static float fullbrightLevel() {
		return ModuleManager.get(FullbrightModule.class)
			.filter(Module::isEnabled)
			.map(module -> module.intensity.getFloat())
			.orElse(0.0F);
	}
}
