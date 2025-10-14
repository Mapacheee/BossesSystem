package me.mapacheee.bossessystem.bosses.entity;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.shared.config.Config;

import java.util.Map;
import java.util.Set;

@Service
public final class BossRegistryService {

  private final Container<Config> config;

  @Inject
  public BossRegistryService(final Container<Config> config) {
    this.config = config;
  }

  public Set<String> bossIds() {
    final var bosses = this.config.get().bosses();
    return bosses != null ? bosses.keySet() : Set.of();
  }

  public Config.Boss getBoss(final String bossId) {
    final Map<String, Config.Boss> bosses = this.config.get().bosses();
    if (bosses == null) return null;
    final var boss = bosses.get(bossId);
    if (boss == null) return null;
    return boss;
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

