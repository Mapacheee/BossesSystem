package me.mapacheee.bossessystem.bosses.entity;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.shared.config.Config;

@Service
public final class BossRegistryService {

  private final Container<Config> config;

  @Inject
  public BossRegistryService(final Container<Config> config) {
    this.config = config;
  }

  public double getPrice(final Config.Arena arena) {
    return arena.price() != null ? arena.price() : this.config.get().general().defaultPrice();
  }

  public int getTimeLimitSeconds(final Config.Arena arena) {
    return arena.timeLimitSeconds() != null ? arena.timeLimitSeconds() : this.config.get().general().defaultTimeLimitSeconds();
  }

  public int getMaxPlayers(final Config.Arena arena) {
    return arena.maxPlayers() != null ? arena.maxPlayers() : this.config.get().general().defaultMaxPlayers();
  }

  public int getSpawnDelaySeconds(final Config.Arena arena) {
    return arena.spawnDelaySeconds() != null ? arena.spawnDelaySeconds() : this.config.get().general().defaultSpawnDelaySeconds();
  }

  public int defaultMaxPlayers() {
    return this.config.get().general().defaultMaxPlayers();
  }

  public int defaultTimeLimitSeconds() {
    return this.config.get().general().defaultTimeLimitSeconds();
  }

  public int defaultSpawnDelaySeconds() {
    return this.config.get().general().defaultSpawnDelaySeconds();
  }
}
