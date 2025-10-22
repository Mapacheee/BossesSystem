package me.mapacheee.bossessystem;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;
import org.bukkit.Bukkit;

@WinterBootPlugin
public final class BossesSystemPlugin extends PaperWinterPlugin {

  public static BossesSystemPlugin get() {
    return BossesSystemPlugin.getPlugin(BossesSystemPlugin.class);
  }

  /*@Override
  public void onPluginEnable() {
    super.onPluginEnable();
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      try {
        final var expansionClass = Class.forName("me.mapacheee.bossessystem.shared.placeholder.BossesExpansion");
        final var constructor = expansionClass.getConstructor(
            Class.forName("me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService"),
            Class.forName("me.mapacheee.bossessystem.bosses.entity.SessionService"),
            Class.forName("me.mapacheee.bossessystem.shared.stats.StatsService")
        );
        final var expansion = constructor.newInstance(
            getInjector().getInstance(Class.forName("me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService")),
            getInjector().getInstance(Class.forName("me.mapacheee.bossessystem.bosses.entity.SessionService")),
            getInjector().getInstance(Class.forName("me.mapacheee.bossessystem.shared.stats.StatsService"))
        );
        final var registerMethod = expansionClass.getMethod("register", Class.forName("org.bukkit.plugin.Plugin"));
        registerMethod.invoke(expansion, this);
      } catch (Throwable t) {
        getLogger().warning("Failed to register PlaceholderAPI expansion: " + t.getMessage());
      }
    }
  }*/
}
