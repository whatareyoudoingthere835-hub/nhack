package dev.nhack.client.module.modules.skyegames;

import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.SubCategory;
import dev.nhack.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

/**
 * Port of Meteor Client's {@code Xray} (render category) for nhack.
 *
 * <p>Two things are locked compared to the original:
 * <ul>
 *     <li>{@code exposed-only} is always on and has no setting — ores buried in solid rock stay hidden,</li>
 *     <li>the whitelist is fixed to diamond ore + deepslate diamond ore, nothing else ever lights up.</li>
 * </ul>
 *
 * <p>Rendering hooks live in {@code ModelBlockRendererMixin} (per-block alpha + face culling),
 * {@code ItemBlockRenderTypesMixin} (moves ghosted blocks into the translucent layer so alpha actually blends),
 * {@code VisGraphMixin} (disables chunk occlusion, so you can look through the world),
 * {@code BlockBehaviourMixin} (kills ambient occlusion shading) and
 * {@code BlockEntityRenderDispatcherMixin} (hides chests / furnaces / ... together with the blocks).
 */
public final class XrayModule extends Module {
	/** Locked whitelist — the only blocks that keep rendering normally. */
	public static final List<Block> ORES = List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);

	/** Always {@code true} on purpose: there is no setting to turn it off. */
	public static final boolean EXPOSED_ONLY = true;

	private static final ThreadLocal<BlockPos.MutableBlockPos> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

	/** Cached for the mixins, they run on chunk builder threads and can't afford a lookup per block. */
	private static volatile XrayModule instance;

	private final NumberSetting opacity = addSetting(new NumberSetting(
		"Opacity",
		"Alpha of every other block (0 = invisible, 255 = normal)",
		25,
		0,
		255,
		1
	));

	private boolean pendingReload;

	public XrayModule() {
		super("Xray", "Ghost world that only lights up exposed diamond ore — exposed-only is locked on", Category.SKYGAMES, SubCategory.TESTING);
		opacity.onChanged(() -> {
			if (isEnabled()) {
				pendingReload = true;
			}
		});
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
	 * Alpha this block should be drawn with.
	 *
	 * @param pos block position, {@code null} when the caller has none (layer selection) — then exposure is ignored
	 * @return {@code -1} when xray is off or the block is a visible ore, otherwise {@code 0..255}
	 */
	public static int alpha(BlockState state, BlockPos pos) {
		XrayModule module = instance;
		if (module == null || !module.isEnabled()) {
			return -1;
		}
		return module.isBlocked(state.getBlock(), pos) ? module.opacity.getInt() : -1;
	}

	/** Everything that isn't an exposed diamond ore is "blocked" and gets the ghost treatment. */
	public boolean isBlocked(Block block, BlockPos pos) {
		return !(ORES.contains(block) && (!EXPOSED_ONLY || pos == null || isExposed(pos)));
	}

	/**
	 * Forces the faces of visible ores to draw even when they touch a ghosted block.
	 * Ported from {@code Xray#modifyDrawSide}.
	 */
	public boolean modifyDrawSide(BlockState state, BlockGetter level, BlockPos pos, Direction facing, boolean returns) {
		if (!returns && !isBlocked(state.getBlock(), pos)) {
			BlockPos adjPos = pos.relative(facing);
			BlockState adjState = level.getBlockState(adjPos);
			return adjState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block()
				|| adjState.getBlock() != state.getBlock()
				|| !adjState.isSolidRender()
				|| isBlocked(adjState.getBlock(), adjPos);
		}

		return returns;
	}

	/** True when at least one of the six neighbours isn't a solid full cube (air, cave, water, ...). */
	public static boolean isExposed(BlockPos pos) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
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

	@Override
	protected void onEnable() {
		reload();
	}

	@Override
	protected void onDisable() {
		reload();
	}

	@Override
	public void onTick(TickEvent.Post event) {
		if (pendingReload) {
			pendingReload = false;
			reload();
		}
	}

	/** Rebuilds every loaded section — batched to one call per tick so slider drags stay smooth. */
	private void reload() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.levelRenderer != null) {
			mc.levelRenderer.allChanged();
		}
	}
}
