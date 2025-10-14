package me.mapacheee.bossessystem.bosses.entity;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.shared.config.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public final class ArenaRegistryService {

  private final Container<Config> config;
  private final Map<String, Boolean> occupancy = new HashMap<>();

  @Inject
  public ArenaRegistryService(final Container<Config> config) {
    this.config = config;
  }

  public Set<String> arenaIds() {
    final var arenas = this.config.get().arenas();
    return arenas != null ? arenas.keySet() : Collections.emptySet();
  }

  public boolean isOccupied(final String arenaId) {
    return this.occupancy.getOrDefault(arenaId, false);
  }

  public void setOccupied(final String arenaId, final boolean occupied) {
    this.occupancy.put(arenaId, occupied);
  }

  public Config.Arena getArena(final String arenaId) {
    final var arenas = this.config.get().arenas();
    return arenas != null ? arenas.get(arenaId) : null;
  }
}

