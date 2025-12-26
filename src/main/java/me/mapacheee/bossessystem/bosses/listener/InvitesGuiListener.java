package me.mapacheee.bossessystem.bosses.listener;

import com.google.inject.Inject;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.bossessystem.bosses.gui.ArenasGui;
import me.mapacheee.bossessystem.bosses.gui.InvitesGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

@ListenerComponent
public final class InvitesGuiListener implements Listener {

  private final InvitesGui invitesGui;
  private final ArenasGui arenasGui;

  @Inject
  public InvitesGuiListener(final InvitesGui invitesGui, final ArenasGui arenasGui) {
    this.invitesGui = invitesGui;
    this.arenasGui = arenasGui;
  }

  @EventHandler
  public void onClick(final InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    final var view = event.getView();
    final var top = view.getTopInventory();

    if (top.getHolder() instanceof InvitesGui.Holder) {
      event.setCancelled(true);
      final int rawSlot = event.getRawSlot();
      if (rawSlot < 0 || rawSlot >= top.getSize()) return;
      this.invitesGui.handleClick(player, top, rawSlot);
      return;
    }

    if (top.getHolder() instanceof ArenasGui.Holder) {
      event.setCancelled(true);
      final int rawSlot = event.getRawSlot();
      if (rawSlot < 0 || rawSlot >= top.getSize()) return;
      this.arenasGui.handleClick(player, top, rawSlot);
    }
  }
}

