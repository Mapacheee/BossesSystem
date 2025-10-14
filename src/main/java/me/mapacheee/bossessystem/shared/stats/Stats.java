package me.mapacheee.bossessystem.shared.stats;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
@Configurate("stats")
public record Stats(Map<String, BossStats> bosses) {

  @ConfigSerializable
  public record BossStats(int defeats, long bestTimeMillis, String bestBy) {}
}

