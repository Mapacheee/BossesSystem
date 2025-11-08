package me.mapacheee.bossessystem.shared.config;

import com.thewinterframework.configurate.config.Configurate;
import com.thewinterframework.configurate.serializer.ConfigurateSerializer;
import com.thewinterframework.configurate.serializer.provider.ConfigurateSerializerProvider;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.List;
import java.util.Map;

@ConfigSerializable
@Configurate("config")
public record Config(
    General general,
    Spectators spectators,
    VictoryEffects victoryEffects,
    Exit exit,
    Integrations integrations,
    Gui gui,
    Map<String, Arena> arenas
) {

  @ConfigSerializable
  public record General(
      int rejoinTimeoutSeconds,
      boolean deathRejoinAsSpectator,
      Invitation invitation,
      int defaultSpawnDelaySeconds,
      int defaultTimeLimitSeconds,
      int defaultMaxPlayers,
      double defaultPrice
  ) {}

  @ConfigSerializable
  public record Invitation(
      int autoExpireSeconds,
      boolean requireAllAccepted
  ) {}

  @ConfigSerializable
  public record Spectators(
      boolean enabled,
      String gamemode,
      boolean restorePreviousGamemode
  ) {}

  @ConfigSerializable
  public record Exit(OnEnd onEnd) {
    @ConfigSerializable
    public record OnEnd(Phase victory, Phase timeout, Phase aborted) {
      @ConfigSerializable
      public record Phase(String dispatchAs, List<String> commands) {}
    }
  }

  @ConfigSerializable
  public record Integrations(boolean requireVault, boolean requireMythicmobs, PlaceholderApi placeholderapi) {
    @ConfigSerializable
    public record PlaceholderApi(boolean enabled) {}
  }

  @ConfigSerializable
  public record Gui(Invites invites) {
    @ConfigSerializable
    public record Invites(
        String title,
        int rows,
        boolean showOnlineOnly,
        boolean excludeLeader,
        int pageSize,
        Slots slots,
        Items items,
        Sounds sounds
    ) {
      @ConfigSerializable
      public record Slots(int playersStartSlot, int confirmSlot, int prevSlot, int nextSlot, List<Integer> fillerSlots) {}
      @ConfigSerializable
      public record Items(PlayerItem player, SimpleItem confirm, SimpleItem prev, SimpleItem next, SimpleItem filler) {
        @ConfigSerializable
        public record PlayerItem(String name, List<String> loreSelected, List<String> loreUnselected) {}
        @ConfigSerializable
        public record SimpleItem(String material, String name, List<String> lore) {}
      }
      @ConfigSerializable
      public record Sounds(String click, String confirm, String error) {}
    }
  }

  @ConfigSerializable
  public record Arena(
      String world,
      Spawn spawn,
      String mythicMobId,
      Double price,
      Integer timeLimitSeconds,
      Integer maxPlayers,
      Integer spawnDelaySeconds
  ) {
    @ConfigSerializable
    public record Spawn(double x, double y, double z, float yaw, float pitch) {}
  }

  @ConfigSerializable
  public record VictoryEffects(
      int delayBeforeTeleportSeconds,
      Title title,
      Sound sound
  ) {
    @ConfigSerializable
    public record Title(
        boolean enabled,
        String main,
        String subtitle,
        int fadeIn,
        int stay,
        int fadeOut
    ) {}

    @ConfigSerializable
    public record Sound(
        boolean enabled,
        String type,
        double volume,
        double pitch
    ) {}
  }

  @ConfigurateSerializerProvider
  public static class Serializers {
    @ConfigurateSerializer
    public TypeSerializerCollection item() {
      return TypeSerializerCollection.defaults();
    }
  }
}
