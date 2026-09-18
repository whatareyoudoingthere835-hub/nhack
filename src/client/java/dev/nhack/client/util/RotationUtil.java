package dev.nhack.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil {
	public static boolean active;
	public static boolean clientLook;
	public static float yaw;
	public static float pitch;
	public static float visualYaw;
	public static float visualPitch;

	/**
	 * Поворот, который сервер реально получил последним пакетом движения. Боевые проверки
	 * античита смотрят именно на него, а не на то, что клиент нарисовал себе в этом тике.
	 */
	public static float sentYaw;
	public static float sentPitch;

	private RotationUtil() {
	}

	public static void captureVisual(LocalPlayer player) {
		visualYaw = player.getYRot();
		visualPitch = player.getXRot();
	}

	/** Ставит поворот сразу (с точностью до шага мыши). Для плавной наводки есть {@link #smooth}. */
	public static void set(float nextYaw, float nextPitch, boolean look) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			active = false;
			return;
		}

		engage(player);
		clientLook = look;
		yaw += quantize(Mth.wrapDegrees(nextYaw - yaw), mc);
		pitch = Mth.clamp(pitch + quantize(nextPitch - pitch, mc), -90.0F, 90.0F);
	}

	/**
	 * Плавная наводка: за кадр проходится не больше {@code maxDegreesPerTick * dtTicks} градусов,
	 * с замедлением у цели и случайным разбросом, поэтому движение не выглядит линейным.
	 * Каждый шаг приводится к сетке мыши — сервер видит «честные» дельты.
	 *
	 * @param maxDegreesPerTick скорость наводки в градусах за игровой тик
	 * @param dtTicks           сколько тиков времени прошло с прошлого кадра
	 */
	public static void smooth(float targetYaw, float targetPitch, float maxDegreesPerTick, float dtTicks, boolean look) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			active = false;
			return;
		}

		engage(player);
		clientLook = look;

		float dt = Mth.clamp(dtTicks, 0.0F, 4.0F);
		if (dt <= 0.0F) {
			return;
		}

		float budget = Math.max(0.05F, maxDegreesPerTick) * dt;
		float deltaYaw = Mth.wrapDegrees(targetYaw - yaw);
		float deltaPitch = Mth.clamp(targetPitch, -90.0F, 90.0F) - pitch;

		yaw += quantize(ease(deltaYaw, budget), mc);
		pitch = Mth.clamp(pitch + quantize(ease(deltaPitch, budget), mc), -90.0F, 90.0F);
	}

	/** Обрезает шаг бюджетом и замедляет его у цели (ease-out), чтобы не было равномерного «робота». */
	private static float ease(float delta, float budget) {
		if (delta == 0.0F) {
			return 0.0F;
		}
		float limited = Mth.clamp(delta, -budget, budget);
		float progress = Mth.clamp(Math.abs(delta) / (budget * 2.5F), 0.0F, 1.0F);
		return limited * (0.5F + 0.5F * progress);
	}

	/**
	 * Первый вызов после {@link #clear()}: стартуем от реального взгляда игрока и идём к цели
	 * постепенно. Без этого сервер увидел бы мгновенный доворот на цель в одном пакете.
	 */
	private static void engage(LocalPlayer player) {
		if (active) {
			return;
		}
		active = true;
		yaw = player.getYRot();
		pitch = player.getXRot();
		sentYaw = yaw;
		sentPitch = pitch;
	}

	public static void clear() {
		active = false;
	}

	/** Фиксирует, что именно ушло на сервер в этом тике. Вызывается из миксина {@code sendPosition}. */
	public static void markSent() {
		sentYaw = yaw;
		sentPitch = pitch;
	}

	/**
	 * Множитель чувствительности мыши из ванильного {@code MouseHandler.turnPlayer}:
	 * {@code (sensitivity * 0.6 + 0.2)^3 * 8}, а с подзорной трубой в первом лице — без восьмёрки.
	 */
	public static double sensitivityScale(Minecraft mc) {
		// OptionInstance<Double>, но читаем через Number — так не зависит от того,
		// как именно объявлена опция в текущих маппингах.
		Object raw = mc.options.sensitivity().get();
		double value = raw instanceof Number number ? number.doubleValue() : 0.5;
		double factor = value * 0.6 + 0.2;
		double cubed = factor * factor * factor;
		LocalPlayer player = mc.player;
		if (player != null && player.isScoping() && mc.options.getCameraType().isFirstPerson()) {
			return cubed;
		}
		return cubed * 8.0;
	}

	/**
	 * Приводит дельту поворота к сетке мыши: {@code (float)(counts * sens) * 0.15F} — ровно та
	 * арифметика, что в {@code Entity.turn}. Grim и Polar считают GCD дельт поворота и ждут
	 * кратности этому шагу; произвольные float из {@code atan2} ловятся за пару секунд боя.
	 */
	public static float quantize(float delta, Minecraft mc) {
		double sens = sensitivityScale(mc);
		if (!(sens > 1.0E-4)) {
			return delta;
		}
		double counts = Math.round(delta / (sens * 0.15));
		return (float) (counts * sens) * 0.15F;
	}

	/** Шаг мыши в градусах — насколько «грубой» получается наводка при текущей чувствительности. */
	public static float sensitivityUnit(Minecraft mc) {
		return (float) (sensitivityScale(mc) * 0.15);
	}

	public static float[] angles(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		float nextYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float nextPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));
		return new float[]{Mth.wrapDegrees(nextYaw), Mth.clamp(nextPitch, -90.0F, 90.0F)};
	}

	public static boolean inFov(LocalPlayer player, Vec3 point, float fov) {
		if (fov >= 180.0F) {
			return true;
		}
		float[] rot = angles(player.getEyePosition(), point);
		float delta = Math.abs(Mth.wrapDegrees(rot[0] - Mth.wrapDegrees(player.getYRot())));
		return delta <= fov;
	}

	public static double squaredDistanceFromEyes(LocalPlayer player, Vec3 point) {
		return player.getEyePosition().distanceToSqr(point);
	}

	public static boolean checkRtx(LocalPlayer player, Entity target, float yaw, float pitch, float range, float wallRange, boolean walls) {
		Vec3 start = player.getEyePosition();
		Vec3 look = Vec3.directionFromRotation(pitch, yaw);
		Vec3 end = start.add(look.scale(range));
		AABB box = target.getBoundingBox().inflate(0.05);
		var hit = box.clip(start, end);
		if (hit.isEmpty()) {
			return false;
		}

		if (!walls || player.level() == null) {
			return true;
		}

		BlockHitResult block = player.level().clip(new ClipContext(
			start,
			hit.get(),
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		));
		if (block.getType() == HitResult.Type.MISS) {
			return true;
		}
		return start.distanceTo(block.getLocation()) <= wallRange + 0.05;
	}

	public static boolean canSeePoint(LocalPlayer player, Entity target, float range, float wallRange) {
		double half = target.getBoundingBox().getXsize() / 2.0;
		double height = target.getBoundingBox().getYsize();
		for (float x = (float) -half; x <= half; x += 0.2F) {
			for (float z = (float) -half; z <= half; z += 0.2F) {
				for (float y = 0.05F; y <= height; y += 0.25F) {
					Vec3 point = new Vec3(target.getX() + x, target.getY() + y, target.getZ() + z);
					if (squaredDistanceFromEyes(player, point) > range * range) {
						continue;
					}
					float[] rot = angles(player.getEyePosition(), point);
					if (checkRtx(player, target, rot[0], rot[1], range, wallRange, true)) {
						return true;
					}
				}
			}
		}
		return squaredDistanceFromEyes(player, target.getEyePosition()) <= wallRange * wallRange;
	}

	public static Vec3 lerp(Entity entity, float partialTick) {
		return new Vec3(
			entity.xo + (entity.getX() - entity.xo) * partialTick,
			entity.yo + (entity.getY() - entity.yo) * partialTick,
			entity.zo + (entity.getZ() - entity.zo) * partialTick
		);
	}
}
