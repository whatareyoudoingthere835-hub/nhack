package dev.nhack.client.util;

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

	private RotationUtil() {
	}

	public static void captureVisual(LocalPlayer player) {
		visualYaw = player.getYRot();
		visualPitch = player.getXRot();
	}

	public static void set(float nextYaw, float nextPitch, boolean look) {
		active = true;
		clientLook = look;
		yaw = nextYaw;
		pitch = Mth.clamp(nextPitch, -90.0F, 90.0F);
	}

	public static void clear() {
		active = false;
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
}
