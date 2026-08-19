package dev.nhack.client.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class WorldToScreen {
	private WorldToScreen() {
	}

	/**
	 * Projects a world-space point into GUI coordinates.
	 * Returns {@code null} when the point is behind (or almost behind) the camera.
	 *
	 * <p>Minecraft's camera basis is +Z forward, +Y up, +X left — not OpenGL's -Z view space.
	 */
	public static float[] project(Vec3 world) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null || mc.getWindow() == null) {
			return null;
		}

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		double dx = world.x - cameraPos.x;
		double dy = world.y - cameraPos.y;
		double dz = world.z - cameraPos.z;

		Vector3fc forward = camera.forwardVector();
		Vector3fc up = camera.upVector();
		Vector3fc left = camera.leftVector();

		float depth = (float) (dx * forward.x() + dy * forward.y() + dz * forward.z());
		if (depth < 0.15F) {
			return null;
		}

		float camLeft = (float) (dx * left.x() + dy * left.y() + dz * left.z());
		float camUp = (float) (dx * up.x() + dy * up.y() + dz * up.z());

		double fov = Math.toRadians(Math.max(30, mc.options.fov().get()));
		float tan = (float) Math.tan(fov * 0.5);
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();
		float aspect = (float) width / (float) height;

		float x = width / 2.0F - (camLeft / (depth * tan * aspect)) * (width / 2.0F);
		float y = height / 2.0F - (camUp / (depth * tan)) * (height / 2.0F);
		return new float[]{x, y};
	}

	public static Vec3 lerp(Entity entity, float partialTick) {
		return new Vec3(
			entity.xo + (entity.getX() - entity.xo) * partialTick,
			entity.yo + (entity.getY() - entity.yo) * partialTick,
			entity.zo + (entity.getZ() - entity.zo) * partialTick
		);
	}
}
