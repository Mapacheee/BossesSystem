package me.mapacheee.bossessystem.bosses.model;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Session {
  private final String arenaId;
  private final String bossId;
  private final UUID leader;
  private final Set<UUID> participants = new HashSet<>();
  private final Set<UUID> spectators = new HashSet<>();
  private UUID bossUuid;
  private long startMillis;
  private final long timeLimitSeconds;
  private final Location arenaSpawn;
  private double price;

  public Session(final String arenaId, final String bossId, final UUID leader, final long timeLimitSeconds, final Location arenaSpawn) {
    this.arenaId = arenaId;
    this.bossId = bossId;
    this.leader = leader;
    this.timeLimitSeconds = timeLimitSeconds;
    this.arenaSpawn = arenaSpawn;
  }

  public String getArenaId() {
    return arenaId;
  }

  public String getBossId() {
    return bossId;
  }

  public UUID getLeader() {
    return leader;
  }

  public Set<UUID> getParticipants() {
    return participants;
  }

  public Set<UUID> getSpectators() {
    return spectators;
  }

  public UUID getBossUuid() {
    return bossUuid;
  }

  public void setBossUuid(final UUID bossUuid) {
    this.bossUuid = bossUuid;
  }

  public long getStartMillis() {
    return startMillis;
  }

  public void setStartMillis(final long startMillis) {
    this.startMillis = startMillis;
  }

  public long getTimeLimitSeconds() {
    return timeLimitSeconds;
  }

  public Location getArenaSpawn() {
    return arenaSpawn;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(final double price) {
    this.price = price;
  }
}
