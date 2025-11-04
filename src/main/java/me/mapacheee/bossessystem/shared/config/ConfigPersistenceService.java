package me.mapacheee.bossessystem.shared.config;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;

@Service
public final class ConfigPersistenceService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigPersistenceService.class);

  private final Container<Config> config;

  @Inject
  public ConfigPersistenceService(final Container<Config> config) {
    this.config = config;
  }

  public void saveArenas() {
    if (config.save()) {
      logger.debug("Arenas saved to config");
    } else {
      logger.error("Failed to save arenas");
    }
  }

  public void putArena(final String id, final Config.Arena arena) {
    logger.debug("Guardando arena '{}' con mythicMobId: '{}'", id, arena.mythicMobId());

    try {
      Map<String, Config.Arena> arenas = this.config.get().arenas();
      if (arenas != null) {
        arenas.put(id, arena);
      }

      var arenaNode = config.getNode().node("arenas", id);

      arenaNode.node("world").set(arena.world());

      if (arena.spawn() != null) {
        arenaNode.node("spawn", "x").set(arena.spawn().x());
        arenaNode.node("spawn", "y").set(arena.spawn().y());
        arenaNode.node("spawn", "z").set(arena.spawn().z());
        arenaNode.node("spawn", "yaw").set(arena.spawn().yaw());
        arenaNode.node("spawn", "pitch").set(arena.spawn().pitch());
      }

      if (arena.mythicMobId() != null && !arena.mythicMobId().isEmpty()) {
        arenaNode.node("mythicMobId").set(arena.mythicMobId());
      }

      if (arena.price() != null) {
        arenaNode.node("price").set(arena.price());
      }

      if (arena.timeLimitSeconds() != null) {
        arenaNode.node("timeLimitSeconds").set(arena.timeLimitSeconds());
      }

      if (arena.maxPlayers() != null) {
        arenaNode.node("maxPlayers").set(arena.maxPlayers());
      }

      if (arena.spawnDelaySeconds() != null) {
        arenaNode.node("spawnDelaySeconds").set(arena.spawnDelaySeconds());
      }

      boolean saved = config.save();

      if (!saved) {
        logger.error("Error al guardar arena '{}' al archivo", id);
      }

    } catch (SerializationException e) {
      logger.error("Error al serializar arena '{}'", id, e);
    }
  }

  public void removeArena(final String id) {
    try {
      config.getNode().node("arenas", id).set(null);

      boolean saved = config.save();

      if (saved) {
        config.reload();
        logger.debug("Arena '{}' eliminada exitosamente", id);
      } else {
        logger.error("Error al eliminar arena '{}'", id);
      }

    } catch (SerializationException e) {
      logger.error("Error al eliminar arena '{}'", id, e);
    }
  }
}
