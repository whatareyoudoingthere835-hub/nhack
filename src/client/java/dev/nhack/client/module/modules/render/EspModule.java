package dev.nhack.client.module.modules.render;

import dev.nhack.client.event.events.HudRenderEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import dev.nhack.client.setting.ColorSetting;
import dev.nhack.client.setting.ModeSetting;
import dev.nhack.client.setting.NumberSetting;
import dev.nhack.client.util.FriendManager;
import dev.nhack.client.util.WorldToScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EspModule extends Module {
	private final BoolSetting lingeringPotions = addSetting(new BoolSetting("LingeringPotions", "Show lingering cloud radius", false));
	private final BoolSetting tntFuse = addSetting(new BoolSetting("TNTFuse", "Show TNT fuse", false));
	private final NumberSetting tntRange = addSetting(new NumberSetting("TNTRange", "Blast radius overlay", 8.0, 0.0, 8.0, 0.5));
	private final ColorSetting tntFuseText = addSetting(new ColorSetting("TNTFuseText", "Fuse text color", 0xFFFFFFFF));
	private final BoolSetting tntRadius = addSetting(new BoolSetting("TNTRadius", "Show TNT range text", false));
	private final ColorSetting tntRadiusColor = addSetting(new ColorSetting("TNTSphereColor", "TNT range color", 0xFFFFFFFF));
	private final BoolSetting beaconRadius = addSetting(new BoolSetting("BeaconRadius", "Show nearby beacons", false));
	private final BoolSetting keepY = addSetting(new BoolSetting("KeepY", "Ignore beacon height when listing", false));
	private final ColorSetting sphereColor = addSetting(new ColorSetting("SphereColor", "Beacon tag color", 0xFFFFFFFF));
	private final ColorSetting beaconColor = addSetting(new ColorSetting("BeakonColor", "Beacon box color", 0xFFFFFFFF));
	private final BoolSetting burrow = addSetting(new BoolSetting("Burrow", "Mark players inside hard blocks", false));
	private final ColorSetting burrowTextColor = addSetting(new ColorSetting("BurrowTextColor", "Burrow text", 0xFFFFFFFF));
	private final ColorSetting burrowColor = addSetting(new ColorSetting("BurrowColor", "Burrow box", 0xFFFF9200));
	private final BoolSetting pearls = addSetting(new BoolSetting("Pearls", "Pearl tracer + distance", false));
	private final BoolSetting dizorentRadius = addSetting(new BoolSetting("DizorentRadius", "Highlight nearby players while holding an eye", true));
	private final ColorSetting dizorentColor = addSetting(new ColorSetting("DizorentColor", "Eye highlight", 0xB300F1CC));

	private final BoolSetting players = addSetting(new BoolSetting("Players", "Box players", true));
	private final BoolSetting friends = addSetting(new BoolSetting("Friends", "Box friends", true));
	private final BoolSetting crystals = addSetting(new BoolSetting("Crystals", "Box end crystals", true));
	private final BoolSetting creatures = addSetting(new BoolSetting("Creatures", "Box animals", false));
	private final BoolSetting monsters = addSetting(new BoolSetting("Monsters", "Box monsters", false));
	private final BoolSetting ambients = addSetting(new BoolSetting("Ambients", "Box ambient mobs", false));
	private final BoolSetting others = addSetting(new BoolSetting("Others", "Box everything else", false));
	private final BoolSetting outline = addSetting(new BoolSetting("Outline", "Draw a black outline", true));
	private final ModeSetting colorMode = addSetting(new ModeSetting("ColorMode", "Box color source", "SyncColor", "SyncColor", "Custom"));
	private final BoolSetting renderHealth = addSetting(new BoolSetting("RenderHealth", "Health bar", true));

	private final ColorSetting playersC = addSetting(new ColorSetting("PlayersC", "Player box", 0xFFFF9200));
	private final ColorSetting friendsC = addSetting(new ColorSetting("FriendsC", "Friend box", 0xFF30FF00));
	private final ColorSetting crystalsC = addSetting(new ColorSetting("CrystalsC", "Crystal box", 0xFF00BBFF));
	private final ColorSetting creaturesC = addSetting(new ColorSetting("CreaturesC", "Creature box", 0xFFA0A4A6));
	private final ColorSetting monstersC = addSetting(new ColorSetting("MonstersC", "Monster box", 0xFFFF0000));
	private final ColorSetting ambientsC = addSetting(new ColorSetting("AmbientsC", "Ambient box", 0xFF7B00FF));
	private final ColorSetting othersC = addSetting(new ColorSetting("OthersC", "Other box", 0xFFFF0062));
	private final ColorSetting healthB = addSetting(new ColorSetting("HealthB", "Low health", 0xFFFF1100));
	private final ColorSetting healthU = addSetting(new ColorSetting("HealthU", "Full health", 0xFF2FFF00));

	public EspModule() {
		super("ESP", "2D boxes, TNT, pearls, burrow, beacons and lingering clouds", Category.RENDER);
	}

	@Override
	public void onRenderHud(HudRenderEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			return;
		}

		GuiGraphics graphics = event.graphics();
		Font font = mc.font;
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		for (Entity entity : mc.level.entitiesForRendering()) {
			if (shouldRender(mc, entity)) {
				drawBox(graphics, font, entity, pt);
			}
			if (lingeringPotions.get() && entity instanceof AreaEffectCloud cloud) {
				drawCloud(graphics, font, cloud, pt);
			}
			if ((tntFuse.get() || tntRadius.get()) && entity instanceof PrimedTnt tnt) {
				drawTnt(graphics, font, tnt, pt);
			}
			if (pearls.get() && entity instanceof ThrownEnderpearl pearl) {
				drawPearl(graphics, font, mc, pearl, pt);
			}
			if (dizorentRadius.get() && entity instanceof Player other && other != mc.player
				&& mc.player.getMainHandItem().is(Items.ENDER_EYE)
				&& mc.player.distanceToSqr(other) <= 100.0) {
				drawBoxColored(graphics, other, pt, dizorentColor.argb());
			}
		}

		if (burrow.get()) {
			for (Player player : mc.level.players()) {
				drawBurrow(graphics, font, mc, player, pt);
			}
		}

		if (beaconRadius.get()) {
			drawBeacons(graphics, font, mc);
		}
	}

	private boolean shouldRender(Minecraft mc, Entity entity) {
		if (entity == null || mc.player == null || entity == mc.player) {
			return false;
		}
		if (entity instanceof Player player) {
			if (FriendManager.isFriend(player.getName().getString())) {
				return friends.get();
			}
			return players.get();
		}
		if (entity instanceof EndCrystal) {
			return crystals.get();
		}
		MobCategory category = entity.getType().getCategory();
		if (category == MobCategory.CREATURE || category == MobCategory.WATER_CREATURE) {
			return creatures.get();
		}
		if (category == MobCategory.MONSTER) {
			return monsters.get();
		}
		if (category == MobCategory.AMBIENT || category == MobCategory.WATER_AMBIENT) {
			return ambients.get();
		}
		return others.get();
	}

	private int entityColor(Entity entity) {
		if (colorMode.is("SyncColor")) {
			return cycle(entity.getId());
		}
		if (entity instanceof Player player) {
			if (FriendManager.isFriend(player.getName().getString())) {
				return friendsC.argb();
			}
			return playersC.argb();
		}
		if (entity instanceof EndCrystal) {
			return crystalsC.argb();
		}
		MobCategory category = entity.getType().getCategory();
		if (category == MobCategory.CREATURE || category == MobCategory.WATER_CREATURE) {
			return creaturesC.argb();
		}
		if (category == MobCategory.MONSTER) {
			return monstersC.argb();
		}
		if (category == MobCategory.AMBIENT || category == MobCategory.WATER_AMBIENT) {
			return ambientsC.argb();
		}
		return othersC.argb();
	}

	private void drawBox(GuiGraphics graphics, Font font, Entity entity, float pt) {
		drawBoxColored(graphics, entity, pt, entityColor(entity));
		if (renderHealth.get() && entity instanceof LivingEntity living && living.getHealth() > 0) {
			float[] box = projectBox(entity, pt);
			if (box == null) {
				return;
			}
			float height = box[3] - box[1];
			float ratio = Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
			int barX = (int) box[0] - 4;
			graphics.fill(barX, (int) box[1], barX + 2, (int) box[3], 0xFF000000);
			int top = (int) (box[3] - height * ratio);
			graphics.fill(barX, top, barX + 2, (int) box[3], healthU.argb());
			graphics.fill(barX, top, barX + 2, top + 1, healthB.argb());
		}
	}

	private void drawBoxColored(GuiGraphics graphics, Entity entity, float pt, int color) {
		float[] box = projectBox(entity, pt);
		if (box == null) {
			return;
		}
		int x1 = (int) box[0];
		int y1 = (int) box[1];
		int x2 = (int) box[2];
		int y2 = (int) box[3];
		if (outline.get()) {
			graphics.fill(x1 - 1, y1 - 1, x2 + 1, y1, 0xFF000000);
			graphics.fill(x1 - 1, y2, x2 + 1, y2 + 1, 0xFF000000);
			graphics.fill(x1 - 1, y1, x1, y2, 0xFF000000);
			graphics.fill(x2, y1, x2 + 1, y2, 0xFF000000);
		}
		graphics.fill(x1, y1, x2, y1 + 1, color);
		graphics.fill(x1, y2 - 1, x2, y2, color);
		graphics.fill(x1, y1, x1 + 1, y2, color);
		graphics.fill(x2 - 1, y1, x2, y2, color);
	}

	private float[] projectBox(Entity entity, float pt) {
		Vec3 pos = WorldToScreen.lerp(entity, pt);
		AABB bb = entity.getBoundingBox();
		AABB shifted = new AABB(
			bb.minX - entity.getX() + pos.x - 0.05,
			bb.minY - entity.getY() + pos.y,
			bb.minZ - entity.getZ() + pos.z - 0.05,
			bb.maxX - entity.getX() + pos.x + 0.05,
			bb.maxY - entity.getY() + pos.y + 0.15,
			bb.maxZ - entity.getZ() + pos.z + 0.05
		);

		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		boolean any = false;
		for (Vec3 corner : corners(shifted)) {
			float[] screen = WorldToScreen.project(corner);
			if (screen == null) {
				continue;
			}
			any = true;
			minX = Math.min(minX, screen[0]);
			minY = Math.min(minY, screen[1]);
			maxX = Math.max(maxX, screen[0]);
			maxY = Math.max(maxY, screen[1]);
		}
		return any ? new float[]{minX, minY, maxX, maxY} : null;
	}

	private static Vec3[] corners(AABB box) {
		return new Vec3[]{
			new Vec3(box.minX, box.minY, box.minZ),
			new Vec3(box.minX, box.maxY, box.minZ),
			new Vec3(box.maxX, box.minY, box.minZ),
			new Vec3(box.maxX, box.maxY, box.minZ),
			new Vec3(box.minX, box.minY, box.maxZ),
			new Vec3(box.minX, box.maxY, box.maxZ),
			new Vec3(box.maxX, box.minY, box.maxZ),
			new Vec3(box.maxX, box.maxY, box.maxZ)
		};
	}

	private void drawCloud(GuiGraphics graphics, Font font, AreaEffectCloud cloud, float pt) {
		float[] screen = WorldToScreen.project(WorldToScreen.lerp(cloud, pt));
		if (screen == null) {
			return;
		}
		String text = String.format("cloud %.1f", Math.max(0.0F, cloud.getRadius() * 10.0F - 5.0F));
		int x = (int) screen[0] - font.width(text) / 2;
		int y = (int) screen[1] - 10;
		graphics.drawString(font, text, x, y, 0xFFFFFFFF);
	}

	private void drawTnt(GuiGraphics graphics, Font font, PrimedTnt tnt, float pt) {
		float[] screen = WorldToScreen.project(WorldToScreen.lerp(tnt, pt).add(0, 0.5, 0));
		if (screen == null) {
			return;
		}
		int x = (int) screen[0];
		int y = (int) screen[1];
		if (tntFuse.get()) {
			String text = String.format("%.1fs", tnt.getFuse() / 20.0F);
			graphics.drawString(font, text, x - font.width(text) / 2, y - 10, tntFuseText.argb());
		}
		if (tntRadius.get()) {
			String range = String.format("r%.1f", tntRange.getFloat());
			graphics.drawString(font, range, x - font.width(range) / 2, y + 2, tntRadiusColor.argb());
		}
	}

	private void drawPearl(GuiGraphics graphics, Font font, Minecraft mc, ThrownEnderpearl pearl, float pt) {
		Vec3 pos = WorldToScreen.lerp(pearl, pt);
		double dx = pos.x - mc.player.getX();
		double dz = pos.z - mc.player.getZ();
		float yaw = (float) -(Math.atan2(dx, dz) * (180.0 / Math.PI)) - mc.player.getYRot();
		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 2;
		double rad = Math.toRadians(yaw);
		int px = cx + (int) (Math.sin(rad) * 50);
		int py = cy - (int) (Math.cos(rad) * 50);
		graphics.fill(px - 3, py - 3, px + 3, py + 3, 0xFF00BBFF);
		String dist = String.format("%.1fm", mc.player.distanceTo(pearl));
		graphics.drawString(font, dist, px - font.width(dist) / 2, py - 12, 0xFFFFFFFF);
	}

	private void drawBurrow(GuiGraphics graphics, Font font, Minecraft mc, Player player, float pt) {
		BlockPos pos = BlockPos.containing(player.getX(), player.getY() + 0.15, player.getZ());
		var block = mc.level.getBlockState(pos).getBlock();
		if (block != Blocks.OBSIDIAN && block != Blocks.CRYING_OBSIDIAN && block != Blocks.ANVIL
			&& block != Blocks.PLAYER_HEAD && block != Blocks.SKELETON_SKULL && block != Blocks.WITHER_SKELETON_SKULL) {
			return;
		}
		float[] screen = WorldToScreen.project(WorldToScreen.lerp(player, pt).add(0, 0.5, 0));
		if (screen == null) {
			return;
		}
		int x = (int) screen[0];
		int y = (int) screen[1];
		graphics.fill(x - 18, y - 8, x + 18, y + 8, burrowColor.argb() & 0x88FFFFFF | 0x33000000);
		graphics.drawString(font, "BURROW", x - font.width("BURROW") / 2, y - 4, burrowTextColor.argb());
	}

	private void drawBeacons(GuiGraphics graphics, Font font, Minecraft mc) {
		BlockPos origin = mc.player.blockPosition();
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		for (int cx = chunkX - 4; cx <= chunkX + 4; cx++) {
			for (int cz = chunkZ - 4; cz <= chunkZ + 4; cz++) {
				LevelChunk chunk = mc.level.getChunk(cx, cz);
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof BeaconBlockEntity)) {
						continue;
					}
					BlockPos pos = entity.getBlockPos();
					if (!keepY.get() && Math.abs(pos.getY() - origin.getY()) > 256) {
						continue;
					}
					float[] screen = WorldToScreen.project(Vec3.atCenterOf(pos));
					if (screen == null) {
						continue;
					}
					int x = (int) screen[0];
					int y = (int) screen[1];
					graphics.fill(x - 16, y - 8, x + 16, y + 8, beaconColor.argb() & 0x55FFFFFF | 0x22000000);
					graphics.drawString(font, "BEACON", x - font.width("BEACON") / 2, y - 4, sphereColor.argb());
				}
			}
		}
	}

	private static int cycle(int seed) {
		float hue = (seed * 37 % 360) / 360.0F;
		return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8F, 1.0F);
	}
}
