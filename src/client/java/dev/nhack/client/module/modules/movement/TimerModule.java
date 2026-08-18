package dev.nhack.client.module.modules.movement;

import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.event.events.PacketReceiveEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BindSetting;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.ChatUtil;
import dev.nhack.client.util.ColorUtil;
import dev.nhack.client.util.TickManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class TimerModule extends Module {
	private final ModeSetting mode = addSetting(new ModeSetting("Mode", "How the timer charges and spends ticks", "Normal", "Normal", "Matrix", "Shift", "Grim"));
	private final BoolSetting old = addSetting(new BoolSetting("Old", "Older Matrix drain", false));
	private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Tick multiplier", 2.0, 0.1, 10.0, 0.1));
	private final NumberSetting shiftTicks = addSetting(new NumberSetting("ShiftTicks", "Extra player ticks for Shift mode", 10, 1, 40, 1));
	private final BindSetting boostKey = addSetting(new BindSetting("BoostKey", "Hold to spend Grim charge", GLFW.GLFW_KEY_UNKNOWN));
	private final ModeSetting onFlag = addSetting(new ModeSetting("OnFlag", "What to do after a setback", "Reset", "Disable", "None", "Reset"));
	private final BoolSetting autoDisable = addSetting(new BoolSetting("AutoDisable", "Turn off after a delay", false));
	private final NumberSetting autoDisableTime = addSetting(new NumberSetting("DisableTime", "Milliseconds before auto-disable", 500, 100, 10000, 50));

	public static float energy;
	private static double prevX, prevY, prevZ;
	private static float prevYaw, prevPitch;

	private long cancelTime;
	private long enableTime;
	private long lastFlagTime;

	public TimerModule() {
		super("Timer", "Changes client tick speed (Normal / Matrix / Shift / Grim)", Category.MOVEMENT);
	}

	@Override
	protected void onEnable() {
		TickManager.setSpeed(1.0F);
		if (!mode.is("Matrix")) {
			energy = 0.0F;
		}
		if (mode.is("Grim")) {
			cancelTime = System.currentTimeMillis();
		}
		enableTime = System.currentTimeMillis();
		lastFlagTime = 0L;
		captureMotion();
	}

	@Override
	protected void onDisable() {
		TickManager.reset();
	}

	@Override
	public void onTick(TickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			TickManager.setSpeed(1.0F);
			return;
		}

		if (autoDisable.get() && System.currentTimeMillis() - enableTime >= autoDisableTime.getInt()) {
			ChatUtil.info("Timer auto-disabled");
			setEnabled(false);
			return;
		}

		syncEnergy(player);

		switch (mode.get()) {
			case "Normal" -> TickManager.setSpeed(speed.getFloat());
			case "Matrix" -> {
				if (!isMovingKeys(mc)) {
					TickManager.setSpeed(1.0F);
					return;
				}
				TickManager.setSpeed(Math.max(speed.getFloat(), 1.0F));
				if (energy > 0.0F) {
					energy = Mth.clamp(energy - ((0.1F * speed.getFloat()) - 0.1F), 0.0F, 1.0F);
				} else {
					ChatUtil.info("Timer out of charge");
					setEnabled(false);
				}
			}
			case "Grim" -> {
				long sinceFlag = lastFlagTime == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - lastFlagTime;
				if (energy <= 0.0F || !boostHeld(mc) || sinceFlag < 2000L) {
					TickManager.setSpeed(1.0F);
					return;
				}
				TickManager.setSpeed(Math.max(speed.getFloat(), 1.0F));
				energy = Mth.clamp(energy - ((0.0025F * speed.getFloat()) - 0.0025F), 0.0F, 1.0F);
			}
			case "Shift" -> {
				if (energy < 0.9F) {
					ChatUtil.info("Stand still to recharge before Shift");
					setEnabled(false);
					return;
				}
				TickManager.requestShift(shiftTicks.getInt());
				ChatUtil.info("Ticks shifted");
				setEnabled(false);
			}
			default -> TickManager.setSpeed(1.0F);
		}
	}

	@Subscribe
	public void onPacket(PacketReceiveEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mode.is("Grim") && event.packet() instanceof ClientboundPingPacket) {
			long sinceFlag = lastFlagTime == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - lastFlagTime;
			if (sinceFlag > 2000L) {
				if (System.currentTimeMillis() - cancelTime > 25_000L) {
					cancelTime = System.currentTimeMillis();
					energy = 0.0F;
					return;
				}
				if (mc.player != null && !isMovingKeys(mc)) {
					energy = Mth.clamp(energy + 0.005F, 0.0F, 1.0F);
				}
				event.cancel();
			}
		}

		if (event.packet() instanceof ClientboundPlayerPositionPacket) {
			lastFlagTime = System.currentTimeMillis();
			switch (onFlag.get()) {
				case "Reset" -> {
					TickManager.setSpeed(1.0F);
					energy = 0.0F;
				}
				case "Disable" -> {
					energy = 0.0F;
					ChatUtil.info("Timer disabled after setback");
					setEnabled(false);
				}
				default -> {
				}
			}
		}

		if (mode.is("Grim")
			&& event.packet() instanceof ClientboundSetEntityMotionPacket motion
			&& mc.player != null
			&& motion.getId() == mc.player.getId()) {
			TickManager.setSpeed(1.0F);
			energy = 0.0F;
		}
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		if (!mode.is("Matrix") && !mode.is("Grim") && !mode.is("Shift")) {
			return;
		}
		GuiGraphics graphics = event.graphics();
		Font font = Minecraft.getInstance().font;
		String text = String.format("Timer  %.0f%%", energy * 100.0F);
		graphics.drawString(font, text, 8, event.graphics().guiHeight() - 24, ColorUtil.TEXT);
	}

	private void syncEnergy(LocalPlayer player) {
		if (mode.is("Matrix")) {
			energy = Mth.clamp(notMoving(player) ? energy + 0.025F : energy - (old.get() ? 0.005F : 0.0F), 0.0F, 1.0F);
		} else if (mode.is("Shift") && notMoving(player)) {
			energy = Mth.clamp(energy + 0.05F, 0.0F, 1.0F);
		}
		prevX = player.getX();
		prevY = player.getY();
		prevZ = player.getZ();
		prevYaw = player.getYRot();
		prevPitch = player.getXRot();
	}

	private void captureMotion() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		prevX = mc.player.getX();
		prevY = mc.player.getY();
		prevZ = mc.player.getZ();
		prevYaw = mc.player.getYRot();
		prevPitch = mc.player.getXRot();
	}

	private static boolean notMoving(LocalPlayer player) {
		return prevX == player.getX() && prevY == player.getY() && prevZ == player.getZ()
			&& prevYaw == player.getYRot() && prevPitch == player.getXRot();
	}

	private static boolean isMovingKeys(Minecraft mc) {
		return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
			|| mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
	}

	private boolean boostHeld(Minecraft mc) {
		if (mc.getWindow() == null) {
			return false;
		}
		return boostKey.isDown(mc.getWindow().handle());
	}
}
