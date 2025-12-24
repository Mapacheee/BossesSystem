package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import me.mapacheee.bossessystem.shared.config.Config;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

@ListenerComponent
public final class CommandRestrictionListener implements Listener {

  private final SessionService sessions;
  private final Container<Config> config;
  private final MessageService messages;

  @Inject
  public CommandRestrictionListener(final SessionService sessions,
                                     final Container<Config> config,
                                     final MessageService messages) {
    this.sessions = sessions;
    this.config = config;
    this.messages = messages;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCommand(final PlayerCommandPreprocessEvent event) {
    final Player player = event.getPlayer();

    final var session = this.sessions.getSessionByPlayer(player.getUniqueId());
    if (session == null) {
      return;
    }

    if (this.sessions.isSpectator(player.getUniqueId())) {
      return;
    }

    final var restrictions = this.config.get().general().commandRestrictions();
    if (!restrictions.enabled()) {
      return;
    }

    final String message = event.getMessage();
    final String command = message.substring(1).split(" ")[0].toLowerCase();

    final boolean isWhitelisted = restrictions.whitelist().stream()
        .anyMatch(whitelisted -> {
          final String normalizedWhitelist = whitelisted.toLowerCase().trim();
          return command.equals(normalizedWhitelist)
              || command.startsWith(normalizedWhitelist + ":");
        });

    if (!isWhitelisted) {
      event.setCancelled(true);
      this.messages.errorCommandBlockedInCombat(player);
    }
  }
}

