package me.mapacheee.bossessystem.shared.placeholder;

import com.google.inject.Inject;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import me.mapacheee.bossessystem.shared.stats.StatsService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

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

  @OnEnable
  void register() {
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
      return;
    }
    new BossesExpansion(this.arenaRegistry, this.sessions, this.stats).register();
  }

  static final class BossesExpansion extends PlaceholderExpansion {
    private final ArenaRegistryService arenaRegistry;
    private final SessionService sessions;
    private final StatsService stats;

    BossesExpansion(final ArenaRegistryService arenaRegistry, final SessionService sessions, final StatsService stats) {
      this.arenaRegistry = arenaRegistry;
      this.sessions = sessions;
      this.stats = stats;
    }

    @Override
    public @NotNull String getIdentifier() { return "bossesystem"; }
    @Override
    public @NotNull String getAuthor() { return "Mapacheee"; }
    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public String onPlaceholderRequest(final org.bukkit.entity.Player player, @NotNull final String params) {
      if (params.startsWith("arena_status_")) {
        final String arenaId = params.substring("arena_status_".length());
        if (arenaId.isEmpty()) return "";
        final boolean occupied = this.arenaRegistry.isOccupied(arenaId);
        return occupied ? "ocupada" : "libre";
      }
      if (params.startsWith("arena_boss_")) {
        final String arenaId = params.substring("arena_boss_".length());
        final var arena = this.arenaRegistry.getArena(arenaId);
        return arena == null ? "" : arena.bossId();
      }
      if (params.startsWith("arena_players_")) {
        final String arenaId = params.substring("arena_players_".length());
        return String.valueOf(this.sessions.activePlayersInArena(arenaId));
      }
      if (params.startsWith("boss_best_time_")) {
        final String bossId = params.substring("boss_best_time_".length());
        return String.valueOf(this.stats.bestTimeMillis(bossId));
      }
      if (params.startsWith("boss_defeats_")) {
        final String bossId = params.substring("boss_defeats_".length());
        return String.valueOf(this.stats.defeats(bossId));
      }
      return null;
    }
  }
}
