package dev.nhack.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Блэклист руд-«соседей» для CaveXRay: если алмаз стоит вплотную к руде из списка, метка не ставится.
 * Обфускатор анти-xray сыпет фейковые руды всех видов вперемешку, поэтому чужая руда рядом — признак
 * фейка; настоящие жилы разных руд так близко почти не срастаются.
 *
 * <p>В ClickGUI настройка рисуется кнопкой, по клику раскрывается список руд с чекбоксами —
 * крестик означает «руда в блэклисте». В конфиг сохраняется список id блоков.
 */
public final class OreBlacklistSetting extends Setting<List<String>> {
	/** Одна строка списка: id блока, имя для GUI и сам блок для сравнения состояний. */
	public record Entry(String id, String label, Block block) {
	}

	public static final List<Entry> ENTRIES = List.of(
		new Entry("coal_ore", "Уголь", Blocks.COAL_ORE),
		new Entry("deepslate_coal_ore", "Глубинный уголь", Blocks.DEEPSLATE_COAL_ORE),
		new Entry("iron_ore", "Железо", Blocks.IRON_ORE),
		new Entry("deepslate_iron_ore", "Глубинное железо", Blocks.DEEPSLATE_IRON_ORE),
		new Entry("copper_ore", "Медь", Blocks.COPPER_ORE),
		new Entry("deepslate_copper_ore", "Глубинная медь", Blocks.DEEPSLATE_COPPER_ORE),
		new Entry("gold_ore", "Золото", Blocks.GOLD_ORE),
		new Entry("deepslate_gold_ore", "Глубинное золото", Blocks.DEEPSLATE_GOLD_ORE)
	);

	/**
	 * По умолчанию блэклист пуст: на серверах с анти-xray фейковая руда стоит рядом с чем угодно,
	 * поэтому предзаполненный список съедал и настоящие алмазы тоже (5 из 5 на catlean.su).
	 * Крестики расставляются вручную под конкретный сервер.
	 */
	public OreBlacklistSetting(String name, String description) {
		super(name, description, List.of());
	}

	@Override
	protected List<String> sanitize(List<String> value) {
		if (value == null) {
			return getDefault();
		}
		List<String> clean = new ArrayList<>();
		for (Entry entry : ENTRIES) {
			if (value.contains(entry.id())) {
				clean.add(entry.id());
			}
		}
		return clean;
	}

	/** True, если блок состояния — одна из отмеченных крестиком руд. */
	public boolean isBlacklisted(Block block) {
		for (Entry entry : ENTRIES) {
			if (entry.block() == block && get().contains(entry.id())) {
				return true;
			}
		}
		return false;
	}

	public void toggle(String id) {
		List<String> next = new ArrayList<>(get());
		if (!next.remove(id)) {
			next.add(id);
		}
		set(next);
	}

	public int count() {
		return get().size();
	}

	public boolean isEmpty() {
		return get().isEmpty();
	}

	@Override
	public JsonElement toJson() {
		JsonArray array = new JsonArray();
		for (String id : get()) {
			array.add(new JsonPrimitive(id));
		}
		return array;
	}

	@Override
	public void fromJson(JsonElement element) {
		if (element == null || !element.isJsonArray()) {
			return;
		}
		JsonArray array = element.getAsJsonArray();
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < array.size(); i++) {
			ids.add(array.get(i).getAsString());
		}
		set(ids);
	}
}
