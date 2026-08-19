package dev.nhack.client.module.modules.combat;

import com.mojang.authlib.GameProfile;
import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.PacketSendEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BindSetting;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.ChatUtil;
import dev.nhack.client.util.ColorUtil;
import dev.nhack.client.util.FriendManager;
import dev.nhack.client.util.MathUtil;
import dev.nhack.client.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "test" — a kill aura that can be trained how to turn.
 * <p>
 * Press the {@code Record} bind while aiming to record the exact mouse motion you use
 * to track a target (a fake player is spawned in front of you for that). The recorded
 * micro-movements are then replayed on real targets at the same speed, so the aim
 * follows a curved, human-like path instead of snapping in a straight line.
 */
public final class TestModule extends Module {
	public static boolean sendingAttack;

	// Radius — 2.9 .. 4.0 blocks.
	private final NumberSetting range = addSetting(new NumberSetting("Range", "Attack reach in blocks", 3.2, 2.9, 4.0, 0.05));

	// How the crosshair moves onto the target.
	private final ModeSetting rotationMode = addSetting(new ModeSetting(
		"Rotation",
		"Curved replays your recorded motion, Straight snaps in a straight line at fixed speed",
		"Curved",
		"Curved",
		"Straight"
	));

	// Chance 0..99 to whiff — on a miss it simply does not attack that swing.
	private final NumberSetting missChance = addSetting(new NumberSetting("MissChance", "0-99% chance to just not swing", 0, 0, 99, 1));

	// Silent: only rotate the server-side body, camera stays put. Off = camera also turns.
	private final BoolSetting silent = addSetting(new BoolSetting("Silent", "Spoof body rotation only; the camera does not move", true));

	// Multiplier for the recorded speed (1.0 = exactly as recorded).
	private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Multiplier for recorded rotation speed", 1.0, 0.1, 3.0, 0.05));

	// Degrees per tick for Straight mode.
	private final NumberSetting rotationSpeed = addSetting(new NumberSetting("RotationSpeed", "Degrees per tick in Straight mode", 6.0, 1.0, 20.0, 0.5));

	private final NumberSetting attackDelay = addSetting(new NumberSetting("AttackDelay", "Ticks between swings", 3, 1, 20, 1));
	private final ModeSetting targets = addSetting(new ModeSetting("Targets", "Which entities to attack", "All", "Players", "All", "Mobs"));

	// The record button: press once to spawn a fake player + start recording your aim,
	// press again to stop, despawn the fake player and save the recorded motion.
	private final BindSetting recordKey = addSetting(new BindSetting("Record", "Press to record your aim on a fake player", GLFW.GLFW_KEY_UNKNOWN));

	private final List<float[]> record = new ArrayList<>();

	private LivingEntity target;
	private Player fakePlayer;
	private boolean recording;
	private boolean prevRecordDown;

	private float lastYaw;
	private float lastPitch;
	private float rotationYaw;
	private float rotationPitch;
	private int curveIndex;
	private int playIndex;
	private float[] cumYaw;
	private float[] cumPitch;
	private int hitTicks;
	private boolean readyForAttack;

	public TestModule() {
		super("Test", "Kill aura you can train: records your mouse motion and replays it on targets", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		target = null;
		recording = false;
		prevRecordDown = false;
		curveIndex = 0;
		playIndex = 0;
		cumYaw = null;
		cumPitch = null;
		hitTicks = 0;
		readyForAttack = false;
		if (mc.player != null) {
			rotationYaw = mc.player.getYRot();
			rotationPitch = mc.player.getXRot();
			RotationUtil.captureVisual(mc.player);
		}
	}

	@Override
	protected void onDisable() {
		if (recording) {
			recording = false;
			removeFake(Minecraft.getInstance());
		}
		target = null;
		readyForAttack = false;
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

		handleRecordKey(mc, player);
		readyForAttack = false;

		// While recording we only capture the camera motion.
		if (recording) {
			sampleRecord(player);
			RotationUtil.clear();
			return;
		}

		updateTarget(mc, player);
		if (target == null) {
			RotationUtil.clear();
			hitTicks = Math.max(0, hitTicks - 1);
			return;
		}

		aim(player);
		hitTicks = Math.max(0, hitTicks - 1);

		if (hitTicks > 0) {
			return;
		}
		// Only attack once the crosshair is actually aimed at the target's hitbox.
		if (!isAimedAt(player)) {
			readyForAttack = false;
			return;
		}
		// On a miss we simply do not attack this window.
		if (attemptMiss()) {
			hitTicks = attackDelay.getInt();
			return;
		}
		readyForAttack = true;
	}

	@Subscribe
	public void onPostTick(TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null
			|| target == null || !readyForAttack || recording) {
			return;
		}
		attack(mc, player);
	}

	@Subscribe
	public void onPacketSend(PacketSendEvent event) {
		if (target == null || sendingAttack || recording) {
			return;
		}
		if (event.packet() instanceof ServerboundInteractPacket) {
			event.cancel();
		}
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		GuiGraphics graphics = event.graphics();
		Font font = mc.font;

		String line;
		if (recording) {
			line = "REC " + record.size();
		} else if (target != null) {
			line = "T " + target.getName().getString();
		} else {
			return;
		}

		int width = font.width(line);
		graphics.fill(8, 30, 8 + width + 8, 42, 0x88000000);
		graphics.drawString(font, line, 12, 32, recording ? ColorUtil.ACCENT_HOVER : ColorUtil.TEXT);
	}

	// ------------------------------------------------------------------ recording

	private void handleRecordKey(Minecraft mc, LocalPlayer player) {
		if (mc.getWindow() == null) {
			prevRecordDown = false;
			return;
		}
		int key = recordKey.get();
		boolean down = key > GLFW.GLFW_KEY_UNKNOWN && key <= GLFW.GLFW_KEY_LAST
			&& GLFW.glfwGetKey(mc.getWindow().handle(), key) == GLFW.GLFW_PRESS;
		if (down && !prevRecordDown && mc.screen == null) {
			toggleRecording(mc, player);
		}
		prevRecordDown = down;
	}

	private void toggleRecording(Minecraft mc, LocalPlayer player) {
		if (recording) {
			recording = false;
			removeFake(mc);
			buildCurve();
			playIndex = 0;
			curveIndex = 0;
			ChatUtil.info("Recording stopped — saved " + record.size() + " frames");
			return;
		}
		if (player == null || mc.level == null) {
			return;
		}
		record.clear();
		cumYaw = null;
		cumPitch = null;
		playIndex = 0;
		curveIndex = 0;
		lastYaw = player.getYRot();
		lastPitch = player.getXRot();
		recording = true;
		spawnFake(mc, player);
		ChatUtil.info("Recording started — turn your camera to aim at the fake player");
	}

	private void spawnFake(Minecraft mc, LocalPlayer player) {
		removeFake(mc);
		RemotePlayer fake = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Fake"));
		double rad = Math.toRadians(player.getYRot());
		double range = this.range.getFloat();
		fake.setPos(player.getX() - Math.sin(rad) * range, player.getY(), player.getZ() + Math.cos(rad) * range);
		fake.setYRot(player.getYRot());
		fake.setXRot(player.getXRot());
		fake.setYHeadRot(player.getYRot());
		mc.level.addEntity(fake);
		fakePlayer = fake;
	}

	private void removeFake(Minecraft mc) {
		if (fakePlayer != null) {
			if (mc.level != null) {
				mc.level.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
			}
			fakePlayer = null;
		}
	}

	private void sampleRecord(LocalPlayer player) {
		float dyaw = Mth.wrapDegrees(player.getYRot() - lastYaw);
		float dpitch = player.getXRot() - lastPitch;
		record.add(new float[]{dyaw, dpitch});
		lastYaw = player.getYRot();
		lastPitch = player.getXRot();
		while (record.size() > 2400) {
			record.remove(0);
		}
	}

	// Pre-compute the cumulative rotation of the recording so the curve can be
	// scaled to whatever angle the real target needs while keeping its shape.
	private void buildCurve() {
		int n = record.size();
		cumYaw = new float[n];
		cumPitch = new float[n];
		float sumYaw = 0.0F;
		float sumPitch = 0.0F;
		for (int i = 0; i < n; i++) {
			float[] frame = record.get(i);
			sumYaw += frame[0];
			sumPitch += frame[1];
			cumYaw[i] = sumYaw;
			cumPitch[i] = sumPitch;
		}
	}

	// ------------------------------------------------------------------ combat

	private void updateTarget(Minecraft mc, LocalPlayer player) {
		LivingEntity candidate = findTarget(mc, player);
		if (target == null) {
			setTarget(player, candidate);
			return;
		}
		float r = range.getFloat();
		boolean invalid = target.isRemoved() || target.isDeadOrDying() || !target.isAlive()
			|| player.distanceToSqr(target) > (r + 0.5) * (r + 0.5);
		if (invalid) {
			setTarget(player, candidate);
		} else if (candidate != null && player.distanceToSqr(candidate) < player.distanceToSqr(target) - 1.0) {
			setTarget(player, candidate);
		}
	}

	// Start tracking a new target: begin the recorded curve again from the player's
	// current camera so the curved approach is visible on every engagement.
	private void setTarget(LocalPlayer player, LivingEntity candidate) {
		target = candidate;
		playIndex = 0;
		curveIndex = 0;
		if (player != null) {
			rotationYaw = player.getYRot();
			rotationPitch = player.getXRot();
		}
	}

	private LivingEntity findTarget(Minecraft mc, LocalPlayer player) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		float r = range.getFloat();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity == fakePlayer || entity == player) {
				continue;
			}
			if (!(entity instanceof LivingEntity living) || living.isDeadOrDying() || !entity.isAlive()) {
				continue;
			}
			if (!selected(entity)) {
				continue;
			}
			if (entity instanceof Player other && FriendManager.isFriend(other.getName().getString())) {
				continue;
			}
			double dist = player.distanceToSqr(entity);
			if (dist > r * r || dist >= bestDist) {
				continue;
			}
			best = living;
			bestDist = dist;
		}
		return best;
	}

	private boolean selected(Entity entity) {
		return switch (targets.get()) {
			case "Players" -> entity instanceof Player;
			case "Mobs" -> entity instanceof Mob;
			default -> true;
		};
	}

	private boolean attemptMiss() {
		if (missChance.getInt() <= 0) {
			return false;
		}
		int roll = (int) MathUtil.random(0, 100);
		return roll < missChance.getInt();
	}

	private void aim(LocalPlayer player) {
		if (target == null) {
			return;
		}
		float[] dest = RotationUtil.angles(player.getEyePosition(), target.getEyePosition());
		float targetYaw = dest[0];
		float targetPitch = dest[1];

		if (rotationMode.is("Curved") && cumYaw != null && cumYaw.length > 0) {
			curvedAim(targetYaw, targetPitch);
		} else {
			straightAim(targetYaw, targetPitch);
		}

		rotationYaw = Mth.wrapDegrees(rotationYaw);
		rotationPitch = Mth.clamp(rotationPitch, -90.0F, 90.0F);

		RotationUtil.set(rotationYaw, rotationPitch, !silent.get());
		if (!silent.get()) {
			player.setYRot(rotationYaw);
			player.setXRot(rotationPitch);
		}
	}

	// Curved: play the recorded mouse motion once, scaled to the angle we actually
	// need, so the crosshair follows the exact curve you recorded at the same speed.
	private void curvedAim(float targetYaw, float targetPitch) {
		float deltaYaw = Mth.wrapDegrees(targetYaw - rotationYaw);
		float deltaPitch = targetPitch - rotationPitch;

		if (playIndex < cumYaw.length) {
			float prevYaw = playIndex > 0 ? cumYaw[playIndex - 1] : 0.0F;
			float prevPitch = playIndex > 0 ? cumPitch[playIndex - 1] : 0.0F;
			float totalYaw = cumYaw[cumYaw.length - 1];
			float totalPitch = cumPitch[cumPitch.length - 1];

			float stepYaw = (cumYaw[playIndex] - prevYaw) * speed.getFloat();
			float stepPitch = (cumPitch[playIndex] - prevPitch) * speed.getFloat();
			if (Math.abs(totalYaw) > 0.01F) {
				stepYaw *= deltaYaw / totalYaw;
			}
			if (Math.abs(totalPitch) > 0.01F) {
				stepPitch *= deltaPitch / totalPitch;
			}
			rotationYaw += stepYaw;
			rotationPitch += stepPitch;
			playIndex++;
		} else {
			// Path is done — hold on the target with tiny recorded micro-movements.
			float[] frame = record.get(curveIndex % record.size());
			curveIndex++;
			rotationYaw += frame[0] * speed.getFloat() * 0.12F;
			rotationPitch += frame[1] * speed.getFloat() * 0.12F;
			rotationYaw += Mth.wrapDegrees(targetYaw - rotationYaw) * 0.4F;
			rotationPitch += (targetPitch - rotationPitch) * 0.4F;
		}
	}

	// Straight: fixed per-tick angular speed in a straight line.
	private void straightAim(float targetYaw, float targetPitch) {
		float stepYaw = Mth.clamp(Mth.wrapDegrees(targetYaw - rotationYaw), -rotationSpeed.getFloat(), rotationSpeed.getFloat());
		float stepPitch = Mth.clamp(targetPitch - rotationPitch, -rotationSpeed.getFloat(), rotationSpeed.getFloat());
		rotationYaw += stepYaw;
		rotationPitch += stepPitch;
	}

	// Whether the current (spoofed) rotation ray lands on the target's hitbox.
	private boolean isAimedAt(LocalPlayer player) {
		if (target == null) {
			return false;
		}
		return RotationUtil.checkRtx(player, target, rotationYaw, rotationPitch, range.getFloat(), range.getFloat(), false);
	}

	private void attack(Minecraft mc, LocalPlayer player) {
		sendingAttack = true;
		try {
			mc.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
		} finally {
			sendingAttack = false;
		}
		hitTicks = attackDelay.getInt();
	}
}
