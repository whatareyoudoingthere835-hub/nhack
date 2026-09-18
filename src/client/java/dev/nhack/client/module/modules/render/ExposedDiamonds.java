package dev.nhack.client.module.modules.render;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.ColorSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.WorldToScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * «Иксрей» для алмазов: подсвечивает на 2D-экране только <b>открытую</b> алмазную руду
 * (ту, что уже видна из пещеры / воздуха / воды — замурованная в камне игнорируется).
 *
 * <p>Сканирование разбито на чанки: один тик — один чанк, поэтому включённый модуль
 * не просаживает TPS. Рендер идёт через {@link HudRenderEvent} (Fabric HUD-хук),
 * сканирование — через {@link TickEvent.Pre}.
 */
public final class ExposedDiamonds extends Module {
	/** Алмазная руда в ванили генерируется от bedrock и до Y=16. */
	private static final int ORE_MAX_Y = 16;

	private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Радиус поиска в чанках, ближние сканируются первыми", 3.0, 1.0, 6.0, 1.0));
	private final NumberSetting maxY = addSetting(new NumberSetting("MaxY", "Верхняя граница поиска по Y", ORE_MAX_Y, -64.0, 320.0, 1.0));
	private final NumberSetting maxPerChunk = addSetting(new NumberSetting("MaxPerChunk", "Максимум меток на чанк, 0 = без лимита", 20.0, 0.0, 64.0, 1.0));
	private final ColorSetting diamondColor = addSetting(new ColorSetting("DiamondColor", "Цвет алмаза", 0xFF00BBFF));

	/** Потокобезопасный список для рендера: скан и рендер идут в клиентском потоке, но список читают итератором. */
	private final List<BlockPos> foundDiamonds = new CopyOnWriteArrayList<>();
	private final List<int[]> chunkOffsets = new ArrayList<>();

	private int scanIndex;
	private int cachedRadius = Integer.MIN_VALUE;
	private int lastChunkX = Integer.MIN_VALUE;
	private int lastChunkZ = Integer.MIN_VALUE;
	private String scannedDimension = "";

	public ExposedDiamonds() {
		super("CaveXRay", "Подсвечивает открытые алмазы на 2D экране", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		foundDiamonds.clear();
		scanIndex = 0;
		cachedRadius = Integer.MIN_VALUE;
		lastChunkX = Integer.MIN_VALUE;
		lastChunkZ = Integer.MIN_VALUE;
		scannedDimension = "";
	}

	@Override
	protected void onDisable() {
		foundDiamonds.clear();
		scanIndex = 0;
		lastChunkX = Integer.MIN_VALUE;
		lastChunkZ = Integer.MIN_VALUE;
	}

	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}

		String dimension = mc.level.dimension().identifier().toString();
		if (!dimension.equals(scannedDimension)) {
			// Портал/телепорт: отметки из прошлого измерения уже бессмысленны.
			scannedDimension = dimension;
			foundDiamonds.clear();
			scanIndex = 0;
			lastChunkX = Integer.MIN_VALUE;
			lastChunkZ = Integer.MIN_VALUE;
		}

		int r = radius.getInt();
		if (r != cachedRadius) {
			rebuildOffsets(r);
			cachedRadius = r;
			scanIndex = 0;
		}

		int playerChunkX = mc.player.getBlockX() >> 4;
		int playerChunkZ = mc.player.getBlockZ() >> 4;

		if (chunkOffsets.isEmpty()) {
			return;
		}

		if (playerChunkX != lastChunkX || playerChunkZ != lastChunkZ) {
			// Ушли в другой чанк: список оффсетов отсортирован по расстоянию, поэтому перезапуск
			// прохода снова ставит в начало чанки под ногами, а не те, что были впереди на старте.
			lastChunkX = playerChunkX;
			lastChunkZ = playerChunkZ;
			scanIndex = 0;
			pruneOutOfRange(playerChunkX, playerChunkZ, r);
		}

		if (scanIndex >= chunkOffsets.size()) {
			scanIndex = 0;
			// Цикл по чанкам завершён — выбрасываем то, что ушло за радиус.
			pruneOutOfRange(playerChunkX, playerChunkZ, r);
		}

		int[] offset = chunkOffsets.get(scanIndex);
		scanIndex++;
		scanChunk(mc, playerChunkX + offset[0], playerChunkZ + offset[1]);
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui || foundDiamonds.isEmpty()) {
			return;
		}

		GuiGraphics graphics = event.graphics();
		Font font = mc.font;
		int color = diamondColor.argb();
		int boxColor = color & 0x66FFFFFF | 0x44000000;

		for (BlockPos pos : foundDiamonds) {
			// Проецируем центр блока на экран; null — точка за спиной камеры.
			float[] screen = WorldToScreen.project(Vec3.atCenterOf(pos));
			if (screen == null) {
				continue;
			}

			int x = (int) screen[0];
			int y = (int) screen[1];

			double dist = Math.sqrt(mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
			String distText = String.format("%.0fm", dist);

			graphics.fill(x - 4, y - 4, x + 4, y + 4, boxColor);
			graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
			graphics.drawString(font, "DIAMOND", x - font.width("DIAMOND") / 2, y - 14, color);
			graphics.drawString(font, distText, x - font.width(distText) / 2, y + 6, 0xFFFFFFFF);
		}
	}

	private void scanChunk(Minecraft mc, int chunkX, int chunkZ) {
		Level level = mc.level;
		if (!level.hasChunk(chunkX, chunkZ)) {
			return;
		}

		LevelChunk chunk = level.getChunk(chunkX, chunkZ);
		if (chunk.isEmpty()) {
			return;
		}

		int minX = chunkX << 4;
		int minZ = chunkZ << 4;
		// Нижняя граница — дно мира (в Незере/Энде это 0), верхняя — настройка, но не выше потолка мира.
		int minY = level.getMinY();
		int topY = Math.min(level.getMaxY(), maxY.getInt());

		if (topY < minY) {
			return;
		}

		List<BlockPos> newFound = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos adjPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= topY; y++) {
					pos.set(minX + x, y, minZ + z);
					BlockState state = chunk.getBlockState(pos);

					if (state != null && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))) {
						if (isExposed(level, pos, adjPos, probe)) {
							newFound.add(pos.immutable());
						}
					}
				}
			}
		}

		int cap = maxPerChunk.getInt();
		if (cap > 0 && newFound.size() > cap) {
			// «Алмазное море» анархии: из чанка берём только ближайшие к игроку, иначе экран
			// тонет в тысячах меток, а рендер кладёт FPS.
			double px = mc.player.getX();
			double py = mc.player.getY();
			double pz = mc.player.getZ();
			newFound.sort(Comparator.comparingDouble(p -> {
				double dx = p.getX() + 0.5 - px;
				double dy = p.getY() + 0.5 - py;
				double dz = p.getZ() + 0.5 - pz;
				return dx * dx + dy * dy + dz * dz;
			}));
			newFound = new ArrayList<>(newFound.subList(0, cap));
		}

		// Чанк пересканирован — старые отметки из него заменяем свежими.
		foundDiamonds.removeIf(p -> (p.getX() >> 4) == chunkX && (p.getZ() >> 4) == chunkZ);
		foundDiamonds.addAll(newFound);
	}

	/**
	 * Руда считается открытой, только если рядом настоящая полость: воздушный сосед засчитывается,
	 * когда сам касается ещё хотя бы одного воздуха (пещера, штрек, разлом), а одиночный карман
	 * в один блок внутри камня — нет. Вода и лава считаются всегда: через них руда видна.
	 *
	 * <p>Соседи из незагруженных чанков читаются ванилью как воздух, поэтому пропускаются явно —
	 * иначе граница загрузки превращала бы всю руду на краю области в «открытую».
	 */
	private boolean isExposed(Level level, BlockPos pos, BlockPos.MutableBlockPos adjPos, BlockPos.MutableBlockPos probe) {
		for (Direction dir : Direction.values()) {
			adjPos.setWithOffset(pos, dir);
			if (!level.isInsideBuildHeight(adjPos.getY()) || !level.hasChunkAt(adjPos)) {
				continue;
			}

			BlockState adjState = level.getBlockState(adjPos);
			if (adjState == null) {
				continue;
			}
			if (adjState.isAir()) {
				if (airTouchesAir(level, adjPos, probe)) {
					return true;
				}
				continue;
			}
			if (!adjState.getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** У воздушной клетки должен быть ещё один воздушный сосед: карман 1×1 в камне — не пещера. */
	private boolean airTouchesAir(Level level, BlockPos airPos, BlockPos.MutableBlockPos probe) {
		for (Direction dir : Direction.values()) {
			probe.setWithOffset(airPos, dir);
			if (!level.isInsideBuildHeight(probe.getY()) || !level.hasChunkAt(probe)) {
				continue;
			}
			BlockState state = level.getBlockState(probe);
			if (state != null && state.isAir()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Список чанков вокруг игрока, отсортированный по расстоянию: центр первым, углы последними.
	 * При проходе по одному чанку за тик это значит, что алмазы под ногами подсвечиваются сразу,
	 * а периферия дорисовывается следом.
	 */
	private void rebuildOffsets(int radius) {
		chunkOffsets.clear();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				chunkOffsets.add(new int[]{x, z});
			}
		}
		// Сортировка устойчивая, поэтому на одинаковом удалении порядок остаётся предсказуемым.
		chunkOffsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
	}

	private void pruneOutOfRange(int playerChunkX, int playerChunkZ, int radius) {
		foundDiamonds.removeIf(pos -> {
			int cx = pos.getX() >> 4;
			int cz = pos.getZ() >> 4;
			return Math.abs(cx - playerChunkX) > radius || Math.abs(cz - playerChunkZ) > radius;
		});
	}
}
