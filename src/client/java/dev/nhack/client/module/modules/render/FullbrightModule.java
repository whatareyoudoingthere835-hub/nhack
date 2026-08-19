package dev.nhack.client.module.modules.render;

import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.NumberSetting;

/**
 * Brightness is applied from {@code LightTextureMixin}.
 * The slider is kept as a setting example — hook it in the mixin if you want variable intensity.
 */
public final class FullbrightModule extends Module {
	public final NumberSetting intensity = addSetting(new NumberSetting(
		"Intensity",
		"Reserved for a custom brightness curve in LightTextureMixin",
		1.0,
		0.1,
		1.0,
		0.05
	));

	public FullbrightModule() {
		super("Fullbright", "Removes world darkness", Category.RENDER);
	}
}
