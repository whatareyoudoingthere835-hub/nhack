package dev.nhack.client.module.modules.combat;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.RenderEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.CombatUtil;
import dev.nhack.client.util.FriendManager;
import dev.nhack.client.util.MathUtil;
import dev.nhack.client.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Киллаура под Sloth / Polar / Fantime.
 *
 * <p>Три вещи, из-за которых старая версия ловилась за пару секунд:
 * <ol>
 * <li>Удар отправлялся в {@code TickEvent.Post}, то есть <b>после</b> пакета движения своего тика.
 *     Ваниль шлёт боевые пакеты из {@code handleKeybinds()} — раньше {@code sendPosition()}.
 *     Любой боевой пакет после движения — это сигнатура Post-проверки.</li>
 * <li>Наводка считалась один раз в тик (20 Гц) и ещё размазывалась синусоидой
 *     {@code sin(tickCount * 0.4) * 3.0} вокруг цели: прицел никогда не вставал на хитбокс,
 *     raytrace то проходил, то нет, а визуально всё это дёргалось. Теперь наводка покадровая
 *     ({@link RenderEvent}) и идёт к настоящей точке на хитбоксе.</li>
 * <li>Решение об ударе принималось по своему же повороту, который сервер ещё не получил.
 *     Теперь — по {@link RotationUtil#sentYaw}, то есть по тому, что реально ушло прошлым тиком.</li>
 * </ol>
 *
 * <p>GCD для Polar никуда не делся, но считается один раз и правильно — в
 * {@link RotationUtil#quantize}: дельта приводится к {@code (float)(counts * sens) * 0.15F},
 * ровно как в {@code MouseHandler.turnPlayer} + {@code Entity.turn}.
 */
public final class KillAuraModule extends Module {
	private final ModeSetting rotationMode = addSetting(new ModeSetting("Rotation", "Тип наведения под античиты", "Sloth/Polar", "Sloth/Polar", "Fantime"));
	private final ModeSetting targets = addSetting(new ModeSetting("Targets", "Кого бьем", "Players", "Players", "All", "Mobs"));
	private final NumberSetting range = addSetting(new NumberSetting("Range", "Дистанция", 3.0, 2.5, 6.0, 0.1));
	private final NumberSetting fov = addSetting(new NumberSetting("FOV", "Угол обзора", 180.0, 10.0, 360.0, 1.0));
	private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Скорость наводки, градусов за тик", 18.0, 1.0, 60.0, 0.5));
	private final NumberSetting jitter = addSetting(new NumberSetting("Jitter", "Случайный разброс скорости наводки, %", 12.0, 0.0, 50.0, 1.0));

	private final BoolSetting onlyCrits = addSetting(new BoolSetting("OnlyCrits", "Бьет только в падении (криты)", true));
	private final BoolSetting smartSprint = addSetting(new BoolSetting("SmartSprint", "Сброс спринта для обхода Polar", true));
	private final BoolSetting silent = addSetting(new BoolSetting("Silent", "Вращение только на сервере", true));
	private final BoolSetting raytrace = addSetting(new BoolSetting("Raytrace", "Строгий чек хитбокса (Polar Safe)", true));

	private LivingEntity target;
	private Vec3 aimOffset = Vec3.ZERO;
	private int aimRefresh;
	private boolean aiming;
	private boolean sprintReset;

	public float rotationYaw;
	public float rotationPitch;

	public KillAuraModule() {
		super("KillAura", "Универсальная киллаура с байпасом Sloth и Polar и говна в чайнике", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		// Две ауры одновременно писать в RotationUtil не могут: наводка превращается в кашу.
		ModuleManager.get(AuraModule.class).ifPresent(other -> {
			if (other.isEnabled()) {
				other.setEnabled(false, false);
			}
		});
		target = null;
		aiming = false;
		sprintReset = false;
		aimRefresh = 0;
		aimOffset = Vec3.ZERO;
		if (mc.player != null) {
			rotationYaw = mc.player.getYRot();
			rotationPitch = mc.player.getXRot();
			RotationUtil.captureVisual(mc.player);
		}
	}

	@Override
	protected void onDisable() {
		target = null;
		aiming = false;
		sprintReset = false;
		CombatUtil.clearSprintDrop();
		RotationUtil.clear();
	}

	/**
	 * Цель, наводка на тик и удар — всё до {@code Minecraft.tick()}, то есть до пакета движения.
	 */
	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		aiming = false;

		if (player == null || mc.level == null || mc.gameMode == null || mc.screen != null || mc.isPaused()) {
			target = null;
			RotationUtil.clear();
			return;
		}

		// Обязательно каждый тик: при Silent миксин sendPosition возвращает камере именно это
		// значение. Без свежего captureVisual камера откатывалась бы к повороту, который был
		// в момент включения модуля (старая версия ловила его только в onEnable).
		RotationUtil.captureVisual(player);

		LivingEntity previous = target;
		updateTarget(mc, player);
		if (target == null) {
			RotationUtil.clear();
			return;
		}

		if (previous != target) {
			// новая цель — точка прицела и счётчик сброса спринта с нуля
			aimOffset = Vec3.ZERO;
			aimRefresh = 0;
			sprintReset = false;
		}

		aiming = true;
		if (--aimRefresh <= 0) {
			refreshAimPoint(target);
			aimRefresh = 8 + (int) MathUtil.random(0.0F, 6.0F);
		}

		// Сервер валидирует удар тем поворотом, который уже получил. Если наводка ещё не ушла —
		// брать её текущее значение бессмысленно, удар всё равно проверят старым.
		float yaw = RotationUtil.active ? RotationUtil.sentYaw : player.getYRot();
		float pitch = RotationUtil.active ? RotationUtil.sentPitch : player.getXRot();
		if (!canAttack(player, yaw, pitch)) {
			return;
		}

		// Сброс спринта: флаг снимется прямо перед sendPosition, и ваниль сама отправит
		// STOP_SPRINTING в правильном месте очереди. Бьём следующим тиком — когда сервер
		// уже знает, что мы не в спринте.
		if (smartSprint.get() && !sprintReset && player.isSprinting()) {
			CombatUtil.requestSprintDrop();
			sprintReset = true;
			return;
		}

		attack(mc, player);
		sprintReset = false;
	}

	/**
	 * Наводка на кадровой частоте: 60–240 обновлений в секунду вместо 20 тиковых — именно это
	 * убирает рывки. Скорость в градусах за тик, умножается на {@link RenderEvent#deltaTime()},
	 * поэтому не зависит от FPS; каждый шаг приводится к сетке мыши внутри {@link RotationUtil}.
	 */
	@Subscribe
	public void onRender(RenderEvent event) {
		if (!aiming) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		LivingEntity entity = target;
		if (player == null || mc.level == null || entity == null || !entity.isAlive()
			|| mc.screen != null || mc.isPaused()) {
			return;
		}

		float partial = Mth.clamp(event.partialTick(), 0.0F, 1.0F);
		Vec3 aim = RotationUtil.lerp(entity, partial).add(aimOffset);
		float[] dest = RotationUtil.angles(player.getEyePosition(partial), aim);

		// Fantime не проверяет GCD, так что там можно доводить быстрее; сетка мыши всё равно
		// соблюдается — она ничего не стоит и не мешает.
		float aimSpeed = speed.getFloat() * (rotationMode.is("Fantime") ? 2.0F : 1.0F);
		float variance = jitter.getFloat() / 100.0F;
		if (variance > 0.0F) {
			aimSpeed *= 1.0F + MathUtil.random(-variance, variance);
		}

		RotationUtil.smooth(dest[0], dest[1], aimSpeed, event.deltaTime(), !silent.get());
		rotationYaw = RotationUtil.yaw;
		rotationPitch = RotationUtil.pitch;

		if (!silent.get()) {
			player.setYRot(rotationYaw);
			player.setXRot(rotationPitch);
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
			if (dist > r * r) {
				continue;
			}

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

	private boolean canAttack(LocalPlayer player, float yaw, float pitch) {
		if (player.getAttackStrengthScale(0.5F) < 0.9F) {
			// ждём почти полный кулдаун оружия: быстрый клик ловят все античиты
			return false;
		}
		if (onlyCrits.get()
			&& (player.onGround() || player.getDeltaMovement().y > 0 || player.onClimbable() || player.isInWater())) {
			return false;
		}
		if (raytrace.get()) {
			// walls = true: луч не должен упираться в блок, удар сквозь стену — мгновенный флаг
			return RotationUtil.checkRtx(player, target, yaw, pitch, range.getFloat(), range.getFloat(), true);
		}
		return true;
	}

	/**
	 * Точка прицела внутри хитбокса: центр по XZ с разбросом и живот/грудь по Y.
	 * Пересчитывается раз в 8–14 тиков, поэтому прицел «дышит» как у живого игрока,
	 * но всегда остаётся внутри коробки и raytrace проходит.
	 */
	private void refreshAimPoint(LivingEntity entity) {
		AABB box = entity.getBoundingBox();
		double halfWidth = box.getXsize() / 2.0;
		double height = box.getYsize();
		aimOffset = new Vec3(
			MathUtil.random(-halfWidth * 0.35, halfWidth * 0.35),
			height * MathUtil.random(0.45, 0.75),
			MathUtil.random(-halfWidth * 0.35, halfWidth * 0.35)
		);
	}

	private void attack(Minecraft mc, LocalPlayer player) {
		CombatUtil.beginAttack();
		try {
			mc.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		} finally {
			CombatUtil.endAttack();
		}
	}
}
