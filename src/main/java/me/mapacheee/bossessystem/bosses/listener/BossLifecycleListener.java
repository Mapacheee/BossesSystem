package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@ListenerComponent
public final class BossLifecycleListener implements Listener {

  private final SessionService sessions;

  @Inject
  public BossLifecycleListener(final SessionService sessions) { this.sessions = sessions; }

  @EventHandler
  public void onMythicMobDeath(final MythicMobDeathEvent event) {
    try {
      final var activeMob = event.getMob();
      final var bukkit = activeMob.getEntity().getBukkitEntity();
      if (bukkit != null) {
        this.sessions.onBossDeath(bukkit.getUniqueId());
      }
    } catch (Throwable ignored) {
    }
  }
}

