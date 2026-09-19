package dev.nhack.client.module.modules.render;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.PacketReceiveEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.ColorSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.setting.OreBlacklistSetting;
import dev.nhack.client.util.ChatUtil;
import dev.nhack.client.util.WorldToScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * «Иксрей» для алмазов: подсвечивает на 2D-экране только <b>открытую</b> алмазную руду
 * (ту, что уже видна из пещеры / воздуха / воды — замурованная в камне игнорируется).
 *
 * <p>Сканирование разбито на чанки: один тик — один чанк, поэтому включённый модуль
 * не просаживает TPS. Рендер идёт через {@link HudRenderEvent} (Fabric HUD-хук),
 * сканирование — через {@link TickEvent.Pre}.
 *
 * <p>Анти-xray engine mode 2 (Paper) подмешивает в данные чанка фейковую руду по всем открытым
 * поверхностям, а настоящую прячет камнем и раскрывает только block-update'ом, когда рядом добыли
 * блок. Фейки отсекаются двумя фильтрами: по количеству воздуха вокруг (фейки сидят в карманах
 * с 1-3 воздуха, руда в пещерах касается куда больше) и по рудам-соседям из блэклиста (обфускатор
 * сыпет микс руд вперемешку, настоящие жилы разных руд так близко не срастаются). Дополнительно
 * пакеты раскрытия ({@link PacketReceiveEvent}) ставят метку на руду, которую сервер сам показал
 * после добычи соседнего блока, и снимают метку, когда фейк «снимает маску» (руда→камень).
 */
public final class ExposedDiamonds extends Module {
	/** Алмазная руда в ванили генерируется от bedrock и до Y=16. */
	private static final int ORE_MAX_Y = 16;

	/** Столько «открытых» алмазов в одном чанке ваниль не даёт: это обфускатор анти-xray. */
	private static final int OBFUSCATION_PER_CHUNK = 12;

	private final NumberSetting radius = addSetting(new NumberSetting("Radius", "Радиус поиска в чанках, ближние сканируются первыми", 3.0, 1.0, 6.0, 1.0));
	private final NumberSetting maxY = addSetting(new NumberSetting("MaxY", "Верхняя граница поиска по Y", ORE_MAX_Y, -64.0, 320.0, 1.0));
	private final NumberSetting maxPerChunk = addSetting(new NumberSetting("MaxPerChunk", "Максимум меток на чанк, 0 = без лимита", 20.0, 0.0, 64.0, 1.0));
	private final NumberSetting airMin = addSetting(new NumberSetting("AirMin", "Минимум воздуха в кубе 3×3×3 вокруг руды: меньше — метка не ставится. Фейки анти-xray сидят в карманах с 1-3 воздуха, руда в пещерах касается ~9. 0 = выкл", 4.0, 0.0, 26.0, 1.0));
	private final NumberSetting oreRadius = addSetting(new NumberSetting("OreRadius", "Радиус проверки руд-соседей вокруг алмаза, блоков в каждую сторону", 4.0, 1.0, 8.0, 1.0));
	private final OreBlacklistSetting oreBlacklist = addSetting(new OreBlacklistSetting("OreBlacklist", "Руды-соседи, при которых метка не ставится: обфускатор сыпет микс руд рядом, настоящие жилы растут поодиночке. Клик — раскрыть список с галочками"));
	private final ColorSetting diamondColor = addSetting(new ColorSetting("DiamondColor", "Цвет алмаза", 0xFF00BBFF));

	/** Потокобезопасный список для рендера: скан и рендер идут в клиентском потоке, но список читают итератором. */
	private final List<BlockPos> foundDiamonds = new CopyOnWriteArrayList<>();
	/** Руда, которую сервер сам раскрыл block-update'ом: единственный честный сигнал при анти-xray mode 2. */
	private final Set<BlockPos> revealedOres = ConcurrentHashMap.newKeySet();
	private final List<int[]> chunkOffsets = new ArrayList<>();

	private int scanIndex;
	private int cachedRadius = Integer.MIN_VALUE;
	private int lastChunkX = Integer.MIN_VALUE;
	private int lastChunkZ = Integer.MIN_VALUE;
	private String scannedDimension = "";
	private boolean obfuscationNotified;

	public ExposedDiamonds() {
		super("CaveXRay", "Подсвечивает открытые алмазы на 2D экране", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		foundDiamonds.clear();
		revealedOres.clear();
		obfuscationNotified = false;
		scanIndex = 0;
		cachedRadius = Integer.MIN_VALUE;
		lastChunkX = Integer.MIN_VALUE;
		lastChunkZ = Integer.MIN_VALUE;
		scannedDimension = "";
	}

	@Override
	protected void onDisable() {
		foundDiamonds.clear();
		revealedOres.clear();
		obfuscationNotified = false;
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
			revealedOres.clear();
			obfuscationNotified = false;
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

		List<BlockPos> candidates = new ArrayList<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos adjPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = minY; y <= topY; y++) {
					pos.set(minX + x, y, minZ + z);
					BlockState state = chunk.getBlockState(pos);

					if (isDiamondOre(state)) {
						if (isExposed(level, pos, adjPos, probe)) {
							candidates.add(pos.immutable());
						}
					}
				}
			}
		}

		if (candidates.size() >= OBFUSCATION_PER_CHUNK && !obfuscationNotified) {
			// Ванильная генерация не даёт десятки открытых алмазов в чанке: это обфускатор.
			obfuscationNotified = true;
			ChatUtil.send(ChatFormatting.YELLOW, "Похоже на анти-xray engine mode 2: фильтрую фейки по воздуху вокруг и рудам-соседям, а раскрытую сервером руду ловлю по block-update'ам.");
		}

		List<BlockPos> newFound = new ArrayList<>();
		for (BlockPos candidate : candidates) {
			if (passesFilters(level, candidate, probe)) {
				newFound.add(candidate);
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

		// Чанк пересканирован — старые отметки из него заменяем свежими, но раскрытые сервером
		// блоки переживают перескан: они не из данных чанка, а из пакетов обновления.
		foundDiamonds.removeIf(p -> ((p.getX() >> 4) == chunkX && (p.getZ() >> 4) == chunkZ) && !revealedOres.contains(p));
		foundDiamonds.addAll(newFound);
	}

	@Subscribe
	public void onPacketReceive(PacketReceiveEvent event) {
		if (!isEnabled()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}

		Object packet = event.packet();
		if (packet instanceof ClientboundBlockUpdatePacket update) {
			handleBlockUpdate(mc, update.getPos(), update.getBlockState());
		} else if (packet instanceof ClientboundSectionBlocksUpdatePacket update) {
			BiConsumer<BlockPos, BlockState> consumer = (pos, state) -> handleBlockUpdate(mc, pos, state);
			update.runUpdates(consumer);
		}
	}

	/**
	 * Камень стал алмазом — сервер раскрыл настоящую руду (update-radius анти-xray после добычи
	 * соседнего блока): ставим метку. Алмаз стал камнем — это фейк обфускатора, снявший маску,
	 * или выкопанная руда (стала воздухом): метку убираем.
	 */
	private void handleBlockUpdate(Minecraft mc, BlockPos pos, BlockState newState) {
		if (pos == null || newState == null) {
			return;
		}

		boolean wasOre = isDiamondOre(mc.level.getBlockState(pos));
		boolean nowOre = isDiamondOre(newState);
		if (wasOre == nowOre) {
			return;
		}

		if (nowOre) {
			if (pos.getY() > maxY.getInt()) {
				return;
			}
			int dx = Math.abs((pos.getX() >> 4) - (mc.player.getBlockX() >> 4));
			int dz = Math.abs((pos.getZ() >> 4) - (mc.player.getBlockZ() >> 4));
			if (dx > radius.getInt() || dz > radius.getInt()) {
				return;
			}
			BlockPos immutable = pos.immutable();
			if (revealedOres.add(immutable)) {
				foundDiamonds.add(immutable);
			}
		} else {
			revealedOres.remove(pos);
			foundDiamonds.remove(pos);
		}
	}

	private static boolean isDiamondOre(BlockState state) {
		return state != null && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE));
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

	/**
	 * Фильтры фейков анти-xray. Воздух: в кубе 3×3×3 вокруг руды его должно быть не меньше AirMin —
	 * фейки обфускатора стоят в узких карманах (1-3 воздуха), руда в живой пещере касается гораздо
	 * большего. Руды-соседи: в кубе радиуса OreRadius не должно быть руды из блэклиста — обфускатор
	 * сеет микс руд вперемешку, а настоящие жилы разных руд вплотную почти не срастаются.
	 * Незагруженные соседи не читаются воздухом и не читаются рудой: пропускаются явно.
	 */
	private boolean passesFilters(Level level, BlockPos pos, BlockPos.MutableBlockPos probe) {
		int airMin = this.airMin.getInt();
		if (airMin > 0) {
			int air = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						probe.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
						if (!level.isInsideBuildHeight(probe.getY()) || !level.hasChunkAt(probe)) {
							continue;
						}
						BlockState state = level.getBlockState(probe);
						if (state != null && state.isAir()) {
							air++;
						}
					}
				}
			}
			if (air < airMin) {
				return false;
			}
		}

		if (!oreBlacklist.isEmpty()) {
			int r = oreRadius.getInt();
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						probe.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
						if (!level.isInsideBuildHeight(probe.getY()) || !level.hasChunkAt(probe)) {
							continue;
						}
						BlockState state = level.getBlockState(probe);
						if (state != null && oreBlacklist.isBlacklisted(state.getBlock())) {
							return false;
						}
					}
				}
			}
		}
		return true;
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
		foundDiamonds.removeIf(pos -> isOutOfRange(pos, playerChunkX, playerChunkZ, radius));
		revealedOres.removeIf(pos -> isOutOfRange(pos, playerChunkX, playerChunkZ, radius));
	}

	private static boolean isOutOfRange(BlockPos pos, int playerChunkX, int playerChunkZ, int radius) {
		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		return Math.abs(cx - playerChunkX) > radius || Math.abs(cz - playerChunkZ) > radius;
	}
}
