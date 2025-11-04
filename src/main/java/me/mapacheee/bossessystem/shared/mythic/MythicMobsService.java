package me.mapacheee.bossessystem.shared.mythic;

import com.thewinterframework.service.annotation.Service;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

@Service
public final class MythicMobsService {

  private static final Logger logger = LoggerFactory.getLogger(MythicMobsService.class);

  public UUID spawn(final String mythicId, final Location location) {
    logger.debug("Attempting to spawn MythicMob '{}' at {}", mythicId, location.getWorld().getName());

    try {
      final Class<?> mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
      final Method inst = mythicBukkit.getMethod("inst");
      final Object mythic = inst.invoke(null);
      final Method getAPIHelper = mythic.getClass().getMethod("getAPIHelper");
      final Object helper = getAPIHelper.invoke(mythic);

      try {
        final Method spawn = helper.getClass().getMethod("spawnMythicMob", String.class, Location.class);
        final Object activeMob = spawn.invoke(helper, mythicId, location);

        if (activeMob == null) {
          logger.error("Failed to spawn MythicMob '{}' - spawnMythicMob returned null", mythicId);
          return null;
        }

        logger.debug("ActiveMob spawned, class: {}", activeMob.getClass().getSimpleName());

        try {
          final Method getEntity = activeMob.getClass().getMethod("getEntity");
          final Object abstractEntity = getEntity.invoke(activeMob);

          if (abstractEntity instanceof Entity) {
            UUID uuid = ((Entity) abstractEntity).getUniqueId();
            logger.debug("UUID extracted via getEntity: {}", uuid);
            return uuid;
          }

          final Method getBukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity");
          final Entity entity = (Entity) getBukkitEntity.invoke(abstractEntity);
          logger.debug("UUID extracted via getEntity->getBukkitEntity: {}", entity.getUniqueId());
          return entity.getUniqueId();
        } catch (Throwable e1) {
          logger.debug("getEntity() not available, trying alternative methods");
        }

        if (activeMob instanceof Entity) {
          UUID uuid = ((Entity) activeMob).getUniqueId();
          logger.debug("UUID extracted directly from Entity: {}", uuid);
          return uuid;
        }

        try {
          final Method getUniqueId = activeMob.getClass().getMethod("getUniqueId");
          UUID uuid = (UUID) getUniqueId.invoke(activeMob);
          logger.debug("UUID extracted via getUniqueId: {}", uuid);
          return uuid;
        } catch (Throwable e3) {
          logger.debug("getUniqueId() not available");
        }

        logger.error("Could not extract UUID from MythicMob '{}'", mythicId);

      } catch (NoSuchMethodException ex) {
        logger.error("MythicMobs API method not found - is MythicMobs outdated?");
      }
    } catch (Throwable t) {
      logger.error("Error spawning MythicMob '{}': {}", mythicId, t.getMessage());
    }

    return null;
  }

  public void despawn(final UUID uuid) {
    if (uuid == null) return;
    final var entity = Bukkit.getEntity(uuid);
    if (entity != null) {
      entity.remove();
      logger.info("Despawned entity with UUID: {}", uuid);
    }
  }
}
