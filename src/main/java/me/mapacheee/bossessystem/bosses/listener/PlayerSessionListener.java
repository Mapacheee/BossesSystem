package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@ListenerComponent
public final class PlayerSessionListener implements Listener {

  private final SessionService sessions;

  @Inject
  public PlayerSessionListener(final SessionService sessions) { this.sessions = sessions; }

  @EventHandler
  public void onQuit(final PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    this.sessions.handlePlayerQuit(player);
  }

  @EventHandler
  public void onJoin(final PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    this.sessions.handlePlayerJoin(player);
  }

  @EventHandler
  public void onDeath(final PlayerDeathEvent event) {
    final Player player = event.getEntity();
    this.sessions.handlePlayerDeath(player);
  }
}

