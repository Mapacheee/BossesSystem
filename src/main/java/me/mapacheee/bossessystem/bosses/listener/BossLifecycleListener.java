package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class BossLifecycleListener implements Listener {

  private final Provider<SessionService> sessions;

  @Inject
  public BossLifecycleListener(final Provider<SessionService> sessions) { this.sessions = sessions; }

  @EventHandler
  public void onMythicMobDeath(final Event event) {
    try {
      if (!event.getClass().getName().equals("io.lumine.mythic.bukkit.events.MythicMobDeathEvent")) {
        return;
      }
      final var getMobMethod = event.getClass().getMethod("getMob");
      final var activeMob = getMobMethod.invoke(event);
      final var getEntityMethod = activeMob.getClass().getMethod("getEntity");
      final var mythicEntity = getEntityMethod.invoke(activeMob);
      final var getBukkitEntityMethod = mythicEntity.getClass().getMethod("getBukkitEntity");
      final var bukkit = getBukkitEntityMethod.invoke(mythicEntity);
      if (bukkit != null) {
        final var getUniqueIdMethod = bukkit.getClass().getMethod("getUniqueId");
        final var uuid = (UUID) getUniqueIdMethod.invoke(bukkit);
        this.sessions.get().onBossDeath(uuid);
      }
    } catch (Throwable ignored) {
    }
  }
}
