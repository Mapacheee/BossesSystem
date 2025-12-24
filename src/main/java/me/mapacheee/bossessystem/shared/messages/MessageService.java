package me.mapacheee.bossessystem.shared.messages;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

@Service
public final class MessageService {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  private final Container<Messages> messages;

  @Inject
  public MessageService(final Container<Messages> messages) {
    this.messages = messages;
  }

  private Component formatKey(final String section, final String key, final Map<String, String> tags) {
    final var msg = this.resolve(section, key);
    return this.formatRaw(msg, tags);
  }

  private void sendKey(final Audience audience, final String section, final String key, final Map<String, String> tags) {
    audience.sendMessage(this.formatKey(section, key, tags));
  }

  private Map<String, String> tags(final Object... pairs) {
    final var map = new java.util.HashMap<String, String>();
    for (int i = 0; i < pairs.length; i += 2) {
      final String key = (String) pairs[i];
      final Object value = pairs[i + 1];
      map.put(key, value == null ? "-" : value.toString());
    }
    return map;
  }

  public Component formatRaw(final String raw, final Map<String, String> tags) {
    var processed = raw;
    final var prefix = this.messages.get().prefix();
    processed = processed.replace("{prefix}", prefix == null ? "" : prefix);
    if (tags != null) {
      for (final var e : tags.entrySet()) {
        processed = processed.replace("{" + e.getKey() + "}", e.getValue());
      }
    }
    processed = processed.replaceAll("&#([0-9A-Fa-f]{6})", "<#$1>");
    return MINI_MESSAGE.deserialize(processed);
  }

  private String resolve(final String section, final String key) {
    final var m = this.messages.get();
    return switch (section) {
      case "errors" -> m.errors().getOrDefault(key, key);
      case "flow" -> m.flow().getOrDefault(key, key);
      case "end" -> m.end().getOrDefault(key, key);
      case "admin" -> m.admin().getOrDefault(key, key);
      case "ui" -> m.ui().getOrDefault(key, key);
      default -> key;
    };
  }

  public void errorNoPermission(final CommandSender sender) {
    this.sendKey(sender, "errors", "no-permission", tags());
  }

  public void errorPlayerOnly(final CommandSender sender) {
    this.sendKey(sender, "errors", "player-only", tags());
  }

  public void errorMissingIntegration(final CommandSender sender, final String dep) {
    this.sendKey(sender, "errors", "missing-integration", tags("dep", dep));
  }

  public void errorInvalidBoss(final CommandSender sender, final String boss) {
    this.sendKey(sender, "errors", "invalid-boss", tags("boss", boss));
  }

  public void errorInvalidArena(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "invalid-arena", tags("arena", arena));
  }

  public void errorArenaOccupied(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "arena-occupied", tags("arena", arena));
  }

  public void errorInsufficientBalance(final CommandSender sender, final String player, final String price) {
    this.sendKey(sender, "errors", "insufficient-balance", tags("player", player, "price", price));
  }

  public void errorMaxPlayers(final CommandSender sender, final int max) {
    this.sendKey(sender, "errors", "max-players", tags("max", String.valueOf(max)));
  }

  public void errorPendingInvites(final CommandSender sender, final String pending) {
    this.sendKey(sender, "errors", "pending-invites", tags("pending", pending));
  }

  public void errorNoInvitation(final CommandSender sender) {
    this.sendKey(sender, "errors", "no-invitation", tags());
  }

  public void errorRejoinUnavailable(final CommandSender sender) {
    this.sendKey(sender, "errors", "rejoin-unavailable", tags());
  }

  public void errorSpectatorDisabled(final CommandSender sender) {
    this.sendKey(sender, "errors", "spectator-disabled", tags());
  }

  public void errorCommandBlockedInCombat(final CommandSender sender) {
    this.sendKey(sender, "errors", "command-blocked-in-combat", tags());
  }

  public void errorInvalidWorld(final CommandSender sender, final String world) {
    this.sendKey(sender, "errors", "invalid-world", tags("world", world));
  }

  public void errorArenaExists(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "arena-exists", tags("arena", arena));
  }

  public void errorArenaNoBoss(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "arena-no-boss", tags("arena", arena));
  }

  public void flowArenasList(final CommandSender sender, final String list) {
    this.sendKey(sender, "flow", "arenas-list", tags("list", list));
  }

  public void flowArenaSelected(final CommandSender sender, final String arena, final String boss) {
    this.sendKey(sender, "flow", "arena-selected", tags("arena", arena, "boss", boss));
  }

  public void flowInvitesSent(final Player leader, final String targets, final int expireSeconds) {
    this.sendKey(leader, "flow", "invites-sent", tags("targets", targets, "expire", String.valueOf(expireSeconds)));
  }

  public void flowInviteReceived(final Player invitee, final String leader, final String boss, final String arena, final int expireSeconds) {
    this.sendKey(invitee, "flow", "invite-received", tags(
        "leader", leader,
        "boss", boss,
        "arena", arena,
        "expire", String.valueOf(expireSeconds)
    ));
  }

  public void flowInviteAccepted(final Player leader, final String player) {
    this.sendKey(leader, "flow", "invite-accepted", tags("player", player));
  }

  public void flowInviteAlreadyAccepted(final CommandSender sender) {
    this.sendKey(sender, "flow", "invite-already-accepted", tags());
  }

  public void flowInviteRejected(final Player leader, final String player) {
    this.sendKey(leader, "flow", "invite-rejected", tags("player", player));
  }

  public void flowInviteExpired(final CommandSender sender) {
    this.sendKey(sender, "flow", "invite-expired", tags());
  }

  public void flowAllAccepted(final CommandSender sender) {
    this.sendKey(sender, "flow", "all-accepted", tags());
  }

  public void flowChargeConfirmation(final CommandSender sender, final String price) {
    this.sendKey(sender, "flow", "charge-confirmation", tags("price", price));
  }

  public void flowTeleporting(final Player player) {
    this.sendKey(player, "flow", "teleporting", tags());
  }

  public void flowBossSpawnsIn(final Player player, final int seconds) {
    this.sendKey(player, "flow", "boss-spawns-in", tags("seconds", String.valueOf(seconds)));
  }

  public void flowFightStarted(final Player player) {
    this.sendKey(player, "flow", "fight-started", tags());
  }

  public void flowRejoinAvailable(final Player player, final int seconds) {
    this.sendKey(player, "flow", "rejoin-available", tags("seconds", String.valueOf(seconds)));
  }

  public void flowRejoinSuccess(final Player player) {
    this.sendKey(player, "flow", "rejoin-success", tags());
  }

  public void flowPlayerDied(final Player player) {
    this.sendKey(player, "flow", "player-died", tags());
  }

  public void flowRejoinSpectator(final Player player) {
    this.sendKey(player, "flow", "rejoin-spectator", tags());
  }

  public void flowCancelledInsufficientBalance(final CommandSender sender, final String player, final String price) {
    this.sendKey(sender, "flow", "cancelled-insufficient-balance", tags("player", player, "price", price));
  }

  public void endCancelled(final CommandSender sender) {
    this.sendKey(sender, "end", "cancelled", tags());
  }

  public void adminReloadOk(final CommandSender sender) {
    this.sendKey(sender, "admin", "reload-ok", tags());
  }

  public void adminArenaCreated(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-created", tags("arena", arena));
  }

  public void adminArenaUpdated(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-updated", tags("arena", arena));
  }

  public void adminArenaDeleted(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-deleted", tags("arena", arena));
  }

  public void adminArenaInfo(final CommandSender sender,
                             final String arena,
                             final String world,
                             final String x,
                             final String y,
                             final String z,
                             final String yaw,
                             final String pitch,
                             final String boss,
                             final String delay) {
    this.sendKey(sender, "admin", "arena-info", tags(
        "arena", arena,
        "world", world,
        "x", x,
        "y", y,
        "z", z,
        "yaw", yaw,
        "pitch", pitch,
        "boss", boss,
        "delay", delay
    ));
  }

  public void sendTitle(final Player player, final String main, final String subtitle,
                       final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
    final var mainComponent = this.formatRaw(main, null);
    final var subtitleComponent = this.formatRaw(subtitle, null);

    final var times = net.kyori.adventure.title.Title.Times.times(
        java.time.Duration.ofMillis(fadeInTicks * 50L),
        java.time.Duration.ofMillis(stayTicks * 50L),
        java.time.Duration.ofMillis(fadeOutTicks * 50L)
    );

    final var title = net.kyori.adventure.title.Title.title(mainComponent, subtitleComponent, times);
    player.showTitle(title);
  }

  public void playSound(final Player player, final String soundName, final float volume, final float pitch) {
    try {
      final var sound = Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase()));
      if (sound != null) {
        player.playSound(player.getLocation(), sound, volume, pitch);
      }
    } catch (IllegalArgumentException ignored) {
    }
  }

  public void sendVictoryTitle(final Player player, final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
    final var mainText = this.resolve("end", "victory-title-main");
    final var subtitleText = this.resolve("end", "victory-title-subtitle");
    this.sendTitle(player, mainText, subtitleText, fadeInTicks, stayTicks, fadeOutTicks);
  }

  public void sendDefeatTitle(final Player player, final int fadeInTicks, final int stayTicks, final int fadeOutTicks) {
    final var mainText = this.resolve("end", "defeat-title-main");
    final var subtitleText = this.resolve("end", "defeat-title-subtitle");
    this.sendTitle(player, mainText, subtitleText, fadeInTicks, stayTicks, fadeOutTicks);
  }

  public void endDefeat(final CommandSender sender) {
    this.sendKey(sender, "end", "defeat", tags());
  }
}
