package me.mapacheee.bossessystem.shared.mythic;

import com.thewinterframework.service.annotation.Service;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.UUID;

@Service
public final class MythicMobsService {

  public UUID spawn(final String mythicId, final Location location) {
    try {
      final Class<?> mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
      final Method inst = mythicBukkit.getMethod("inst");
      final Object mythic = inst.invoke(null);
      final Method getAPIHelper = mythic.getClass().getMethod("getAPIHelper");
      final Object helper = getAPIHelper.invoke(mythic);
      try {
        final Method spawn = helper.getClass().getMethod("spawnMythicMob", String.class, org.bukkit.Location.class);
        final Object activeMob = spawn.invoke(helper, mythicId, location);
        //activeMob.getEntity().getUniqueId() or getBukkitEntity()
        try {
          final Method getEntity = activeMob.getClass().getMethod("getEntity");
          final Object abstractEntity = getEntity.invoke(activeMob);
          final Method getBukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity");
          final Entity entity = (Entity) getBukkitEntity.invoke(abstractEntity);
          return entity.getUniqueId();
        } catch (Throwable ignored) {
          try {
            final Method getBukkitEntity = activeMob.getClass().getMethod("getBukkitEntity");
            final Entity entity = (Entity) getBukkitEntity.invoke(activeMob);
            return entity.getUniqueId();
          } catch (Throwable ignored2) {
          }
        }
      } catch (NoSuchMethodException ex) {
      }
    } catch (Throwable t) {
    }
    final var w = location.getWorld();
    if (w == null) return null;
    final String cmd = String.format("mm mobs spawn %s 1 %s %.2f %.2f %.2f", mythicId, w.getName(), location.getX(), location.getY(), location.getZ());
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    return null;
  }

  public void despawn(final java.util.UUID uuid) {
    if (uuid == null) return;
    final var entity = org.bukkit.Bukkit.getEntity(uuid);
    if (entity != null) {
      entity.remove();
    }
  }
}
