package dev.nhack.client.module.modules.skyegames;

import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.SubCategory;
import dev.nhack.client.util.ChatUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port of Meteor Client's {@code Xray} (render category) for nhack.
 *
 * <p>Two things are locked compared to the original:
 * <ul>
 *     <li>{@code exposed-only} is always on and has no setting — ore buried inside solid rock never shows,</li>
 *     <li>the whitelist is fixed to diamond ore + deepslate diamond ore, nothing else ever lights up.</li>
 * </ul>
 *
 * <p>Meteor fades the rest of the world out with a per-quad alpha. nhack instead skips the geometry of
 * every blocked block completely (Meteor's {@code alpha == 0} path), which keeps the module identical
 * with and without Sodium: partial alpha would have to be written into Sodium's own quad buffer, and
 * that needs a compile time dependency on Sodium. Skipping is also cheaper — no translucent layer
 * sorting for thousands of ghosted quads.
 *
 * <p>Vanilla render hooks: {@code ModelBlockRendererMixin} (block models + face culling),
 * {@code LiquidBlockRendererMixin} (water/lava), {@code VisGraphMixin} (chunk occlusion),
 * {@code BlockBehaviourMixin} (ambient occlusion shade) and {@code BlockEntityRenderDispatcherMixin}
 * (chests, furnaces, ...). With Sodium installed the vanilla chunk pipeline is replaced, so block models,
 * face culling and fluids are hooked again in {@code dev.nhack.client.mixin.sodium}; chunk occlusion and
 * block entities keep working through the vanilla hooks because Sodium still calls into them.
 *
 * <p>Every hook injects with {@code require = 0} and reports itself here, so a Minecraft or Sodium update
 * that renames a target degrades into a chat message instead of a crash on startup.
 */
public final class XrayModule extends Module {
	/** Locked whitelist — the only blocks that keep rendering. */
	public static final List<Block> ORES = List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);

	/** Always {@code true} on purpose: there is no setting to turn it off. */
	public static final boolean EXPOSED_ONLY = true;

	/** Hook ids that ran at least once since the module was enabled. */
	private static final Set<String> FIRED = ConcurrentHashMap.newKeySet();

	/** Chunk meshes are built on worker threads, so the scratch position has to be per thread. */
	private static final ThreadLocal<BlockPos.MutableBlockPos> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	/** Cached for the mixins, they run on chunk builder threads and can't afford a module lookup per block. */
	private static volatile XrayModule instance;

	/** Ticks until the "did my hooks apply?" check runs, {@code -1} when nothing is pending. */
	private int checkIn = -1;

	public XrayModule() {
		super("Xray", "Hides the world and only shows exposed diamond ore — exposed-only is locked on", Category.SKYGAMES, SubCategory.TESTING);
		instance = this;
	}

	public static XrayModule instance() {
		return instance;
	}

	public static boolean isActive() {
		XrayModule module = instance;
		return module != null && module.isEnabled();
	}

	/**
	 * Everything that is not an exposed diamond ore is "blocked" and its geometry is skipped.
	 * Always {@code false} while the module is off, so the hooks cost one volatile read.
	 */
	public static boolean isBlocked(BlockState state, BlockPos pos) {
		XrayModule module = instance;
		if (module == null || !module.isEnabled()) {
			return false;
		}

		return !(ORES.contains(state.getBlock()) && (!EXPOSED_ONLY || isExposed(pos)));
	}

	/** True for the two whitelisted ores, used to force their faces to render. */
	public static boolean isOre(BlockState state) {
		return ORES.contains(state.getBlock());
	}

	/** True when at least one of the six neighbours isn't a solid full cube (air, cave, water, ...). */
	public static boolean isExposed(BlockPos pos) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || pos == null) {
			return false;
		}

		BlockPos.MutableBlockPos mutable = EXPOSED_POS.get();
		for (Direction direction : Direction.values()) {
			if (!level.getBlockState(mutable.setWithOffset(pos, direction)).isSolidRender()) {
				return true;
			}
		}

		return false;
	}

	/** Called by every render hook the first thing it does, so a missing mixin can be reported. */
	public static void hookFired(String id) {
		FIRED.add(id);
	}

	@Override
	protected void onEnable() {
		FIRED.clear();
		checkIn = 60;
		reload();
	}

	@Override
	protected void onDisable() {
		checkIn = -1;
		reload();
	}

	@Override
	public void onTick(TickEvent.Post event) {
		if (checkIn < 0) {
			return;
		}

		// No world yet (main menu) — nothing can render, wait instead of reporting a false failure.
		if (Minecraft.getInstance().level == null) {
			checkIn = 60;
			return;
		}

		if (--checkIn > 0) {
			return;
		}
		checkIn = -1;

		List<String> missing = new ArrayList<>();
		for (String hook : expectedHooks()) {
			if (!FIRED.contains(hook)) {
				missing.add(hook);
			}
		}

		if (!missing.isEmpty()) {
			ChatUtil.error("Xray: render hooks didn't apply (" + String.join(", ", missing) + ") — send latest.log");
		}
	}

	/** Hooks that must fire for any block within a few seconds of enabling, for the renderer in use. */
	private static List<String> expectedHooks() {
		List<String> hooks = new ArrayList<>(List.of("occlusion", "shade"));
		if (FabricLoader.getInstance().isModLoaded("sodium")) {
			hooks.add("sodium-blocks");
		} else {
			hooks.add("blocks");
		}
		return hooks;
	}

	/** Rebuilds every loaded section so the change shows up immediately. */
	private static void reload() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.levelRenderer != null) {
			mc.levelRenderer.allChanged();
		}
	}
}
