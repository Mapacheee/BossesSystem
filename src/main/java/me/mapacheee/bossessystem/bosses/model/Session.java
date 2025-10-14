package me.mapacheee.bossessystem.bosses.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Session {
  public final String arenaId;
  public final String bossId;
  public final UUID leader;
  public final Set<UUID> participants = new HashSet<>();
  public final Set<UUID> spectators = new HashSet<>();
  public UUID bossUuid; // boss entity
  public long startMillis;
  public long timeLimitSeconds;
  public Location arenaSpawn;
  public double price;

  public Session(final String arenaId, final String bossId, final UUID leader, final long timeLimitSeconds, final Location arenaSpawn) {
    this.arenaId = arenaId;
    this.bossId = bossId;
    this.leader = leader;
    this.timeLimitSeconds = timeLimitSeconds;
    this.arenaSpawn = arenaSpawn;
  }
}
