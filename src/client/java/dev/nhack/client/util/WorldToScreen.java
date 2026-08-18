package dev.nhack.client.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WorldToScreen {
	private WorldToScreen() {
	}

	public static float[] project(Vec3 world) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null || mc.getWindow() == null) {
			return null;
		}

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		Vector3f relative = new Vector3f(
			(float) (world.x - cameraPos.x),
			(float) (world.y - cameraPos.y),
			(float) (world.z - cameraPos.z)
		);

		Quaternionf rotation = new Quaternionf(camera.rotation());
		rotation.conjugate();
		rotation.transform(relative);

		if (relative.z > -0.05F) {
			return null;
		}

		double fov = Math.toRadians(Math.max(30, mc.options.fov().get()));
		float tan = (float) Math.tan(fov * 0.5);
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();
		float aspect = (float) width / (float) height;

		float x = width / 2.0F - (relative.x / (relative.z * tan * aspect)) * (width / 2.0F);
		float y = height / 2.0F - (relative.y / (relative.z * tan)) * (height / 2.0F);
		return new float[]{x, y};
	}

	public static Vec3 lerp(net.minecraft.world.entity.Entity entity, float partialTick) {
		return new Vec3(
			entity.xo + (entity.getX() - entity.xo) * partialTick,
			entity.yo + (entity.getY() - entity.yo) * partialTick,
			entity.zo + (entity.getZ() - entity.zo) * partialTick
		);
	}
}
