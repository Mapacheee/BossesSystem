package me.mapacheee.bossessystem.shared.placeholder;

import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import me.mapacheee.bossessystem.shared.stats.StatsService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class BossesExpansion extends PlaceholderExpansion {

  private final ArenaRegistryService arenaRegistry;
  private final SessionService sessions;
  private final StatsService stats;

  public BossesExpansion(final ArenaRegistryService arenaRegistry, final SessionService sessions, final StatsService stats) {
    this.arenaRegistry = arenaRegistry;
    this.sessions = sessions;
    this.stats = stats;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "bossesystem";
  }

  @Override
  public @NotNull String getAuthor() {
    return "Mapacheee";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0.0";
  }

  @Override
  public String onRequest(final OfflinePlayer player, final @NotNull String params) {
    if (params.startsWith("arena_")) {
      final var parts = params.substring(6).split("_");
      if (parts.length >= 2) {
        final var id = parts[0];
        final var type = parts[1];
        final var arena = this.arenaRegistry.getArena(id);
        if (arena != null) {
          if ("occupied".equals(type)) {
            return this.arenaRegistry.isOccupied(id) ? "true" : "false";
          }
          if ("free".equals(type)) {
            return this.arenaRegistry.isOccupied(id) ? "false" : "true";
          }
        }
      }
    }
    return null;
  }
}
