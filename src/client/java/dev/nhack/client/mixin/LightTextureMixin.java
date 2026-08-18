package dev.nhack.client.mixin;

import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.modules.render.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
	@Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
	private static void nhack$brightness(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
		if (fullbright()) {
			cir.setReturnValue(1.0F);
		}
	}

	@Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("RETURN"), cancellable = true)
	private static void nhack$dimensionBrightness(DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
		if (fullbright()) {
			cir.setReturnValue(1.0F);
		}
	}

	private static boolean fullbright() {
		return ModuleManager.get(FullbrightModule.class).map(Module::isEnabled).orElse(false);
	}
}
