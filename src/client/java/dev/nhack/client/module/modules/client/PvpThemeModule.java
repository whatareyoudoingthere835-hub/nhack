package dev.nhack.client.module.modules.client;

import dev.nhack.NHack;
import dev.nhack.client.event.Subscribe;
import dev.nhack.client.event.events.AttackEvent;
import dev.nhack.client.event.events.TickEvent;
import dev.nhack.client.module.Category;
import dev.nhack.client.module.Module;
import dev.nhack.client.setting.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Context music for PvP and nearby team situations.
 *
 * <p>The seven tracks are ordinary mod resources in {@code assets/nhack/sounds}. A trigger starts
 * a track once on the transition into its state; it is not restarted every tick. The MUSIC sound
 * category is used, so the normal Minecraft music-volume slider controls these themes.
 */
public final class PvpThemeModule extends Module {
	private static final int COMBAT_TIMEOUT_TICKS = 200; // ten seconds without a hit
	private static final long TEAM_COOLDOWN_NANOS = 10L * 60L * 1_000_000_000L;
	private static final double VISION_HALF_ANGLE_RADIANS = Math.toRadians(50.0); // 100 degree cone

	private final BoolSetting highHpPvp = addSetting(new BoolSetting(
		"high_hp_pvp", "Начать при первом ударе по игроку", true));
	private final BoolSetting lowHpPvp = addSetting(new BoolSetting(
		"low_hp_pvp", "Заменить high_hp_pvp при здоровье ниже половины", true));
	private final BoolSetting ifSeeTeam = addSetting(new BoolSetting(
		"if_see_team", "Больше одного игрока с командным тегом в поле зрения", true));
	private final BoolSetting diedPvp = addSetting(new BoolSetting(
		"died_pvp", "Воспроизвести при смерти в PvP", true));
	private final BoolSetting killstreak = addSetting(new BoolSetting(
		"killstreak", "Воспроизвести на каждом пятом убийстве игрока", true));
	private final BoolSetting teamPvp = addSetting(new BoolSetting(
		"team_pvp", "Воспроизвести при появлении тимейта, кулдаун 10 минут", true));
	private final BoolSetting winAllPvp = addSetting(new BoolSetting(
		"win_all_pvp", "Воспроизвести, когда в табе остаётся только ты", true));

	private final Map<UUID, Player> pendingKills = new HashMap<>();
	private SoundInstance currentTheme;
	private Object knownLevel;
	private boolean pvpActive;
	private boolean lowThemeStarted;
	private int combatIdleTicks;
	private int killCount;
	private boolean wasDead;
	private boolean manyTaggedVisible;
	private boolean teammateVisible;
	private long nextTeamThemeNanos;
	private boolean tabStateKnown;
	private boolean tabWasSolo;

	public PvpThemeModule() {
		super("PvpTheme", "Музыкальные темы для PvP, тимейтов, побед и смерти", Category.CLIENT);
	}

	@Override
	protected void onEnable() {
		stopTheme();
		resetState(true);
		knownLevel = Minecraft.getInstance().level;
	}

	@Override
	protected void onDisable() {
		stopTheme();
		resetState(true);
		knownLevel = null;
	}

	/** Vanilla and KillAura both pass through MultiPlayerGameMode.attack(). */
	@Subscribe
	public void onAttack(AttackEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || event.attacker() != mc.player || !(event.target() instanceof Player victim)
			|| victim == mc.player) {
			return;
		}

		startPvp(mc.player);
		pendingKills.put(victim.getUUID(), victim);
	}

	@Subscribe
	public void onPreTick(TickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			if (knownLevel != null) {
				stopTheme();
				resetState(true);
				knownLevel = null;
			}
			return;
		}

		if (knownLevel != mc.level) {
			stopTheme();
			resetState(true);
			knownLevel = mc.level;
		}

		Player player = mc.player;
		boolean dead = player.isDeadOrDying() || player.getHealth() <= 0.0F;
		if (dead) {
			if (!wasDead) {
				onDeath(player);
			}
			wasDead = true;
			return;
		}
		if (wasDead) {
			// The death theme is allowed to finish; the next attack starts a new combat theme.
			resetCombatState(false);
		}
		wasDead = false;

		if (player.hurtTime > 0 && player.getLastHurtByPlayer() != null) {
			startPvp(player);
		}

		if (pvpActive) {
			combatIdleTicks++;
			if (combatIdleTicks >= COMBAT_TIMEOUT_TICKS) {
				endPvp();
			}
		}

		if (pvpActive && !lowThemeStarted && player.getHealth() <= player.getMaxHealth() * 0.5F) {
			startLowHealthTheme();
		}

		checkPendingKills();
		scanVisibleTeams(mc, player);
		scanTab(mc, player);
	}

	private void startPvp(Player player) {
		combatIdleTicks = 0;
		if (pvpActive) {
			return;
		}

		pvpActive = true;
		lowThemeStarted = false;
		if (player.getHealth() <= player.getMaxHealth() * 0.5F) {
			startLowHealthTheme();
		} else {
			playTheme("high_hp_pvp");
		}
	}

	private void startLowHealthTheme() {
		if (lowThemeStarted) {
			return;
		}
		lowThemeStarted = true;
		// Even when the low-health file is disabled, the high-health track must stop here.
		stopTheme();
		playTheme("low_hp_pvp");
	}

	private void endPvp() {
		pvpActive = false;
		lowThemeStarted = false;
		combatIdleTicks = 0;
		pendingKills.clear();
		stopTheme();
	}

	private void onDeath(Player player) {
		if (diedPvp.get() && (pvpActive || player.getLastHurtByPlayer() != null)) {
			stopTheme();
			playTheme("died_pvp");
		} else {
			stopTheme();
		}
		pvpActive = false;
		lowThemeStarted = false;
		combatIdleTicks = 0;
		pendingKills.clear();
		killCount = 0;
	}

	private void checkPendingKills() {
		if (pendingKills.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Player>> iterator = pendingKills.entrySet().iterator();
		while (iterator.hasNext()) {
			Player victim = iterator.next().getValue();
			if (victim.isRemoved()) {
				iterator.remove();
				continue;
			}
			if (!victim.isDeadOrDying() && victim.getHealth() > 0.0F) {
				continue;
			}

			iterator.remove();
			killCount++;
			if (killCount % 5 == 0 && killstreak.get()) {
				playTheme("killstreak");
			}
		}
	}

	private void scanVisibleTeams(Minecraft mc, Player self) {
		int taggedPlayers = 0;
		boolean teammate = false;
		String ownTag = teamTag(self);

		for (Object value : mc.level.players()) {
			if (!(value instanceof Player other) || other == self || !canSee(self, other)) {
				continue;
			}

			String otherTag = teamTag(other);
			if (!otherTag.isEmpty()) {
				taggedPlayers++;
			}
			if (!ownTag.isEmpty() && ownTag.equals(otherTag)) {
				teammate = true;
			}
		}

		boolean manyTagged = taggedPlayers > 1;
		if (ifSeeTeam.get() && manyTagged && !manyTaggedVisible) {
			playTheme("if_see_team");
		}
		manyTaggedVisible = manyTagged;

		long now = System.nanoTime();
		if (teamPvp.get() && teammate && !teammateVisible && now >= nextTeamThemeNanos) {
			playTheme("team_pvp");
			nextTeamThemeNanos = now + TEAM_COOLDOWN_NANOS;
		}
		teammateVisible = teammate;
	}

	private void scanTab(Minecraft mc, Player self) {
		if (mc.getConnection() == null) {
			return;
		}

		int listed = 0;
		boolean selfListed = mc.getConnection().getPlayerInfo(self.getUUID()) != null;
		for (Object value : mc.getConnection().getListedOnlinePlayers()) {
			if (value instanceof PlayerInfo) {
				listed++;
			}
		}

		boolean solo = selfListed && listed == 1;
		if (!tabStateKnown) {
			tabStateKnown = true;
		} else if (winAllPvp.get() && solo && !tabWasSolo) {
			playTheme("win_all_pvp");
		}
		tabWasSolo = solo;
	}

	private boolean canSee(Player self, Player other) {
		if (!other.isAlive() || other.isSpectator() || other.isInvisible() || !self.hasLineOfSight(other)) {
			return false;
		}
		Vec3 to = other.getBoundingBox().getCenter().subtract(self.getEyePosition(1.0F));
		if (to.lengthSqr() < 1.0E-6) {
			return true;
		}
		return self.getViewVector(1.0F).dot(to.normalize()) >= Math.cos(VISION_HALF_ANGLE_RADIANS);
	}

	/** Prefix/suffix is what appears in the tab; team name covers servers that use T1/T2 as name. */
	private String teamTag(Player player) {
		PlayerTeam team = player.getTeam();
		if (team == null) {
			return "";
		}
		String prefix = team.getPlayerPrefix().getString().trim();
		if (!prefix.isEmpty()) {
			return prefix;
		}
		String suffix = team.getPlayerSuffix().getString().trim();
		if (!suffix.isEmpty()) {
			return suffix;
		}
		return team.getName().trim();
	}

	private void playTheme(String name) {
		if (!themeEnabled(name)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager() == null) {
			return;
		}
		stopTheme();
		SoundEvent sound = SoundEvent.createVariableRangeEvent(NHack.id(name));
		SimpleSoundInstance instance = SimpleSoundInstance.forMusic(sound);
		mc.getSoundManager().play(instance);
		currentTheme = instance;
	}

	private boolean themeEnabled(String name) {
		return switch (name) {
			case "high_hp_pvp" -> highHpPvp.get();
			case "low_hp_pvp" -> lowHpPvp.get();
			case "if_see_team" -> ifSeeTeam.get();
			case "died_pvp" -> diedPvp.get();
			case "killstreak" -> killstreak.get();
			case "team_pvp" -> teamPvp.get();
			case "win_all_pvp" -> winAllPvp.get();
			default -> false;
		};
	}

	private void stopTheme() {
		if (currentTheme == null) {
			return;
		}
		Minecraft.getInstance().getSoundManager().stop(currentTheme);
		currentTheme = null;
	}

	private void resetState(boolean resetCooldown) {
		resetCombatState(true);
		wasDead = false;
		manyTaggedVisible = false;
		teammateVisible = false;
		tabStateKnown = false;
		tabWasSolo = false;
		if (resetCooldown) {
			nextTeamThemeNanos = 0L;
		}
	}

	private void resetCombatState(boolean clearKills) {
		pvpActive = false;
		lowThemeStarted = false;
		combatIdleTicks = 0;
		pendingKills.clear();
		if (clearKills) {
			killCount = 0;
		}
	}
}
