package me.mapacheee.bossessystem.shared.teleport;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.shared.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public final class TeleportService {

  private static final Logger logger = LoggerFactory.getLogger(TeleportService.class);

  private final Container<Config> config;
  private final Map<UUID, GameMode> previousGamemode = new HashMap<>();

  @Inject
  public TeleportService(final Container<Config> config) {
    this.config = config;
  }

  public void teleportTo(final Player player, final Location location) {
    if (location.getWorld() == null) return;
    player.teleport(location);
  }

  public void setSpectator(final Player player) {
    final var spectators = this.config.get().spectators();
    if (!spectators.enabled()) return;
    this.previousGamemode.putIfAbsent(player.getUniqueId(), player.getGameMode());
    player.setGameMode(GameMode.SPECTATOR);
  }

  public void restoreGamemode(final Player player) {
    final var gm = this.previousGamemode.remove(player.getUniqueId());
    if (gm != null) {
      player.setGameMode(gm);
    }
  }

  public void executeEndCommands(final String result, final String arenaId, final String bossId, final long durationMillis, final UUID leader, final List<UUID> players) {
    final var exit = this.config.get().exit().onEnd();
    final Config.Exit.OnEnd.Phase phase = switch (result) {
      case "victory" -> exit.victory();
      case "defeat" -> exit.defeat();
      case "timeout" -> exit.timeout();
      default -> exit.aborted();
    };

    if (phase == null || phase.commands() == null || phase.commands().isEmpty()) {
      return;
    }

    final var dispatchAs = phase.dispatchAs();
    final var leaderPlayer = leader != null ? Bukkit.getPlayer(leader) : null;
    final var leaderName = leaderPlayer != null ? leaderPlayer.getName() : "";

    for (final var uid : players) {
      final var player = Bukkit.getPlayer(uid);
      if (player == null) continue;

      for (final var raw : phase.commands()) {
        if (raw == null || raw.trim().isEmpty()) continue;

        final var cmd = raw
            .replace("{player}", player.getName())
            .replace("{leader}", leaderName)
            .replace("{arena}", arenaId)
            .replace("{boss}", bossId != null ? bossId : "")
            .replace("{result}", result)
            .replace("{durationMillis}", String.valueOf(durationMillis));

        try {
          if ("player".equalsIgnoreCase(dispatchAs)) {
            player.performCommand(cmd);
          } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
          }
        } catch (Exception e) {
          logger.warn("Error executing command: {} - {}", cmd, e.getMessage());
        }
      }
    }
  }
}
