package me.mapacheee.bossessystem.bosses.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.component.ComponentUtils;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.bossessystem.shared.config.Config;
import me.mapacheee.bossessystem.shared.config.ConfigPersistenceService;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import me.mapacheee.bossessystem.shared.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
@Command("bossadmin|bossesadmin|bosses:admin|boss:admin")
@Permission("bossesystem.admin")
public final class BossAdminCommand {

  private final Container<Config> config;
  private final MessageService messages;
  private final Container<Messages> messagesContainer;
  private final ConfigPersistenceService persistence;
  private final ReloadServiceManager reloadServiceManager;

  @Inject
  public BossAdminCommand(final Container<Config> config,
                          final MessageService messages,
                          final Container<Messages> messagesContainer,
                          final ConfigPersistenceService persistence,
                          final ReloadServiceManager reloadServiceManager) {
    this.config = config;
    this.messages = messages;
    this.messagesContainer = messagesContainer;
    this.persistence = persistence;
    this.reloadServiceManager = reloadServiceManager;
  }

  @Command("reload")
  public void reload(final Source sender) {
    this.reloadServiceManager.reload();
    this.config.reload();
    this.messagesContainer.reload();
    this.messages.adminReloadOk(sender.source());
  }

  @Command("arena list")
  public void list(final Source sender) {
    final var arenas = this.config.get().arenas();
    final var keys = arenas == null || arenas.isEmpty() ? "-" : String.join(", ", arenas.keySet());
    sender.source().sendMessage(ComponentUtils.miniMessage("<gray>Arenas: <white>" + keys + "</white>"));
  }

  @Command("arena info <id>")
  public void info(final Source sender, final @Argument("id") String id) {
    final var arenas = this.config.get().arenas();
    if (arenas == null || !arenas.containsKey(id)) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var a = arenas.get(id);
    this.sendArenaInfo(sender, id, a);
  }

  private void sendArenaInfo(final Source sender, final String id, final Config.Arena a) {
    final var s = a.spawn();
    final String world = a.world() == null ? "-" : a.world();
    final String x = s == null ? "-" : String.format("%.2f", s.x());
    final String y = s == null ? "-" : String.format("%.2f", s.y());
    final String z = s == null ? "-" : String.format("%.2f", s.z());
    final String yaw = s == null ? "-" : String.format("%.1f", s.yaw());
    final String pitch = s == null ? "-" : String.format("%.1f", s.pitch());
    final String boss = a.bossId() == null ? "-" : a.bossId();
    final String delay = a.spawnDelaySeconds() == null ? "-" : String.valueOf(a.spawnDelaySeconds());
    this.messages.adminArenaInfo(sender.source(), id, world, x, y, z, yaw, pitch, boss, delay);
  }

  @Command("arena create <id> <world> <x> <y> <z> [yaw] [pitch]")
  public void create(final Source sender,
                     final @Argument("id") String id,
                     final @Argument("world") String world,
                     final @Argument("x") double x,
                     final @Argument("y") double y,
                     final @Argument("z") double z,
                     final @Argument("yaw") Float yaw,
                     final @Argument("pitch") Float pitch) {
    final var root = this.config.get();
    var arenas = root.arenas();
    if (arenas == null) {
      arenas = new java.util.HashMap<>();
    }
    if (arenas.containsKey(id)) {
      this.messages.errorArenaExists(sender.source(), id);
      return;
    }
    if (Bukkit.getWorld(world) == null) {
      this.messages.errorInvalidWorld(sender.source(), world);
      return;
    }
    final var spawn = new Config.Arena.Spawn(x, y, z, yaw == null ? 0f : yaw, pitch == null ? 0f : pitch);
    final var arenaObj = new Config.Arena(world, spawn, "", null);
    arenas.put(id, arenaObj);
    this.persistence.putArena(id, arenaObj);
    this.messages.adminArenaCreated(sender.source(), id);
    this.sendArenaInfo(sender, id, arenaObj);
  }

  @Command("arena createhere <id>")
  public void createHere(final Source sender, final @Argument("id") String id) {
    if (!(sender.source() instanceof Player p)) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    final var root = this.config.get();
    var arenas = root.arenas();
    if (arenas == null) {
      arenas = new java.util.HashMap<>();
    }
    if (arenas.containsKey(id)) {
      this.messages.errorArenaExists(sender.source(), id);
      return;
    }
    final var loc = p.getLocation();
    final var world = loc.getWorld();
    if (world == null) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    final var spawn = new Config.Arena.Spawn(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    final var arenaObj = new Config.Arena(world.getName(), spawn, "", null);
    arenas.put(id, arenaObj);
    this.persistence.putArena(id, arenaObj);
    this.messages.adminArenaCreated(sender.source(), id);
    this.sendArenaInfo(sender, id, arenaObj);
  }

  @Command("arena clone <fromId> <toId>")
  public void cloneArena(final Source sender,
                         final @Argument("fromId") String fromId,
                         final @Argument("toId") String toId) {
    if (fromId.equalsIgnoreCase(toId)) {
      this.messages.errorArenaExists(sender.source(), toId);
      return;
    }
    final var arenas = this.config.get().arenas();
    if (arenas == null || !arenas.containsKey(fromId)) {
      this.messages.errorInvalidArena(sender.source(), fromId);
      return;
    }
    if (arenas.containsKey(toId)) {
      this.messages.errorArenaExists(sender.source(), toId);
      return;
    }
    final var src = arenas.get(fromId);
    final var clone = new Config.Arena(src.world(), src.spawn(), src.bossId(), src.spawnDelaySeconds());
    arenas.put(toId, clone);
    this.persistence.putArena(toId, clone);
    this.messages.adminArenaCreated(sender.source(), toId);
    this.sendArenaInfo(sender, toId, clone);
  }

  @Command("arena setspawn <id> <x> <y> <z> [yaw] [pitch]")
  public void setSpawn(final Source sender,
                       final @Argument("id") String id,
                       final @Argument("x") double x,
                       final @Argument("y") double y,
                       final @Argument("z") double z,
                       final @Argument("yaw") Float yaw,
                       final @Argument("pitch") Float pitch) {
    final var root = this.config.get();
    final var arenas = root.arenas();
    if (arenas == null || !arenas.containsKey(id)) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var a = arenas.get(id);
    final float baseYaw = a.spawn() != null ? a.spawn().yaw() : 0f;
    final float basePitch = a.spawn() != null ? a.spawn().pitch() : 0f;
    final var spawn = new Config.Arena.Spawn(x, y, z, yaw == null ? baseYaw : yaw, pitch == null ? basePitch : pitch);
    final var updated = new Config.Arena(a.world(), spawn, a.bossId(), a.spawnDelaySeconds());
    arenas.put(id, updated);
    this.persistence.putArena(id, updated);
    this.messages.adminArenaUpdated(sender.source(), id);
    this.sendArenaInfo(sender, id, updated);
  }

  @Command("arena setspawnhere <id>")
  public void setSpawnHere(final Source sender, final @Argument("id") String id) {
    if (!(sender.source() instanceof Player p)) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    final var arenas = this.config.get().arenas();
    if (arenas == null || !arenas.containsKey(id)) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var a = arenas.get(id);
    final var loc = p.getLocation();
    final var world = loc.getWorld();
    if (world == null) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var spawn = new Config.Arena.Spawn(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    final var updated = new Config.Arena(world.getName(), spawn, a.bossId(), a.spawnDelaySeconds());
    arenas.put(id, updated);
    this.persistence.putArena(id, updated);
    this.messages.adminArenaUpdated(sender.source(), id);
    this.sendArenaInfo(sender, id, updated);
  }

  @Command("arena setboss <id> <bossId>")
  public void setBoss(final Source sender,
                      final @Argument("id") String id,
                      final @Argument("bossId") String bossId) {
    final var arenas = this.config.get().arenas();
    if (arenas == null || !arenas.containsKey(id)) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var a = arenas.get(id);
    final var updated = new Config.Arena(a.world(), a.spawn(), bossId, a.spawnDelaySeconds());
    arenas.put(id, updated);
    this.persistence.putArena(id, updated);
    this.messages.adminArenaUpdated(sender.source(), id);
    this.sendArenaInfo(sender, id, updated);
  }

  @Command("arena setdelay <id> <seconds>")
  public void setDelay(final Source sender,
                       final @Argument("id") String id,
                       final @Argument("seconds") int seconds) {
    final var arenas = this.config.get().arenas();
    if (arenas == null || !arenas.containsKey(id)) {
      this.messages.errorInvalidArena(sender.source(), id);
      return;
    }
    final var a = arenas.get(id);
    final var updated = new Config.Arena(a.world(), a.spawn(), a.bossId(), seconds);
    arenas.put(id, updated);
    this.persistence.putArena(id, updated);
    this.messages.adminArenaUpdated(sender.source(), id);
    this.sendArenaInfo(sender, id, updated);
  }

  @Command("arena delete <id>")
  public void delete(final Source sender, final @Argument("id") String id) {
    final var arenas = this.config.get().arenas();
    if (arenas != null) arenas.remove(id);
    this.persistence.removeArena(id);
    this.messages.adminArenaDeleted(sender.source(), id);
  }
}
