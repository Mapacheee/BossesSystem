package me.mapacheee.bossessystem.shared.messages;

import com.google.inject.Inject;
import com.thewinterframework.component.ComponentUtils;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

@Service
public final class MessageService {

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

  public Component formatRaw(final String raw, final Map<String, String> tags) {
    var processed = raw;
    final var prefix = this.messages.get().prefix();
    processed = processed.replace("{prefix}", prefix == null ? "" : prefix);
    if (tags != null) {
      for (final var e : tags.entrySet()) {
        processed = processed.replace("{" + e.getKey() + "}", e.getValue());
      }
    }
    return ComponentUtils.miniMessage(processed);
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
    this.sendKey(sender, "errors", "no-permission", Map.of());
  }

  public void errorPlayerOnly(final CommandSender sender) {
    this.sendKey(sender, "errors", "player-only", Map.of());
  }

  public void errorMissingIntegration(final CommandSender sender, final String dep) {
    this.sendKey(sender, "errors", "missing-integration", Map.of("dep", dep));
  }

  public void errorInvalidBoss(final CommandSender sender, final String boss) {
    this.sendKey(sender, "errors", "invalid-boss", Map.of("boss", boss));
  }

  public void errorInvalidArena(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "invalid-arena", Map.of("arena", arena));
  }

  public void errorArenaOccupied(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "arena-occupied", Map.of("arena", arena));
  }

  public void errorInsufficientBalance(final CommandSender sender, final String player, final String price) {
    this.sendKey(sender, "errors", "insufficient-balance", Map.of("player", player, "price", price));
  }

  public void errorMaxPlayers(final CommandSender sender, final int max) {
    this.sendKey(sender, "errors", "max-players", Map.of("max", String.valueOf(max)));
  }

  public void errorPendingInvites(final CommandSender sender, final String pending) {
    this.sendKey(sender, "errors", "pending-invites", Map.of("pending", pending));
  }

  public void errorNoInvitation(final CommandSender sender) {
    this.sendKey(sender, "errors", "no-invitation", Map.of());
  }

  public void errorRejoinUnavailable(final CommandSender sender) {
    this.sendKey(sender, "errors", "rejoin-unavailable", Map.of());
  }

  public void errorSpectatorDisabled(final CommandSender sender) {
    this.sendKey(sender, "errors", "spectator-disabled", Map.of());
  }

  public void errorInvalidWorld(final CommandSender sender, final String world) {
    this.sendKey(sender, "errors", "invalid-world", Map.of("world", world));
  }

  public void errorArenaExists(final CommandSender sender, final String arena) {
    this.sendKey(sender, "errors", "arena-exists", Map.of("arena", arena));
  }

  public void flowArenasList(final CommandSender sender, final String list) {
    this.sendKey(sender, "flow", "arenas-list", Map.of("list", list));
  }

  public void flowArenaSelected(final CommandSender sender, final String arena, final String boss) {
    this.sendKey(sender, "flow", "arena-selected", Map.of("arena", arena, "boss", boss));
  }

  public void flowInvitesSent(final Player leader, final String targets, final int expireSeconds) {
    this.sendKey(leader, "flow", "invites-sent", Map.of("targets", targets, "expire", String.valueOf(expireSeconds)));
  }

  public void flowInviteReceived(final Player invitee, final String leader, final String boss, final String arena, final int expireSeconds) {
    this.sendKey(invitee, "flow", "invite-received", Map.of(
        "leader", leader,
        "boss", boss,
        "arena", arena,
        "expire", String.valueOf(expireSeconds)
    ));
  }

  public void flowInviteAccepted(final Player leader, final String player) {
    this.sendKey(leader, "flow", "invite-accepted", Map.of("player", player));
  }

  public void flowInviteRejected(final Player leader, final String player) {
    this.sendKey(leader, "flow", "invite-rejected", Map.of("player", player));
  }

  public void flowInviteExpired(final CommandSender sender) {
    this.sendKey(sender, "flow", "invite-expired", Map.of());
  }

  public void flowAllAccepted(final CommandSender sender) {
    this.sendKey(sender, "flow", "all-accepted", Map.of());
  }

  public void flowChargeConfirmation(final CommandSender sender, final String price) {
    this.sendKey(sender, "flow", "charge-confirmation", Map.of("price", price));
  }

  public void flowTeleporting(final Player player) {
    this.sendKey(player, "flow", "teleporting", Map.of());
  }

  public void flowBossSpawnsIn(final Player player, final int seconds) {
    this.sendKey(player, "flow", "boss-spawns-in", Map.of("seconds", String.valueOf(seconds)));
  }

  public void flowFightStarted(final Player player) {
    this.sendKey(player, "flow", "fight-started", Map.of());
  }

  public void flowRejoinAvailable(final Player player, final int seconds) {
    this.sendKey(player, "flow", "rejoin-available", Map.of("seconds", String.valueOf(seconds)));
  }

  public void flowRejoinSuccess(final Player player) {
    this.sendKey(player, "flow", "rejoin-success", Map.of());
  }

  public void flowRejoinSpectator(final Player player) {
    this.sendKey(player, "flow", "rejoin-spectator", Map.of());
  }

  public void endCancelled(final CommandSender sender) {
    this.sendKey(sender, "end", "cancelled", Map.of());
  }

  public void adminReloadOk(final CommandSender sender) {
    this.sendKey(sender, "admin", "reload-ok", Map.of());
  }

  public void adminArenaCreated(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-created", Map.of("arena", arena));
  }

  public void adminArenaUpdated(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-updated", Map.of("arena", arena));
  }

  public void adminArenaDeleted(final CommandSender sender, final String arena) {
    this.sendKey(sender, "admin", "arena-deleted", Map.of("arena", arena));
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
    this.sendKey(sender, "admin", "arena-info", Map.of(
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
}
