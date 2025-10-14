package me.mapacheee.bossessystem.shared.config;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.BossesSystemPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public final class ConfigPersistenceService {

  private final Container<Config> config;

  @Inject
  public ConfigPersistenceService(final Container<Config> config) {
    this.config = config;
  }

  public void saveArenas() {
    try {
      final YamlConfiguration yaml = loadYaml();
      yaml.set("arenas", null);
      final Map<String, Config.Arena> arenas = this.config.get().arenas();
      if (arenas != null) {
        for (final var e : arenas.entrySet()) {
          writeArenaToYaml(yaml, e.getKey(), e.getValue());
        }
      }
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void putArena(final String id, final Config.Arena arena) {
    try {
      final YamlConfiguration yaml = loadYaml();
      writeArenaToYaml(yaml, id, arena);
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void removeArena(final String id) {
    try {
      final YamlConfiguration yaml = loadYaml();
      yaml.set("arenas." + id, null);
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void writeArenaToYaml(final YamlConfiguration yaml, final String id, final Config.Arena a) {
    final String base = "arenas." + id + ".";
    yaml.set(base + "world", a.world());
    if (a.spawn() != null) {
      yaml.set(base + "spawn.x", a.spawn().x());
      yaml.set(base + "spawn.y", a.spawn().y());
      yaml.set(base + "spawn.z", a.spawn().z());
      yaml.set(base + "spawn.yaw", a.spawn().yaw());
      yaml.set(base + "spawn.pitch", a.spawn().pitch());
    }
    yaml.set(base + "bossId", a.bossId());
    if (a.spawnDelaySeconds() != null) {
      yaml.set(base + "spawnDelaySeconds", a.spawnDelaySeconds());
    }
  }

  public void saveBosses() {
    try {
      final YamlConfiguration yaml = this.loadYaml();
      yaml.set("bosses", null);
      final Map<String, Config.Boss> bosses = this.config.get().bosses();
      if (bosses != null) {
        for (final var e : bosses.entrySet()) {
          writeBossToYaml(yaml, e.getKey(), e.getValue());
        }
      }
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void putBoss(final String id, final Config.Boss boss) {
    try {
      final YamlConfiguration yaml = loadYaml();
      writeBossToYaml(yaml, id, boss);
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void removeBoss(final String id) {
    try {
      final YamlConfiguration yaml = loadYaml();
      yaml.set("bosses." + id, null);
      yaml.save(getConfigFile());
      this.config.reload();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void writeBossToYaml(final YamlConfiguration yaml, final String id, final Config.Boss b) {
    final String base = "bosses." + id + ".";
    yaml.set(base + "mythicId", b.mythicId());
    yaml.set(base + "price", b.price());
    yaml.set(base + "timeLimitSeconds", b.timeLimitSeconds());
    yaml.set(base + "maxPlayers", b.maxPlayers());
    yaml.set(base + "spawnDelaySeconds", b.spawnDelaySeconds());
  }

  private static File getConfigFile() {
    final File dataFolder = BossesSystemPlugin.get().getDataFolder();
    if (!dataFolder.exists()) dataFolder.mkdirs();
    final File file = new File(dataFolder, "config.yml");
    if (!file.exists()) {
      BossesSystemPlugin.get().saveResource("config.yml", false);
    }
    return file;
  }

  private static YamlConfiguration loadYaml() {
    return YamlConfiguration.loadConfiguration(getConfigFile());
  }
}
