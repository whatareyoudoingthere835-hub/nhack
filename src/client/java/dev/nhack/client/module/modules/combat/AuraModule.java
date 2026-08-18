package dev.nhack.client.module.modules.combat;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.PacketSendEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.ColorUtil;
import dev.nhack.client.util.FriendManager;
import dev.nhack.client.util.InventoryUtil;
import dev.nhack.client.util.MathUtil;
import dev.nhack.client.util.RotationUtil;
import dev.nhack.client.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AuraModule extends Module {
	public static Entity target;
	public static boolean sendingAttack;

	private final NumberSetting attackRange = addSetting(new NumberSetting("Range", "Attack reach", 3.1, 1.0, 6.0, 0.1));
	private final NumberSetting wallRange = addSetting(new NumberSetting("ThroughWallsRange", "Reach through walls", 3.1, 0.0, 6.0, 0.1));
	private final NumberSetting fov = addSetting(new NumberSetting("FOV", "Field of view limit", 180, 1, 180, 1));

	private final ModeSetting rotationMode = addSetting(new ModeSetting("RotationMode", "How aim is applied", "Track", "Interact", "Track", "None"));
	private final NumberSetting interactTicks = addSetting(new NumberSetting("InteractTicks", "Ticks to hold aim in Interact mode", 3, 1, 10, 1));
	private final NumberSetting rotationSpeed = addSetting(new NumberSetting("RotationSpeed", "Track mode step", 10, 1, 20, 0.5));
	private final BoolSetting clientLook = addSetting(new BoolSetting("ClientLook", "Also turn the camera toward the target", false));

	private final ModeSetting switchMode = addSetting(new ModeSetting("AutoWeapon", "Swap to a weapon before hitting", "None", "Normal", "None", "Silent"));
	private final BoolSetting onlyWeapon = addSetting(new BoolSetting("OnlyWeapon", "Do nothing unless a sword/axe/trident is available", false));

	private final BoolSetting smartCrit = addSetting(new BoolSetting("SmartCrit", "Wait for a crit window", true));
	private final BoolSetting onlySpace = addSetting(new BoolSetting("OnlyCrit", "Only hit while jumping", false));
	private final BoolSetting autoJump = addSetting(new BoolSetting("AutoJump", "Jump to crit", false));

	private final BoolSetting shieldBreaker = addSetting(new BoolSetting("ShieldBreaker", "Swap to an axe against shields", true));
	private final BoolSetting pauseWhileEating = addSetting(new BoolSetting("PauseWhileEating", "Pause while using an item", false));
	private final NumberSetting attackDelay = addSetting(new NumberSetting("AttackDelay", "Ticks between hits", 10, 1, 20, 1));
	private final ModeSetting rayTrace = addSetting(new ModeSetting("RayTrace", "Hitbox / wall check", "OnlyTarget", "OFF", "OnlyTarget", "AllEntities"));
	private final BoolSetting showEsp = addSetting(new BoolSetting("ESP", "Draw the current target on the HUD", true));

	private final ModeSetting sort = addSetting(new ModeSetting("Sort", "Target priority", "LowestDistance", "LowestDistance", "HighestDistance", "LowestHealth", "HighestHealth", "FOV"));
	private final BoolSetting lockTarget = addSetting(new BoolSetting("LockTarget", "Keep the current target until invalid", true));

	private final BoolSetting players = addSetting(new BoolSetting("Players", "Target players", true));
	private final BoolSetting mobs = addSetting(new BoolSetting("Mobs", "Target generic mobs", true));
	private final BoolSetting animals = addSetting(new BoolSetting("Animals", "Target animals", true));
	private final BoolSetting villagers = addSetting(new BoolSetting("Villagers", "Target villagers", true));
	private final BoolSetting slimes = addSetting(new BoolSetting("Slimes", "Target slimes", true));
	private final BoolSetting hostiles = addSetting(new BoolSetting("Hostiles", "Target monsters", true));
	private final BoolSetting onlyAngry = addSetting(new BoolSetting("OnlyAngryHostiles", "Only angry neutrals", true));
	private final BoolSetting projectiles = addSetting(new BoolSetting("Projectiles", "Target fireballs / shulker bullets", true));
	private final BoolSetting ignoreInvisible = addSetting(new BoolSetting("IgnoreInvisibleEntities", "Skip invisible entities", false));
	private final BoolSetting ignoreNamed = addSetting(new BoolSetting("IgnoreNamed", "Skip named entities", false));
	private final BoolSetting ignoreTeam = addSetting(new BoolSetting("IgnoreTeam", "Skip same-team players", false));
	private final BoolSetting ignoreCreative = addSetting(new BoolSetting("IgnoreCreative", "Skip creative players", true));
	private final BoolSetting ignoreNaked = addSetting(new BoolSetting("IgnoreNaked", "Skip players with no armor", false));
	private final BoolSetting ignoreShield = addSetting(new BoolSetting("AttackShieldingEntities", "Hit players who are blocking", true));

	public float rotationYaw;
	public float rotationPitch;

	private Vec3 rotationPoint = Vec3.ZERO;
	private Vec3 rotationMotion = Vec3.ZERO;

	private int hitTicks;
	private int trackTicks;
	private boolean lookingAtHitbox;
	private boolean readyForAttack;

	private final Timer pauseTimer = new Timer();

	public AuraModule() {
		super("Aura", "Attacks entities in range with rotations, crits and weapon swap", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		target = null;
		lookingAtHitbox = false;
		readyForAttack = false;
		rotationPoint = Vec3.ZERO;
		rotationMotion = Vec3.ZERO;
		if (mc.player != null) {
			rotationYaw = mc.player.getYRot();
			rotationPitch = mc.player.getXRot();
			RotationUtil.captureVisual(mc.player);
		}
	}

	@Override
	protected void onDisable() {
		target = null;
		readyForAttack = false;
		RotationUtil.clear();
	}

	public void pause() {
		pauseTimer.reset();
	}

	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null) {
			RotationUtil.clear();
			return;
		}
		if (!pauseTimer.passedMs(1000)) {
			RotationUtil.clear();
			return;
		}
		if (player.isUsingItem() && pauseWhileEating.get()) {
			RotationUtil.clear();
			return;
		}

		RotationUtil.captureVisual(player);
		readyForAttack = false;

		if (!haveWeapon(player)) {
			target = null;
			RotationUtil.clear();
			hitTicks = Math.max(0, hitTicks - 1);
			return;
		}

		updateTarget(mc, player);

		if (target == null) {
			RotationUtil.clear();
			rotationYaw = player.getYRot();
			rotationPitch = player.getXRot();
			hitTicks = Math.max(0, hitTicks - 1);
			return;
		}

		if (!mc.options.keyJump.isDown() && player.onGround() && autoJump.get()) {
			player.jumpFromGround();
		}

		boolean critReady = autoCrit(mc, player);
		calcRotations(player, critReady);
		readyForAttack = critReady && (lookingAtHitbox || skipRayTraceCheck(mc, player));
		hitTicks = Math.max(0, hitTicks - 1);
	}

	@Subscribe
	public void onPostTick(TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null || target == null || !readyForAttack) {
			return;
		}
		if (!pauseTimer.passedMs(1000) || (player.isUsingItem() && pauseWhileEating.get()) || !haveWeapon(player)) {
			return;
		}

		if (shieldBreaker(mc, player, false)) {
			return;
		}

		if (target instanceof Player victim
			&& victim.isUsingItem()
			&& (victim.getOffhandItem().is(Items.SHIELD) || victim.getMainHandItem().is(Items.SHIELD))
			&& !ignoreShield.get()) {
			return;
		}

		attack(mc, player);
	}

	@Subscribe
	public void onPacketSend(PacketSendEvent event) {
		if (target == null || sendingAttack) {
			return;
		}
		if (event.packet() instanceof ServerboundInteractPacket) {
			event.cancel();
		}
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		if (!showEsp.get() || target == null || !haveWeapon(Minecraft.getInstance().player)) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		GuiGraphics graphics = event.graphics();
		Font font = mc.font;
		String hp = target instanceof LivingEntity living
			? String.format("%.1f", living.getHealth() + living.getAbsorptionAmount())
			: "-";
		String line = "Aura  " + target.getName().getString() + "  " + hp + "hp  "
			+ String.format("%.1f", mc.player == null ? 0 : mc.player.distanceTo(target)) + "m";
		int width = font.width(line);
		int x = graphics.guiWidth() / 2 - width / 2;
		int y = graphics.guiHeight() / 2 + 18;
		graphics.fill(x - 4, y - 3, x + width + 4, y + 11, 0x88000000);
		graphics.fill(x - 4, y - 3, x - 2, y + 11, ColorUtil.ACCENT);
		graphics.drawString(font, line, x, y, ColorUtil.TEXT);
	}

	private void attack(Minecraft mc, LocalPlayer player) {
		int previous = switchMethod(mc, player);
		sendingAttack = true;
		try {
			mc.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		} finally {
			sendingAttack = false;
		}
		hitTicks = attackDelay.getInt();
		if (previous != -1) {
			player.getInventory().setSelectedSlot(previous);
			player.connection.send(new ServerboundSetCarriedItemPacket(previous));
		}
	}

	private int switchMethod(Minecraft mc, LocalPlayer player) {
		if (switchMode.is("None")) {
			return -1;
		}
		int sword = InventoryUtil.findSwordHotbar(player);
		if (sword == -1) {
			sword = InventoryUtil.findAxeHotbar(player);
		}
		if (sword == -1) {
			return -1;
		}

		int previous = player.getInventory().getSelectedSlot();
		if (previous == sword) {
			return -1;
		}

		player.getInventory().setSelectedSlot(sword);
		player.connection.send(new ServerboundSetCarriedItemPacket(sword));
		return switchMode.is("Silent") ? previous : -1;
	}

	private boolean haveWeapon(LocalPlayer player) {
		if (player == null || !onlyWeapon.get()) {
			return true;
		}
		if (switchMode.is("None")) {
			return InventoryUtil.isWeapon(player.getMainHandItem());
		}
		return InventoryUtil.findSwordHotbar(player) != -1 || InventoryUtil.findAxeHotbar(player) != -1;
	}

	private boolean skipRayTraceCheck(Minecraft mc, LocalPlayer player) {
		if (rotationMode.is("None") || rayTrace.is("OFF")) {
			return true;
		}
		if (rotationMode.is("Interact") && (interactTicks.getInt() <= 1 || headBlocked(mc, player))) {
			return true;
		}
		return false;
	}

	private boolean headBlocked(Minecraft mc, LocalPlayer player) {
		return mc.level.getBlockCollisions(
			player,
			player.getBoundingBox().inflate(-0.25, 0.0, -0.25).move(0.0, 1.0, 0.0)
		).iterator().hasNext();
	}

	private boolean autoCrit(Minecraft mc, LocalPlayer player) {
		if (!smartCrit.get()) {
			return true;
		}
		if (hitTicks > 0) {
			return false;
		}
		if (player.getAbilities().flying || player.isFallFlying()) {
			return true;
		}
		if (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.SLOW_FALLING)) {
			return true;
		}
		if (!mc.options.keyJump.isDown() && !onlySpace.get() && !autoJump.get()) {
			return true;
		}
		if (player.isInLava() || player.isUnderWater()) {
			return true;
		}
		if (!mc.options.keyJump.isDown() && isAboveWater(mc, player)) {
			return true;
		}
		return !player.onGround() && player.fallDistance > 0.1F;
	}

	private boolean shieldBreaker(Minecraft mc, LocalPlayer player, boolean instant) {
		int axeSlot = InventoryUtil.findAxe(player);
		if (axeSlot == -1 || !shieldBreaker.get() || !(target instanceof Player victim)) {
			return false;
		}
		if (!instant && !victim.isUsingItem()) {
			return false;
		}
		if (!victim.getOffhandItem().is(Items.SHIELD) && !victim.getMainHandItem().is(Items.SHIELD)) {
			return false;
		}

		int selected = player.getInventory().getSelectedSlot();
		if (axeSlot >= 9) {
			int menuSlot = InventoryUtil.toMenuSlot(axeSlot);
			mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, menuSlot, selected, ClickType.SWAP, player);
			sendingAttack = true;
			try {
				mc.gameMode.attack(player, target);
				player.swing(InteractionHand.MAIN_HAND);
			} finally {
				sendingAttack = false;
			}
			mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, menuSlot, selected, ClickType.SWAP, player);
		} else {
			player.connection.send(new ServerboundSetCarriedItemPacket(axeSlot));
			sendingAttack = true;
			try {
				mc.gameMode.attack(player, target);
				player.swing(InteractionHand.MAIN_HAND);
			} finally {
				sendingAttack = false;
			}
			player.connection.send(new ServerboundSetCarriedItemPacket(selected));
		}
		hitTicks = 10;
		return true;
	}

	private boolean isAboveWater(Minecraft mc, LocalPlayer player) {
		if (player.isUnderWater()) {
			return true;
		}
		BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.4, player.getZ());
		return mc.level.getBlockState(below).is(Blocks.WATER);
	}

	private void updateTarget(Minecraft mc, LocalPlayer player) {
		Entity candidate = findTarget(mc, player);
		if (target == null) {
			target = candidate;
			return;
		}
		if (sort.is("FOV") || !lockTarget.get()) {
			target = candidate;
		}
		if (candidate instanceof Projectile) {
			target = candidate;
		}
		if (skipEntity(mc, player, target)) {
			target = null;
		}
	}

	private void calcRotations(LocalPlayer player, boolean ready) {
		Minecraft mc = Minecraft.getInstance();
		if (ready) {
			trackTicks = headBlocked(mc, player) ? 1 : interactTicks.getInt();
		} else if (trackTicks > 0) {
			trackTicks--;
		}

		if (target == null) {
			return;
		}

		Vec3 aim = player.isFallFlying() ? target.getEyePosition() : getLegitLook(target);
		if (aim == null) {
			return;
		}

		float[] dest = RotationUtil.angles(player.getEyePosition(), aim);
		float deltaYaw = Mth.wrapDegrees(dest[0] - rotationYaw);
		float deltaPitch = dest[1] - rotationPitch;

		float yawStep;
		float pitchStep;
		if (!rotationMode.is("Track")) {
			yawStep = 360.0F;
			pitchStep = 180.0F;
		} else {
			float progress = Mth.clamp(Math.abs(deltaYaw) / 180.0F, 0.0F, 1.0F);
			float speed = rotationSpeed.getFloat();
			yawStep = Mth.lerp(progress, speed * 4.0F, speed * 8.0F);
			pitchStep = speed;
		}

		float stepYaw = Mth.clamp(deltaYaw, -yawStep, yawStep);
		float stepPitch = Mth.clamp(deltaPitch, -pitchStep, pitchStep);
		float newYaw = rotationYaw + stepYaw;
		float newPitch = Mth.clamp(rotationPitch + stepPitch, -90.0F, 90.0F);

		if (trackTicks > 0 || rotationMode.is("Track")) {
			rotationYaw = newYaw;
			rotationPitch = newPitch;
		} else {
			rotationYaw = player.getYRot();
			rotationPitch = player.getXRot();
		}

		if (!rotationMode.is("None")) {
			RotationUtil.set(rotationYaw, rotationPitch, clientLook.get());
			if (clientLook.get()) {
				player.setYRot(rotationYaw);
				player.setXRot(rotationPitch);
			}
		} else {
			RotationUtil.clear();
		}

		lookingAtHitbox = RotationUtil.checkRtx(
			player,
			target,
			rotationYaw,
			rotationPitch,
			attackRange.getFloat(),
			wallRange.getFloat(),
			!rayTrace.is("OFF")
		);
	}

	private Vec3 getLegitLook(Entity entity) {
		double lengthX = entity.getBoundingBox().getXsize();
		double lengthY = entity.getBoundingBox().getYsize();
		double lengthZ = entity.getBoundingBox().getZsize();

		if (rotationMotion.equals(Vec3.ZERO)) {
			rotationMotion = new Vec3(MathUtil.random(-0.01F, 0.01F), MathUtil.random(-0.01F, 0.01F), MathUtil.random(-0.01F, 0.01F));
		}

		rotationPoint = rotationPoint.add(rotationMotion);

		if (rotationPoint.x >= (lengthX - 0.05) / 2.0) {
			rotationMotion = new Vec3(-MathUtil.random(0.001F, 0.008F), rotationMotion.y, rotationMotion.z);
		}
		if (rotationPoint.y >= lengthY) {
			rotationMotion = new Vec3(rotationMotion.x, -MathUtil.random(0.0005F, 0.008F), rotationMotion.z);
		}
		if (rotationPoint.z >= (lengthZ - 0.05) / 2.0) {
			rotationMotion = new Vec3(rotationMotion.x, rotationMotion.y, -MathUtil.random(0.001F, 0.008F));
		}
		if (rotationPoint.x <= -(lengthX - 0.05) / 2.0) {
			rotationMotion = new Vec3(MathUtil.random(0.001F, 0.008F), rotationMotion.y, rotationMotion.z);
		}
		if (rotationPoint.y <= 0.05) {
			rotationMotion = new Vec3(rotationMotion.x, MathUtil.random(0.0005F, 0.008F), rotationMotion.z);
		}
		if (rotationPoint.z <= -(lengthZ - 0.05) / 2.0) {
			rotationMotion = new Vec3(rotationMotion.x, rotationMotion.y, MathUtil.random(0.001F, 0.008F));
		}

		return entity.position().add(rotationPoint);
	}

	private boolean isInRange(LocalPlayer player, Entity entity) {
		float range = getRotateDistance();
		if (RotationUtil.squaredDistanceFromEyes(player, entity.getEyePosition()) > (range + 2.0F) * (range + 2.0F)) {
			return false;
		}
		return RotationUtil.canSeePoint(player, entity, range, wallRange.getFloat());
	}

	private float getRotateDistance() {
		if (!rotationMode.is("Track") || rayTrace.is("OFF")) {
			return attackRange.getFloat();
		}
		return attackRange.getFloat() + 1.0F;
	}

	private Entity findTarget(Minecraft mc, LocalPlayer player) {
		List<LivingEntity> living = new ArrayList<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if ((entity instanceof ShulkerBullet || entity instanceof Fireball)
				&& entity.isAlive()
				&& isInRange(player, entity)
				&& projectiles.get()) {
				return entity;
			}
			if (skipEntity(mc, player, entity)) {
				continue;
			}
			if (entity instanceof LivingEntity livingEntity) {
				living.add(livingEntity);
			}
		}

		return switch (sort.get()) {
			case "HighestDistance" -> living.stream().max(Comparator.comparingDouble(entity -> player.distanceToSqr(entity.position()))).orElse(null);
			case "LowestHealth" -> living.stream().min(Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount())).orElse(null);
			case "HighestHealth" -> living.stream().max(Comparator.comparingDouble(entity -> entity.getHealth() + entity.getAbsorptionAmount())).orElse(null);
			case "FOV" -> living.stream().min(Comparator.comparingDouble(entity -> fovAngle(player, entity))).orElse(null);
			default -> living.stream().min(Comparator.comparingDouble(entity -> player.distanceToSqr(entity.position()))).orElse(null);
		};
	}

	private boolean skipEntity(Minecraft mc, LocalPlayer player, Entity entity) {
		if (!(entity instanceof LivingEntity living) || living.isDeadOrDying() || !entity.isAlive()) {
			return true;
		}
		if (entity instanceof ArmorStand || entity instanceof Cat) {
			return true;
		}
		if (skipNotSelected(player, entity)) {
			return true;
		}
		if (!RotationUtil.inFov(player, entity.position(), fov.getFloat())) {
			return true;
		}
		if (entity instanceof Player other) {
			if (other == player || FriendManager.isFriend(other.getName().getString())) {
				return true;
			}
			if (other.isCreative() && ignoreCreative.get()) {
				return true;
			}
			if (other.getArmorValue() == 0 && ignoreNaked.get()) {
				return true;
			}
			if (other.isInvisible() && ignoreInvisible.get()) {
				return true;
			}
			if (ignoreTeam.get() && other.getTeamColor() == player.getTeamColor() && player.getTeamColor() != 0xFFFFFF) {
				return true;
			}
		}
		if (entity.hasCustomName() && ignoreNamed.get()) {
			return true;
		}
		return !isInRange(player, entity);
	}

	private boolean skipNotSelected(LocalPlayer player, Entity entity) {
		if (entity instanceof Slime && !slimes.get()) {
			return true;
		}
		if (entity instanceof Monster) {
			if (!hostiles.get()) {
				return true;
			}
			if (onlyAngry.get() && entity instanceof NeutralMob neutral && !neutral.isAngry()) {
				return true;
			}
		}
		if (entity instanceof Player && !players.get()) {
			return true;
		}
		if (entity instanceof Villager && !villagers.get()) {
			return true;
		}
		if (entity instanceof Mob && !mobs.get()) {
			return true;
		}
		return entity instanceof Animal && !animals.get();
	}

	private float fovAngle(LocalPlayer player, LivingEntity entity) {
		double dx = entity.getX() - player.getX();
		double dz = entity.getZ() - player.getZ();
		float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		return Math.abs(Mth.wrapDegrees(yaw - player.getYRot()));
	}
}
