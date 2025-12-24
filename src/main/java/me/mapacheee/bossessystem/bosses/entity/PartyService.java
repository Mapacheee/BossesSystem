package me.mapacheee.bossessystem.bosses.entity;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.BossesSystemPlugin;
import me.mapacheee.bossessystem.shared.config.Config;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.*;

@Service
public final class PartyService {

  public static final class Invitation {
    public final UUID leader;
    public final String arenaId;
    public final Set<UUID> invited = new HashSet<>();
    public final Set<UUID> accepted = new HashSet<>();
    public final Set<UUID> rejected = new HashSet<>();
    public final Instant createdAt = Instant.now();
    public BukkitTask timeoutTask;

    Invitation(final UUID leader, final String arenaId) {
      this.leader = leader; this.arenaId = arenaId;
    }
  }

  private final Container<Config> config;
  private final MessageService messages;
  private final me.mapacheee.bossessystem.bosses.entity.SessionService sessions;

  private final Map<UUID, Invitation> pendingByLeader = new HashMap<>();
  private final Map<UUID, UUID> pendingByInvitee = new HashMap<>();

  @Inject
  public PartyService(final Container<Config> config,
                      final MessageService messages,
                      final me.mapacheee.bossessystem.bosses.entity.SessionService sessions) {
    this.config = config; this.messages = messages; this.sessions = sessions;
  }

  public Invitation startInvites(final Player leader, final String arenaId, final Collection<Player> toInvite) {
    this.clear(leader.getUniqueId());

    final var inv = new Invitation(leader.getUniqueId(), arenaId);
    toInvite.forEach(p -> {
      inv.invited.add(p.getUniqueId());
      this.pendingByInvitee.put(p.getUniqueId(), leader.getUniqueId());
    });
    this.pendingByLeader.put(leader.getUniqueId(), inv);

    if (inv.invited.isEmpty()) {
      this.begin(inv);
      return inv;
    }

    final long delayTicks = this.config.get().general().invitation().autoExpireSeconds() * 20L;
    inv.timeoutTask = Bukkit.getScheduler().runTaskLater(BossesSystemPlugin.get(), () -> this.expire(leader.getUniqueId()), delayTicks);

    return inv;
  }

  public Optional<Invitation> getForLeader(final UUID leader) {
    return Optional.ofNullable(this.pendingByLeader.get(leader));
  }

  public Optional<Invitation> getForInvitee(final UUID invitee) {
    final var leader = this.pendingByInvitee.get(invitee);
    return leader == null ? Optional.empty() : this.getForLeader(leader);
  }

  public void accept(final Player invitee) {
    final var opt = this.getForInvitee(invitee.getUniqueId());
    if (opt.isEmpty()) return;
    final var inv = opt.get();
    if (this.isExpired(inv)) {
      this.expire(inv.leader);
      return;
    }

    if (inv.accepted.contains(invitee.getUniqueId())) {
      this.messages.flowInviteAlreadyAccepted(invitee);
      return;
    }

    inv.accepted.add(invitee.getUniqueId());
    final var leader = Bukkit.getPlayer(inv.leader);
    if (leader != null) {
      this.messages.flowInviteAccepted(leader, invitee.getName());
    }
    final boolean requireAll = this.config.get().general().invitation().requireAllAccepted();
    if (requireAll && inv.accepted.containsAll(inv.invited)) {
      if (leader != null) this.messages.flowAllAccepted(leader);
      this.begin(inv);
    }
  }

  public void reject(final Player invitee) {
    final var opt = this.getForInvitee(invitee.getUniqueId());
    if (opt.isEmpty()) return;
    final var inv = opt.get();
    inv.rejected.add(invitee.getUniqueId());
    final var leader = Bukkit.getPlayer(inv.leader);
    if (leader != null) {
      this.messages.flowInviteRejected(leader, invitee.getName());
    }
    this.cancel(inv.leader);
  }

  public boolean isExpired(final Invitation inv) {
    final var ttl = this.config.get().general().invitation().autoExpireSeconds();
    return Instant.now().isAfter(inv.createdAt.plusSeconds(ttl));
  }

  public void clear(final UUID leader) {
    final var inv = this.pendingByLeader.remove(leader);
    if (inv != null) {
      if (inv.timeoutTask != null) inv.timeoutTask.cancel();
      inv.invited.forEach(this.pendingByInvitee::remove);
    }
  }

  public void cancel(final UUID leader) {
    final var inv = this.pendingByLeader.get(leader);
    if (inv == null) return;
    this.clear(leader);
  }

  private void expire(final UUID leader) {
    final var inv = this.pendingByLeader.get(leader);
    if (inv == null) return;
    final var leaderP = Bukkit.getPlayer(inv.leader);
    if (leaderP != null) this.messages.flowInviteExpired(leaderP);
    for (final var uid : inv.invited) {
      final var p = Bukkit.getPlayer(uid);
      if (p != null) this.messages.flowInviteExpired(p);
    }
    this.clear(leader);
  }

  private void begin(final Invitation inv) {
    final java.util.List<java.util.UUID> participants = new java.util.ArrayList<>();
    participants.add(inv.leader);
    if (!inv.invited.isEmpty()) participants.addAll(inv.accepted);

    this.clear(inv.leader);

    this.sessions.beginFromInvitation(inv.arenaId, participants);
  }
}
