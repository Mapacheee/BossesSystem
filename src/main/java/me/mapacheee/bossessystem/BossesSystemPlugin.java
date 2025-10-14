package me.mapacheee.bossessystem;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;

@WinterBootPlugin
public final class BossesSystemPlugin extends PaperWinterPlugin {

  public static BossesSystemPlugin get() {
    return BossesSystemPlugin.getPlugin(BossesSystemPlugin.class);
  }
}

