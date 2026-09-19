package dev.nhack.client.module.modules.render;

import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.NumberSetting;

/**
 * Brightness is applied to the 1.21.11 GPU lightmap from {@code LightTextureMixin}.
 */
public final class FullbrightModule extends Module {
	public final NumberSetting intensity = addSetting(new NumberSetting(
		"Intensity",
		"Интенсивность GPU-lightmap, 1.0 = полный свет",
		1.0,
		0.1,
		1.0,
		0.05
	));

	public FullbrightModule() {
		super("Fullbright", "Removes world darkness", Category.RENDER);
	}
}
