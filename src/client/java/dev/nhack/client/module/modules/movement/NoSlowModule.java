package dev.nhack.client.module.modules.movement;

import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.phys.Vec3;

public final class NoSlowModule extends Module {
	private final ModeSetting mode = addSetting(new ModeSetting(
		"Mode",
		"Bypass flavour",
		"NCP",
		"NCP", "StrictNCP", "Matrix", "Grim", "MusteryGrief", "GrimNew", "Matrix2", "LFCraft", "Matrix3"
	));
	private final BoolSetting mainHand = addSetting(new BoolSetting("MainHand", "Also bypass main-hand use", true));
	private final BoolSetting food = addSetting(new BoolSetting("Food", "Ignore food slowdown", true));
	private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Ignore bow / crossbow / trident", true));
	private final BoolSetting shield = addSetting(new BoolSetting("Shield", "Ignore shield slowdown", true));
	private final BoolSetting soulSand = addSetting(new BoolSetting("SoulSand", "Ignore soul sand", true));
	private final BoolSetting honey = addSetting(new BoolSetting("Honey", "Ignore honey", true));
	private final BoolSetting slime = addSetting(new BoolSetting("Slime", "Ignore slime", true));
	private final BoolSetting ice = addSetting(new BoolSetting("Ice", "Ignore ice", true));
	private final BoolSetting sweetBerryBush = addSetting(new BoolSetting("SweetBerryBush", "Ignore berry bushes", true));
	private final BoolSetting sneak = addSetting(new BoolSetting("Sneak", "Ignore sneak slowdown", false));
	private final BoolSetting crawl = addSetting(new BoolSetting("Crawl", "Ignore crawl slowdown", false));

	private boolean returnSneak;

	public NoSlowModule() {
		super("NoSlow", "Removes item and block movement slowdown", Category.MOVEMENT);
	}

	public static boolean shouldCancelItemSlow() {
		return ModuleManager.get(NoSlowModule.class)
			.filter(Module::isEnabled)
			.map(NoSlowModule::canNoSlow)
			.orElse(false);
	}

	@Override
	public void onTick(dev.nhack.client.event.events.TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.connection == null) {
			return;
		}

		if (returnSneak) {
			mc.options.keyShift.setDown(false);
			player.setSprinting(true);
			returnSneak = false;
		}

		if (mode.is("Matrix3") && player.isUsingItem() && !player.isFallFlying()) {
			applyMatrix3(player);
		}

		if (!player.isUsingItem() || player.isPassenger() || player.isFallFlying()) {
			return;
		}

		int slot = player.getInventory().getSelectedSlot();
		switch (mode.get()) {
			case "StrictNCP" -> player.connection.send(new ServerboundSetCarriedItemPacket(slot));
			case "MusteryGrief" -> {
				if (player.onGround() && mc.options.keyJump.isDown()) {
					mc.options.keyShift.setDown(true);
					returnSneak = true;
				}
			}
			case "Grim" -> {
				if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
					spoofSlots(player, slot);
				} else if (mainHand.get()) {
					player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, player.getYRot(), player.getXRot()));
				}
			}
			case "GrimNew" -> {
				if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
					spoofSlots(player, slot);
				} else if (mainHand.get() && (player.getTicksUsingItem() <= 3 || player.tickCount % 2 == 0)) {
					player.connection.send(new ServerboundUseItemPacket(InteractionHand.OFF_HAND, 0, player.getYRot(), player.getXRot()));
				}
			}
			case "Matrix" -> {
				Vec3 vel = player.getDeltaMovement();
				if (player.onGround() && !mc.options.keyJump.isDown()) {
					player.setDeltaMovement(vel.x * 0.3, vel.y, vel.z * 0.3);
				} else if (player.fallDistance > 0.2F) {
					player.setDeltaMovement(vel.x * 0.95F, vel.y, vel.z * 0.95F);
				}
			}
			case "Matrix2" -> {
				if (player.onGround()) {
					Vec3 vel = player.getDeltaMovement();
					float mul = player.tickCount % 2 == 0 ? 0.5F : 0.95F;
					player.setDeltaMovement(vel.x * mul, vel.y, vel.z * mul);
				}
			}
			case "LFCraft" -> {
				if (player.getTicksUsingItem() <= 3) {
					player.connection.send(new ServerboundPlayerActionPacket(
						ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
						player.blockPosition().above(),
						Direction.NORTH
					));
				}
			}
			default -> {
			}
		}
	}

	public boolean canNoSlow() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !isEnabled()) {
			return false;
		}
		if (mode.is("Matrix3")) {
			return false;
		}
		if (!player.isUsingItem()) {
			return true;
		}

		var used = player.getUseItem();
		if (!food.get() && used.has(DataComponents.FOOD)) {
			return false;
		}
		if (!shield.get() && used.is(Items.SHIELD)) {
			return false;
		}
		if (!projectiles.get() && (used.is(Items.CROSSBOW) || used.is(Items.BOW) || used.is(Items.TRIDENT))) {
			return false;
		}
		if (mode.is("MusteryGrief") && player.onGround() && !mc.options.keyJump.isDown()) {
			return false;
		}
		if (!mainHand.get() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
			return false;
		}
		if ((mode.is("Grim") || mode.is("GrimNew"))
			&& player.getUsedItemHand() == InteractionHand.MAIN_HAND
			&& (player.getOffhandItem().has(DataComponents.FOOD) || player.getOffhandItem().is(Items.SHIELD))) {
			return false;
		}
		return true;
	}

	public boolean shouldCancelBlockSlow(Entity entity) {
		if (!isEnabled() || !(entity instanceof LocalPlayer player) || player.level() == null) {
			return false;
		}
		var state = player.level().getBlockState(player.blockPosition());
		if (soulSand.get() && (state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL))) {
			return true;
		}
		if (honey.get() && state.is(Blocks.HONEY_BLOCK)) {
			return true;
		}
		if (slime.get() && state.is(Blocks.SLIME_BLOCK)) {
			return true;
		}
		return ice.get() && (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE));
	}

	public boolean shouldCancelBerrySlow() {
		Minecraft mc = Minecraft.getInstance();
		if (!isEnabled() || !sweetBerryBush.get() || mc.player == null || mc.level == null) {
			return false;
		}
		return mc.level.getBlockState(mc.player.blockPosition()).getBlock() instanceof SweetBerryBushBlock;
	}

	public boolean shouldCancelSneakSlow() {
		if (!isEnabled()) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return false;
		}
		if (sneak.get() && mc.player.isShiftKeyDown()) {
			return true;
		}
		return crawl.get() && mc.player.getPose() == Pose.SWIMMING && !mc.player.isInWater();
	}

	private void applyMatrix3(LocalPlayer player) {
		Vec3 vel = player.getDeltaMovement();
		float mul = player.onGround() ? 1.15F : 1.05F;
		player.setDeltaMovement(vel.x * mul, vel.y, vel.z * mul);
	}

	private static void spoofSlots(LocalPlayer player, int slot) {
		player.connection.send(new ServerboundSetCarriedItemPacket(slot % 8 + 1));
		player.connection.send(new ServerboundSetCarriedItemPacket(slot % 7 + 2));
		player.connection.send(new ServerboundSetCarriedItemPacket(slot));
	}
}
