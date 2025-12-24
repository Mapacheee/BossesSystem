package me.mapacheee.bossessystem.bosses.entity;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.BossesSystemPlugin;
import me.mapacheee.bossessystem.bosses.model.Session;
import me.mapacheee.bossessystem.shared.config.Config;
import me.mapacheee.bossessystem.shared.economy.EconomyService;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import me.mapacheee.bossessystem.shared.mythic.MythicMobsService;
import me.mapacheee.bossessystem.shared.teleport.TeleportService;
import me.mapacheee.bossessystem.shared.stats.StatsService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public final class SessionService {

  private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

  private final ArenaRegistryService arenas;
  private final BossRegistryService bosses;
  private final EconomyService economy;
  private final TeleportService teleport;
  private final MythicMobsService mythic;
  private final MessageService messages;
  private final Container<Config> config;
  private final StatsService stats;

  private final Map<String, Session> sessionsByArena = new HashMap<>();
  private final Map<UUID, String> playerToArena = new HashMap<>();
  private final Map<String, BukkitTask> timeoutTasks = new HashMap<>();
  private final Map<UUID, Long> rejoinDeadline = new HashMap<>();
  private final Set<UUID> deadOrSpectator = new HashSet<>();

  @Inject
  public SessionService(final ArenaRegistryService arenas,
                        final BossRegistryService bosses,
                        final EconomyService economy,
                        final TeleportService teleport,
                        final MythicMobsService mythic,
                        final MessageService messages,
                        final Container<Config> config,
                        final StatsService stats) {
    this.arenas = arenas;
    this.bosses = bosses;
    this.economy = economy;
    this.teleport = teleport;
    this.mythic = mythic;
    this.messages = messages;
    this.config = config;
    this.stats = stats;
  }

  public boolean hasActiveSession(final String arenaId) {
    return this.sessionsByArena.containsKey(arenaId);
  }

  public Session getSessionByArena(final String arenaId) {
    return this.sessionsByArena.get(arenaId);
  }

  public Session getSessionByPlayer(final UUID playerId) {
    final var arenaId = this.playerToArena.get(playerId);
    return arenaId == null ? null : this.sessionsByArena.get(arenaId);
  }

  public boolean isSpectator(final UUID playerId) {
    return this.deadOrSpectator.contains(playerId);
  }

  public boolean forceEndSession(final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) {
      return false;
    }

    this.mythic.despawn(session.getBossUuid());

    final List<UUID> allPlayers = new ArrayList<>();
    allPlayers.addAll(session.getParticipants());
    allPlayers.addAll(session.getSpectators());

    final long duration = session.getStartMillis() > 0
        ? System.currentTimeMillis() - session.getStartMillis()
        : 0L;

    this.endSession(arenaId, session, "aborted", duration, allPlayers);

    return true;
  }

  public void beginFromInvitation(final String arenaId, final List<UUID> participants) {
    final var arenaCfg = this.arenas.getArena(arenaId);
    if (arenaCfg == null) {
      this.sendTo(participants, p -> this.messages.errorInvalidArena(p, arenaId));
      return;
    }
    if (this.arenas.isOccupied(arenaId) || this.sessionsByArena.containsKey(arenaId)) {
      this.sendTo(participants, p -> this.messages.errorArenaOccupied(p, arenaId));
      return;
    }

    if (arenaCfg.mythicMobId() == null || arenaCfg.mythicMobId().isEmpty()) {
      this.sendTo(participants, p -> this.messages.errorArenaNoBoss(p, arenaId));
      return;
    }

    final double price = this.bosses.getPrice(arenaCfg);
    final List<Player> online = participants.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();

    if (!this.economy.isAvailable()) {
      this.sendTo(participants, p -> this.messages.errorMissingIntegration(p, "Vault"));
      return;
    }

    for (final var p : online) {
      if (!this.economy.has(p, price)) {
        this.sendTo(participants, player -> this.messages.flowCancelledInsufficientBalance(player, p.getName(), String.valueOf(price)));
        return;
      }
    }

    final List<Player> charged = new ArrayList<>();
    for (final var p : online) {
      if (this.economy.withdraw(p, price)) {
        charged.add(p);
      } else {
        charged.forEach(c -> this.economy.deposit(c, price));
        this.sendTo(participants, this.messages::endCancelled);
        return;
      }
    }

    this.arenas.setOccupied(arenaId, true);

    final World world = Bukkit.getWorld(arenaCfg.world());
    if (world == null) {
      charged.forEach(c -> this.economy.deposit(c, price));
      this.arenas.setOccupied(arenaId, false);
      this.sendTo(participants, this.messages::endCancelled);
      return;
    }

    final Location spawn = new Location(world, arenaCfg.spawn().x(), arenaCfg.spawn().y(), arenaCfg.spawn().z(), arenaCfg.spawn().yaw(), arenaCfg.spawn().pitch());
    final long timeLimit = this.bosses.getTimeLimitSeconds(arenaCfg);
    final Session session = new Session(arenaId, arenaCfg.mythicMobId(), participants.getFirst(), timeLimit, spawn);
    session.getParticipants().addAll(participants);
    session.setPrice(price);
    this.sessionsByArena.put(arenaId, session);
    participants.forEach(uid -> this.playerToArena.put(uid, arenaId));

    for (final var p : online) {
      this.messages.flowTeleporting(p);
      this.teleport.teleportTo(p, spawn);
    }

    final int spawnDelay = participants.size() == 1 ? 0 : this.bosses.getSpawnDelaySeconds(arenaCfg);

    if (spawnDelay > 0) {
      this.sendTo(participants, player -> this.messages.flowBossSpawnsIn(player, spawnDelay));
    }

    Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> {
      final var uuid = this.mythic.spawn(arenaCfg.mythicMobId(), spawn);
      session.setStartMillis(System.currentTimeMillis());
      session.setBossUuid(uuid);
      this.sendTo(participants, this.messages::flowFightStarted);
      final long ticks = timeLimit * 20L;
      final var task = Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> this.timeout(arenaId), ticks);
      this.timeoutTasks.put(arenaId, task);
    }, spawnDelay * 20L);
  }

  private void timeout(final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) return;
    this.mythic.despawn(session.getBossUuid());
    final var players = new ArrayList<>(session.getParticipants());
    final long duration = session.getStartMillis() > 0 ? System.currentTimeMillis() - session.getStartMillis() : 0L;
    this.endSession(arenaId, session, "timeout", duration, players);
  }

  private void defeat(final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) return;

    logger.info("All players died in arena '{}' - Defeat!", arenaId);

    this.mythic.despawn(session.getBossUuid());

    final List<UUID> allPlayers = new ArrayList<>();
    allPlayers.addAll(session.getParticipants());
    allPlayers.addAll(session.getSpectators());

    for (final var uid : allPlayers) {
      final var p = Bukkit.getPlayer(uid);
      if (p != null) {
        this.messages.endDefeat(p);
        this.messages.sendDefeatTitle(p, 10, 70, 20);
      }
    }

    final long duration = session.getStartMillis() > 0
        ? System.currentTimeMillis() - session.getStartMillis()
        : 0L;

    this.endSession(arenaId, session, "defeat", duration, allPlayers);
  }

  public void onBossDeath(final UUID bossUuid) {
    final var entry = this.sessionsByArena.entrySet().stream()
        .filter(e -> Objects.equals(e.getValue().getBossUuid(), bossUuid))
        .findFirst().orElse(null);

    if (entry == null) {
      return;
    }

    final var arenaId = entry.getKey();
    final var session = entry.getValue();
    logger.info("Boss defeated in arena '{}' - Victory!", arenaId);

    this.playVictoryEffects(arenaId, session);

    final var victoryConfig = this.config.get().victoryEffects();
    final int delaySeconds = victoryConfig.delayBeforeTeleportSeconds();

    Bukkit.getScheduler().runTaskLater(
        BossesSystemPlugin.get(),
        () -> {
          final long duration = session.getStartMillis() > 0
              ? System.currentTimeMillis() - session.getStartMillis()
              : 0L;
          this.endSession(arenaId, session, "victory", duration, new ArrayList<>(session.getParticipants()));

          final var leader = Bukkit.getPlayer(session.getLeader());
          this.stats.recordVictory(session.getBossId(), duration, leader != null ? leader.getName() : "");
        },
        delaySeconds * 20L
    );
  }

  private void playVictoryEffects(final String arenaId, final Session session) {
    final var victoryConfig = this.config.get().victoryEffects();
    final var participants = session.getParticipants();

    if (victoryConfig.sound().enabled()) {
      final var soundConfig = victoryConfig.sound();
      for (final var uid : participants) {
        final var p = Bukkit.getPlayer(uid);
        if (p != null) {
          this.messages.playSound(p, soundConfig.type(), (float) soundConfig.volume(), (float) soundConfig.pitch());
        }
      }
    }

    if (victoryConfig.title().enabled()) {
      final var titleConfig = victoryConfig.title();
      for (final var uid : participants) {
        final var p = Bukkit.getPlayer(uid);
        if (p != null) {
          this.messages.sendVictoryTitle(p, titleConfig.fadeIn(), titleConfig.stay(), titleConfig.fadeOut());
        }
      }
    }

    if (victoryConfig.fireworks().enabled()) {
      final var fireworksConfig = victoryConfig.fireworks();
      for (final var uid : participants) {
        final var p = Bukkit.getPlayer(uid);
        if (p != null) {
          this.spawnVictoryFireworks(p.getLocation(), fireworksConfig);
        }
      }
    }
  }

  private void spawnVictoryFireworks(final org.bukkit.Location location, final Config.VictoryEffects.Fireworks config) {
    final int count = config.count();
    final var colors = config.colors();
    final var types = config.types();
    final boolean trail = config.withTrail();
    final boolean flicker = config.withFlicker();

    for (int i = 0; i < count; i++) {
      Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> {
        final var firework = location.getWorld().spawn(location, org.bukkit.entity.Firework.class);
        final var meta = firework.getFireworkMeta();

        final var effect = org.bukkit.FireworkEffect.builder();

        if (colors != null && !colors.isEmpty()) {
          for (final var colorHex : colors) {
            try {
              final int rgb = Integer.parseInt(colorHex, 16);
              final var color = org.bukkit.Color.fromRGB(rgb);
              effect.withColor(color);
            } catch (NumberFormatException ignored) {}
          }
        } else {
          effect.withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.LIME, org.bukkit.Color.FUCHSIA);
        }

        if (types != null && !types.isEmpty()) {
          try {
            final var type = org.bukkit.FireworkEffect.Type.valueOf(types.get(0));
            effect.with(type);
          } catch (IllegalArgumentException ignored) {
            effect.with(org.bukkit.FireworkEffect.Type.BALL_LARGE);
          }
        } else {
          effect.with(org.bukkit.FireworkEffect.Type.BALL_LARGE);
        }

        if (trail) effect.withTrail();
        if (flicker) effect.withFlicker();

        meta.addEffect(effect.build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);

        Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), firework::detonate, 1L);
      }, i * 10L);
    }
  }

  private void endSession(final String arenaId, final Session session, final String result, final long durationMillis, final List<UUID> participants) {
    final var task = this.timeoutTasks.remove(arenaId);
    if (task != null) task.cancel();

    final List<UUID> allPlayers = new ArrayList<>(participants);
    allPlayers.addAll(session.getSpectators());

    this.teleport.executeEndCommands(result, arenaId, session.getBossId(), durationMillis, session.getLeader(), allPlayers);

    for (final var uid : session.getSpectators()) {
      final var sp = Bukkit.getPlayer(uid);
      if (sp != null) this.teleport.restoreGamemode(sp);
    }

    this.sessionsByArena.remove(arenaId);
    this.arenas.setOccupied(arenaId, false);
    participants.forEach(this.playerToArena::remove);
    session.getSpectators().forEach(this.playerToArena::remove);
    this.deadOrSpectator.removeAll(session.getSpectators());
  }

  private interface PlayerMessage {
    void send(Player p);
  }

  private void sendTo(final List<UUID> players, final PlayerMessage fn) {
    for (final var uid : players) {
      final var p = Bukkit.getPlayer(uid);
      if (p != null) fn.send(p);
    }
  }

  public void handlePlayerQuit(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) return;
    final long ttl = this.config.get().general().rejoinTimeoutSeconds();
    this.rejoinDeadline.put(player.getUniqueId(), System.currentTimeMillis() + ttl * 1000L);
    this.messages.flowRejoinAvailable(player, (int) ttl);
    Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> {
      final var still = this.rejoinDeadline.get(player.getUniqueId());
      final boolean expired = still != null && System.currentTimeMillis() >= still;
      final boolean stillInSession = this.getSessionByPlayer(player.getUniqueId()) != null;
      if (expired && stillInSession) {
        session.getParticipants().remove(player.getUniqueId());
        this.playerToArena.remove(player.getUniqueId());
        this.economy.deposit(player, session.getPrice());
        if (session.getParticipants().isEmpty()) {
          this.timeout(session.getArenaId());
        }
      }
      this.rejoinDeadline.remove(player.getUniqueId());
    }, ttl * 20L);
  }

  public void handlePlayerJoin(final Player player) {
    final var expiry = this.rejoinDeadline.get(player.getUniqueId());
    if (expiry == null) return;
    if (System.currentTimeMillis() > expiry) {
      this.rejoinDeadline.remove(player.getUniqueId());
      this.messages.flowInviteExpired(player);
      return;
    }
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session != null && session.getParticipants().contains(player.getUniqueId())) {
      this.teleport.teleportTo(player, session.getArenaSpawn());
      this.messages.flowRejoinSuccess(player);
    }
  }

  public void handlePlayerDeath(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) return;
    session.getParticipants().remove(player.getUniqueId());
    session.getSpectators().add(player.getUniqueId());
    this.deadOrSpectator.add(player.getUniqueId());

    if (session.getParticipants().isEmpty()) {
      this.defeat(session.getArenaId());
    } else {
      this.messages.flowPlayerDied(player);
    }
  }

  public void rejoinCommand(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) {
      this.messages.errorRejoinUnavailable(player);
      return;
    }
    if (this.deadOrSpectator.contains(player.getUniqueId())) {
      this.teleport.setSpectator(player);
      final var boss = session.getBossUuid() != null ? Bukkit.getEntity(session.getBossUuid()) : null;
      this.teleport.teleportTo(player, boss != null ? boss.getLocation() : session.getArenaSpawn());
      return;
    }
    this.teleport.teleportTo(player, session.getArenaSpawn());
    this.messages.flowRejoinSuccess(player);
  }

  public void spectateCommand(final Player player, final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) {
      this.messages.errorInvalidArena(player, arenaId);
      return;
    }
    session.getSpectators().add(player.getUniqueId());
    this.teleport.setSpectator(player);
    final var boss = session.getBossUuid() != null ? Bukkit.getEntity(session.getBossUuid()) : null;
    this.teleport.teleportTo(player, boss != null ? boss.getLocation() : session.getArenaSpawn());
  }

  public int activePlayersInArena(final String arenaId) {
    final var s = this.sessionsByArena.get(arenaId);
    return s == null ? 0 : s.getParticipants().size();
  }

  public void leaveCommand(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) return;
    if (session.getSpectators().remove(player.getUniqueId())) {
      this.teleport.restoreGamemode(player);
      this.playerToArena.remove(player.getUniqueId());
      return;
    }
    if (session.getParticipants().remove(player.getUniqueId())) {
      this.playerToArena.remove(player.getUniqueId());
      if (session.getParticipants().isEmpty()) {
        this.timeout(session.getArenaId());
      }
    }
  }
}
