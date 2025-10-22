package me.mapacheee.bossessystem.shared.placeholder;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import me.mapacheee.bossessystem.shared.stats.StatsService;

@Service
public final class PlaceholderService {

  private final ArenaRegistryService arenaRegistry;
  private final SessionService sessions;
  private final StatsService stats;

  @Inject
  public PlaceholderService(final ArenaRegistryService arenaRegistry, final SessionService sessions, final StatsService stats) {
    this.arenaRegistry = arenaRegistry;
    this.sessions = sessions;
    this.stats = stats;
  }
}