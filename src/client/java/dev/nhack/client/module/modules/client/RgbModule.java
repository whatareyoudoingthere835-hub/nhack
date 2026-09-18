package dev.nhack.client.module.modules.client;

import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.setting.Setting;
import dev.nhack.client.util.ColorUtil;

/**
 * «1.12.2?» — привет из старых клиентов: все интерфейсы чита (ClickGUI, HUD, главное меню,
 * префикс в чате) переливаются RGB слева направо.
 *
 * <p>Оттенок считается в {@link ColorUtil#rgb(float, float)} от экранной координаты X и времени,
 * поэтому отдельные окна и строки внутри одного окна окрашены по-разному, а вся картинка постоянно
 * едет вправо. Интерфейсы рисуют через {@link dev.nhack.client.util.RgbUtil}, который при
 * выключенном модуле ведёт себя ровно как обычный {@code fill}/{@code drawString}.
 */
public final class RgbModule extends Module {
	private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Циклов спектра в секунду", 0.8, 0.05, 3.0, 0.05));
	private final NumberSetting spread = addSetting(new NumberSetting("Spread", "Пикселей на полный цикл слева направо", 200.0, 40.0, 800.0, 10.0));
	private final NumberSetting saturation = addSetting(new NumberSetting("Saturation", "Насыщенность цвета", 0.9, 0.0, 1.0, 0.05));
	private final NumberSetting brightness = addSetting(new NumberSetting("Brightness", "Яркость цвета", 1.0, 0.2, 1.0, 0.05));
	private final NumberSetting tilt = addSetting(new NumberSetting("Tilt", "Наклон ленты, % от Y: 0 — строго слева направо", 0.0, 0.0, 100.0, 5.0));
	private final BoolSetting perCharText = addSetting(new BoolSetting("PerCharText", "Красить текст побуквенно, как в 1.12.2", true));
	private final BoolSetting chat = addSetting(new BoolSetting("Chat", "Переливающийся префикс в чате", true));

	public RgbModule() {
		super("1.12.2?", "RGB-переливание всех интерфейсов клиента слева направо", Category.CLIENT);
		// Любое движение ползунка сразу уходит в ColorUtil — превью видно вживую,
		// пока открыт ClickGUI.
		for (Setting<?> setting : getSettings()) {
			setting.onChanged(this::push);
		}
	}

	@Override
	protected void onEnable() {
		push();
	}

	@Override
	protected void onDisable() {
		push();
	}

	/**
	 * Состояние живёт в {@link ColorUtil}, а не в модуле: так любой интерфейс берёт оттенок
	 * одним статическим вызовом и не тянет зависимость от ModuleManager.
	 */
	private void push() {
		ColorUtil.configureRgb(
			isEnabled(),
			speed.getFloat(),
			spread.getFloat(),
			saturation.getFloat(),
			brightness.getFloat(),
			tilt.getFloat() / 100.0F
		);
		ColorUtil.configureRgbExtras(perCharText.get(), chat.get());
	}
}
