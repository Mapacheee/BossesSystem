package me.mapacheee.bossessystem.shared.mythic;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public final class MythicMobsService {

  private static final Logger logger = LoggerFactory.getLogger(MythicMobsService.class);

  private final Provider<SessionService> sessionsProvider;
  private final BossLifecycleListener listener;

  @Inject
  public MythicMobsService(final Provider<SessionService> sessionsProvider) {
    this.sessionsProvider = sessionsProvider;
    this.listener = new BossLifecycleListener(sessionsProvider);
  }

  @OnEnable
  void registerListener() {
    Bukkit.getPluginManager().registerEvents(this.listener, me.mapacheee.bossessystem.BossesSystemPlugin.get());
  }

  public @Nullable UUID spawn(final String mythicId, final Location location) {
    logger.info("Attempting to spawn MythicMob '{}' at location: {}", mythicId, location);

    try {
      final var mythicMobs = MythicBukkit.inst();
      if (mythicMobs == null) {
        logger.error("MythicBukkit instance is null - is MythicMobs installed?");
        return null;
      }

      final var apiHelper = mythicMobs.getAPIHelper();
      if (apiHelper == null) {
        logger.error("MythicMobs APIHelper is null");
        return null;
      }

      logger.info("Using MythicMobs API to spawn mob");
      final var spawnedEntity = apiHelper.spawnMythicMob(mythicId, location);

      if (spawnedEntity == null) {
        logger.error("Failed to spawn MythicMob '{}' - spawnMythicMob returned null. Check if the mob ID exists in MythicMobs config.", mythicId);
        return null;
      }

      logger.info("MythicMob spawned successfully, extracting entity UUID...");

      final UUID uuid = spawnedEntity.getUniqueId();
      logger.info("MythicMob '{}' spawned successfully with UUID: {}", mythicId, uuid);
      return uuid;

    } catch (Exception e) {
      logger.error("Error spawning MythicMob '{}': {}", mythicId, e.getMessage(), e);
      return null;
    }
  }

  public void despawn(final UUID uuid) {
    if (uuid == null) return;
    final var entity = Bukkit.getEntity(uuid);
    if (entity != null) {
      entity.remove();
      logger.info("Despawned entity with UUID: {}", uuid);
    }
  }

  private static final class BossLifecycleListener implements Listener {
    private final Provider<SessionService> sessionsProvider;

    BossLifecycleListener(final Provider<SessionService> sessionsProvider) {
      this.sessionsProvider = sessionsProvider;
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent event) {
      final var entity = event.getEntity();
      final var uuid = entity.getUniqueId();

      final var mythicMobs = MythicBukkit.inst();
      if (mythicMobs == null) {
        return;
      }

      final var mobManager = mythicMobs.getMobManager();
      final var mythicMobOpt = mobManager.getActiveMob(uuid);

      if (mythicMobOpt.isEmpty()) {
        return;
      }

      logger.info("MythicMob death detected! Notifying SessionService...");
      this.sessionsProvider.get().onBossDeath(uuid);
      logger.info("SessionService notified successfully");
    }
  }
}
