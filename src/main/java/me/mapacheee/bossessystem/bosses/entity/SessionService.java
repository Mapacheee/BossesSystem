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

import java.util.*;

@Service
public final class SessionService {

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

    final var bossCfg = this.bosses.getBoss(arenaCfg.bossId());
    if (bossCfg == null) {
      this.sendTo(participants, p -> this.messages.errorInvalidBoss(p, arenaCfg.bossId()));
      return;
    }

    final double price = bossCfg.price();
    final List<Player> online = participants.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    for (final var p : online) {
      if (!this.economy.isAvailable() || !this.economy.has(p, price)) {
        this.messages.errorInsufficientBalance(p, p.getName(), String.valueOf(price));
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
    final long timeLimit = bossCfg.timeLimitSeconds() > 0 ? bossCfg.timeLimitSeconds() : this.bosses.defaultTimeLimitSeconds();
    final Session session = new Session(arenaId, arenaCfg.bossId(), participants.get(0), timeLimit, spawn);
    session.participants.addAll(participants);
    session.price = price;
    this.sessionsByArena.put(arenaId, session);
    participants.forEach(uid -> this.playerToArena.put(uid, arenaId));

    for (final var p : online) {
      this.messages.flowTeleporting(p);
      this.teleport.teleportTo(p, spawn);
    }

    final int spawnDelay = arenaCfg.spawnDelaySeconds() != null ? arenaCfg.spawnDelaySeconds() : (bossCfg.spawnDelaySeconds() > 0 ? bossCfg.spawnDelaySeconds() : this.bosses.defaultSpawnDelaySeconds());
    this.sendTo(participants, player -> this.messages.flowBossSpawnsIn(player, spawnDelay));

    Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> {
      final var uuid = this.mythic.spawn(bossCfg.mythicId(), spawn);
      session.startMillis = System.currentTimeMillis();
      session.bossUuid = uuid;
      this.sendTo(participants, this.messages::flowFightStarted);
      final long ticks = timeLimit * 20L;
      final var task = Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> this.timeout(arenaId), ticks);
      this.timeoutTasks.put(arenaId, task);
    }, spawnDelay * 20L);
  }

  private void timeout(final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) return;
    this.mythic.despawn(session.bossUuid);
    final var players = new ArrayList<>(session.participants);
    final long duration = session.startMillis > 0 ? System.currentTimeMillis() - session.startMillis : 0L;
    this.endSession(arenaId, session, "timeout", duration, players);
  }

  public void onBossDeath(final UUID bossUuid) {
    final var entry = this.sessionsByArena.entrySet().stream()
        .filter(e -> Objects.equals(e.getValue().bossUuid, bossUuid))
        .findFirst().orElse(null);
    if (entry == null) return;
    final var arenaId = entry.getKey();
    final var session = entry.getValue();
    final long duration = session.startMillis > 0 ? System.currentTimeMillis() - session.startMillis : 0L;
    this.endSession(arenaId, session, "victory", duration, new ArrayList<>(session.participants));
    final var leader = Bukkit.getPlayer(session.leader);
    this.stats.recordVictory(session.bossId, duration, leader != null ? leader.getName() : "");
  }

  private void endSession(final String arenaId, final Session session, final String result, final long durationMillis, final List<UUID> participants) {
    final var task = this.timeoutTasks.remove(arenaId);
    if (task != null) task.cancel();

    this.teleport.executeEndCommands(result, arenaId, session.bossId, durationMillis, session.leader, participants);

    for (final var uid : session.spectators) {
      final var sp = Bukkit.getPlayer(uid);
      if (sp != null) this.teleport.restoreGamemode(sp);
    }

    this.sessionsByArena.remove(arenaId);
    this.arenas.setOccupied(arenaId, false);
    participants.forEach(this.playerToArena::remove);
    this.deadOrSpectator.removeAll(session.spectators);
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
        session.participants.remove(player.getUniqueId());
        this.playerToArena.remove(player.getUniqueId());
        this.economy.deposit(player, session.price);
        if (session.participants.isEmpty()) {
          this.timeout(session.arenaId);
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
    if (session != null && session.participants.contains(player.getUniqueId())) {
      this.teleport.teleportTo(player, session.arenaSpawn);
      this.messages.flowRejoinSuccess(player);
    }
  }

  public void handlePlayerDeath(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) return;
    session.participants.remove(player.getUniqueId());
    session.spectators.add(player.getUniqueId());
    this.deadOrSpectator.add(player.getUniqueId());
    this.messages.flowRejoinSpectator(player);
    if (session.participants.isEmpty()) {
      this.timeout(session.arenaId);
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
      final var boss = session.bossUuid != null ? Bukkit.getEntity(session.bossUuid) : null;
      this.teleport.teleportTo(player, boss != null ? boss.getLocation() : session.arenaSpawn);
      return;
    }
    this.teleport.teleportTo(player, session.arenaSpawn);
    this.messages.flowRejoinSuccess(player);
  }

  public void spectateCommand(final Player player, final String arenaId) {
    final var session = this.sessionsByArena.get(arenaId);
    if (session == null) {
      this.messages.errorInvalidArena(player, arenaId);
      return;
    }
    session.spectators.add(player.getUniqueId());
    this.teleport.setSpectator(player);
    final var boss = session.bossUuid != null ? Bukkit.getEntity(session.bossUuid) : null;
    this.teleport.teleportTo(player, boss != null ? boss.getLocation() : session.arenaSpawn);
  }

  public int activePlayersInArena(final String arenaId) {
    final var s = this.sessionsByArena.get(arenaId);
    return s == null ? 0 : s.participants.size();
  }

  public void leaveCommand(final Player player) {
    final var session = this.getSessionByPlayer(player.getUniqueId());
    if (session == null) return;
    if (session.spectators.remove(player.getUniqueId())) {
      this.teleport.restoreGamemode(player);
      this.playerToArena.remove(player.getUniqueId());
      return;
    }
    if (session.participants.remove(player.getUniqueId())) {
      this.playerToArena.remove(player.getUniqueId());
      if (session.participants.isEmpty()) {
        this.timeout(session.arenaId);
      }
    }
  }
}
