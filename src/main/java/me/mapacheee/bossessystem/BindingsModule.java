package me.mapacheee.bossessystem;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.thewinterframework.module.annotation.ModuleComponent;
import com.thewinterframework.plugin.WinterPlugin;
import com.thewinterframework.plugin.module.PluginModule;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

@ModuleComponent
public class BindingsModule implements PluginModule {

    @Override
    public boolean onLoad(WinterPlugin plugin) {
        return true;
    }

    @Override
    public boolean onEnable(WinterPlugin plugin) {
        return true;
    }

    @Override
    public boolean onDisable(WinterPlugin plugin) {
        return true;
    }

    @Provides
    @Singleton
    public PluginManager providePluginManager(Plugin plugin) {
        return plugin.getServer().getPluginManager();
    }
}
