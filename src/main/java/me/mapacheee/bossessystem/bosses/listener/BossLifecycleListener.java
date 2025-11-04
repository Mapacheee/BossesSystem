package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ListenerComponent
public final class BossLifecycleListener implements Listener {

  private static final Logger logger = LoggerFactory.getLogger(BossLifecycleListener.class);

  private final Provider<SessionService> sessions;

  @Inject
  public BossLifecycleListener(final Provider<SessionService> sessions) {
    this.sessions = sessions;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(final EntityDeathEvent event) {
    final LivingEntity entity = event.getEntity();

    logger.debug("Entity death: {} at {}", entity.getType(), entity.getLocation());

    // Verificar si es un MythicMob usando reflexión
    try {
      final Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
      final var instMethod = mythicBukkitClass.getMethod("inst");
      final var mythicInstance = instMethod.invoke(null);

      final var getAPIHelperMethod = mythicInstance.getClass().getMethod("getAPIHelper");
      final var apiHelper = getAPIHelperMethod.invoke(mythicInstance);

      final var isMythicMobMethod = apiHelper.getClass().getMethod("isMythicMob", org.bukkit.entity.Entity.class);
      final boolean isMythicMob = (boolean) isMythicMobMethod.invoke(apiHelper, entity);

      if (isMythicMob) {
        logger.debug("MythicMob death detected, notifying SessionService");
        this.sessions.get().onBossDeath(entity.getUniqueId());
      }
    } catch (Throwable e) {
      logger.debug("Error checking MythicMob: {}", e.getMessage());
    }
  }
}
