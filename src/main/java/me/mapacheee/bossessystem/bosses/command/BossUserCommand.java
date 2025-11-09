package me.mapacheee.bossessystem.bosses.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.component.ComponentUtils;
import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.PartyService;
import me.mapacheee.bossessystem.bosses.entity.SessionService;
import me.mapacheee.bossessystem.bosses.gui.InvitesGui;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

import java.util.stream.Collectors;

@CommandComponent
public final class BossUserCommand {

  private final InvitesGui invitesGui;
  private final ArenaRegistryService arenas;
  private final PartyService parties;
  private final MessageService messages;
  private final SessionService sessions;

  @Inject
  public BossUserCommand(final InvitesGui invitesGui, final ArenaRegistryService arenas, final PartyService parties, final MessageService messages, final SessionService sessions) {
    this.invitesGui = invitesGui;
    this.arenas = arenas;
    this.parties = parties;
    this.messages = messages;
    this.sessions = sessions;
  }

  @Command("boss|bosses arenas")
  @Permission("bossesystem.use.arenas")
  public void arenas(final Source sender) {
    final var ids = this.arenas.arenaIds();
    final var list = ids.isEmpty() ? "-" : ids.stream()
        .map(id -> id + "(" + (this.arenas.isOccupied(id) ? "ocupada" : "libre") + ")")
        .collect(Collectors.joining(", "));
    this.messages.flowArenasList(sender.source(), list);
  }

  @Command("boss|bosses select <arenaId>")
  @Permission("bossesystem.use.select")
  public void select(final Source sender, final @Argument("arenaId") String arenaId) {
    if (!(sender.source() instanceof Player p)) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    final var arena = this.arenas.getArena(arenaId);
    if (arena == null) {
      this.messages.errorInvalidArena(sender.source(), arenaId);
      return;
    }
    if (this.arenas.isOccupied(arenaId)) {
      this.messages.errorArenaOccupied(sender.source(), arenaId);
      return;
    }
    this.messages.flowArenaSelected(sender.source(), arenaId, arena.mythicMobId());
    this.invitesGui.open(p, arenaId);
  }

  @Command("boss|bosses accept")
  @Permission("bossesystem.use.accept")
  public void accept(final Source sender) {
    if (!(sender.source() instanceof Player p)) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    this.parties.accept(p);
  }

  @Command("boss|bosses reject")
  @Permission("bossesystem.use.reject")
  public void reject(final Source sender) {
    if (!(sender.source() instanceof Player p)) {
      this.messages.errorPlayerOnly(sender.source());
      return;
    }
    this.parties.reject(p);
  }

  @Command("boss|bosses leave")
  @Permission("bossesystem.use.leave")
  public void leave(final Source sender) {
    if (sender.source() instanceof Player p) {
      this.sessions.leaveCommand(p);
    }
  }

  @Command("boss|bosses rejoin")
  @Permission("bossesystem.use.rejoin")
  public void rejoin(final Source sender) {
    if (sender.source() instanceof Player p) {
      this.sessions.rejoinCommand(p);
    }
  }

  @Command("boss|bosses spectate <arenaId>")
  @Permission("bossesystem.use.spectate")
  public void spectate(final Source sender, final @Argument("arenaId") String arenaId) {
    if (sender.source() instanceof Player p) {
      this.sessions.spectateCommand(p, arenaId);
    }
  }

  @Command("boss|bosses stats [bossId]")
  @Permission("bossesystem.use.stats")
  public void stats(final Source sender, final @Default("") @Argument("bossId") String bossId) {
    if (bossId == null || bossId.isBlank()) {
      sender.source().sendMessage(ComponentUtils.miniMessage("<gray>Mostrando estadísticas generales..."));
    } else {
      sender.source().sendMessage(ComponentUtils.miniMessage("<gray>Mostrando estadísticas de <white>" + bossId + "</white>..."));
    }
  }
}
