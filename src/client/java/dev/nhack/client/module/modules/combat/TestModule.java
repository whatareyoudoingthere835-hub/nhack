package dev.nhack.client.module.modules.combat;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.PacketSendEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.FriendManager;
import dev.nhack.client.util.MathUtil;
import dev.nhack.client.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TestModule extends Module {
	public static boolean sendingAttack;

	private final ModeSetting rotationMode = addSetting(new ModeSetting("Rotation", "Тип наведения под античиты", "Sloth/Polar", "Sloth/Polar", "Fantime"));
	private final ModeSetting targets = addSetting(new ModeSetting("Targets", "Кого бьем", "Players", "Players", "All", "Mobs"));
	private final NumberSetting range = addSetting(new NumberSetting("Range", "Дистанция", 3.0, 2.5, 6.0, 0.1));
	private final NumberSetting fov = addSetting(new NumberSetting("FOV", "Угол обзора", 180.0, 10.0, 360.0, 1.0));

	private final BoolSetting onlyCrits = addSetting(new BoolSetting("OnlyCrits", "Бьет только в падении (криты)", true));
	private final BoolSetting smartSprint = addSetting(new BoolSetting("SmartSprint", "Сброс спринта для обхода Polar", true));
	private final BoolSetting silent = addSetting(new BoolSetting("Silent", "Вращение только на сервере", true));
	private final BoolSetting raytrace = addSetting(new BoolSetting("Raytrace", "Строгий чек хитбокса (Polar Safe)", true));

	private LivingEntity target;
	private float rotationYaw;
	private float rotationPitch;
	private float[] pitchHistory = new float[10];

	public TestModule() {
		super("Aura", "Универсальная киллаура с байпасом Sloth и Polar и говна в чайнике", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		target = null;
		if (mc.player != null) {
			rotationYaw = mc.player.getYRot();
			rotationPitch = mc.player.getXRot();
			RotationUtil.captureVisual(mc.player);
			for (int i = 0; i < pitchHistory.length; i++) {
				pitchHistory[i] = rotationPitch;
			}
		}
	}

	@Override
	protected void onDisable() {
		target = null;
		RotationUtil.clear();
	}

	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null) {
			RotationUtil.clear();
			return;
		}

		updateTarget(mc, player);
		if (target == null) {
			RotationUtil.clear();
			return;
		}

		aim(player);
	}

	@Subscribe
	public void onPostTick(TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null || target == null) {
			return;
		}

		if (canAttack(player)) {
			attack(mc, player);
		}
	}

	@Subscribe
	public void onPacketSend(PacketSendEvent event) {
		if (target == null || sendingAttack) {
			return;
		}
		if (event.packet() instanceof ServerboundInteractPacket) {
			// Блок лишних интерактов если не бьем
		}
	}

	private void updateTarget(Minecraft mc, LocalPlayer player) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		float r = range.getFloat();

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == player || !(entity instanceof LivingEntity living) || living.isDeadOrDying() || !entity.isAlive()) {
				continue;
			}
			if (!isValidTarget(entity)) {
				continue;
			}
			if (entity instanceof Player other && FriendManager.isFriend(other.getName().getString())) {
				continue;
			}

			double dist = player.distanceToSqr(entity);
			if (dist > r * r) continue;

			// FOV Check
			float[] rots = RotationUtil.angles(player.getEyePosition(), entity.getBoundingBox().getCenter());
			if (Math.abs(Mth.wrapDegrees(player.getYRot() - rots[0])) > fov.getFloat() / 2.0F) {
				continue;
			}

			if (dist < bestDist) {
				best = living;
				bestDist = dist;
			}
		}

		target = best;
	}

	private boolean isValidTarget(Entity entity) {
		return switch (targets.get()) {
			case "Players" -> entity instanceof Player;
			case "Mobs" -> entity instanceof Mob;
			case "All" -> entity instanceof Player || entity instanceof Mob;
			default -> true;
		};
	}

	private boolean canAttack(LocalPlayer player) {
		if (player.getAttackStrengthScale(0.5F) < 0.9F) {
			return false;
		}
		if (onlyCrits.get()) {
			if (player.onGround() || player.getDeltaMovement().y > 0 || player.onClimbable() || player.isInWater()) {
				return false;
			}
		}
		if (raytrace.get()) {
			return RotationUtil.checkRtx(player, target, rotationYaw, rotationPitch, range.getFloat(), range.getFloat(), false);
		}
		return true;
	}

	private void aim(LocalPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 targetPos = target.getBoundingBox().getCenter();

		float[] baseRots = RotationUtil.angles(eye, targetPos);
		float targetYaw = baseRots[0];
		float targetPitch = baseRots[1];

		// Sloth / Fantime Smoothing Math
		float time = player.tickCount;
		float smoothW = (float) ((Math.sin(time * 0.4) * 3.0) + (Math.sin((time * 0.95) + 1.4) * 2.0));
		float smoothH = (float) ((Math.cos(time * 0.5 + 0.7) * 0.5) + (Math.cos(time * 0.78 + 3.1) * 1.5));

		System.arraycopy(pitchHistory, 0, pitchHistory, 1, pitchHistory.length - 1);
		pitchHistory[0] = targetPitch;

		float finalYaw = interpolateAngle(rotationYaw, targetYaw + smoothW, 0.4F);
		float finalPitch = interpolateAngle(rotationPitch, pitchHistory[Math.min(9, player.tickCount % 5)] + (smoothH * 1.5F), 0.3F);

		if (rotationMode.is("Sloth/Polar")) {
			// Apply strict GCD for Polar
			float f = Minecraft.getInstance().options.sensitivity().get().floatValue() * 0.6F + 0.2F;
			float gcd = f * f * f * 1.2F;

			float diffYaw = finalYaw - rotationYaw;
			float diffPitch = finalPitch - rotationPitch;

			diffYaw -= diffYaw % gcd;
			diffPitch -= diffPitch % gcd;

			finalYaw = rotationYaw + diffYaw;
			finalPitch = rotationPitch + diffPitch;
		}

		rotationYaw = Mth.wrapDegrees(finalYaw);
		rotationPitch = Mth.clamp(finalPitch, -90.0F, 90.0F);

		RotationUtil.set(rotationYaw, rotationPitch, !silent.get());
		if (!silent.get()) {
			player.setYRot(rotationYaw);
			player.setXRot(rotationPitch);
		}
	}

	private void attack(Minecraft mc, LocalPlayer player) {
		boolean wasSprinting = player.isSprinting();

		if (smartSprint.get() && wasSprinting) {
			// Дроп спринта до удара (Movement heuristic bypass)
			player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
			player.setSprinting(false);
		}

		sendingAttack = true;
		try {
			mc.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		} finally {
			sendingAttack = false;
		}

		if (smartSprint.get() && wasSprinting) {
			// Возврат спринта после
			player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
			player.setSprinting(true);
		}
	}

	private float interpolateAngle(float current, float target, float percent) {
		float diff = Mth.wrapDegrees(target - current);
		return current + diff * percent;
	}
}