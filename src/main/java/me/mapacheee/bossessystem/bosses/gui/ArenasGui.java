package me.mapacheee.bossessystem.bosses.gui;

import com.google.inject.Inject;
import com.thewinterframework.component.ComponentUtils;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.BossRegistryService;
import me.mapacheee.bossessystem.shared.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class ArenasGui {

  private final Container<Config> config;
  private final ArenaRegistryService arenas;
  private final BossRegistryService bosses;
  private final InvitesGui invitesGui;

  @Inject
  public ArenasGui(final Container<Config> config,
                   final ArenaRegistryService arenas,
                   final BossRegistryService bosses,
                   final InvitesGui invitesGui) {
    this.config = config;
    this.arenas = arenas;
    this.bosses = bosses;
    this.invitesGui = invitesGui;
  }

  public static final class Holder implements InventoryHolder {
    final int page;

    Holder(final int page) {
      this.page = page;
    }

    @Override
    public Inventory getInventory() {
      return null;
    }
  }

  public void open(final Player player) {
    this.open(player, 0);
  }

  public void open(final Player player, final int page) {
    final var cfg = this.config.get().gui().arenas();
    final var rows = Math.max(1, Math.min(cfg.rows(), 6));
    final var size = rows * 9;
    final var title = ComponentUtils.miniMessage(cfg.title());
    final var inv = Bukkit.createInventory(new Holder(page), size, title);
    this.render(inv, page);
    player.openInventory(inv);
  }

  public void handleClick(final Player clicker, final Inventory inv, final int slot) {
    if (!(inv.getHolder() instanceof Holder holder)) return;
    final var cfg = this.config.get().gui().arenas();

    if (slot == cfg.slots().prevSlot() && holder.page > 0) {
      this.open(clicker, holder.page - 1);
      return;
    }

    final var allArenas = new ArrayList<>(this.arenas.arenaIds());
    final var totalPages = (int) Math.ceil((double) allArenas.size() / cfg.pageSize());
    if (slot == cfg.slots().nextSlot() && holder.page < totalPages - 1) {
      this.open(clicker, holder.page + 1);
      return;
    }

    final int start = cfg.slots().arenasStartSlot();
    if (slot < start) return;
    final var index = slot - start + (holder.page * cfg.pageSize());
    if (index < 0 || index >= allArenas.size()) return;

    final var arenaId = allArenas.get(index);
    final var arena = this.arenas.getArena(arenaId);
    if (arena == null) return;

    if (this.arenas.isOccupied(arenaId)) {
      return;
    }

    clicker.closeInventory();
    this.invitesGui.open(clicker, arenaId);
  }

  private void render(final Inventory inv, final int page) {
    final var cfg = this.config.get().gui().arenas();
    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);

    final var allArenas = new ArrayList<>(this.arenas.arenaIds());
    final var pageSize = cfg.pageSize();
    final var from = page * pageSize;
    final var to = Math.min(from + pageSize, allArenas.size());

    final int start = cfg.slots().arenasStartSlot();
    for (int i = from; i < to; i++) {
      final var arenaId = allArenas.get(i);
      final var arena = this.arenas.getArena(arenaId);
      if (arena == null) continue;

      final var isOccupied = this.arenas.isOccupied(arenaId);
      final var item = this.createArenaItem(arenaId, arena, isOccupied);
      inv.setItem(start + (i - from), item);
    }

    final var fillerItem = this.createItem(
        Material.valueOf(cfg.items().filler().material()),
        cfg.items().filler().name(),
        cfg.items().filler().lore(),
        Collections.emptyMap()
    );
    for (final var slot : cfg.slots().fillerSlots()) {
      inv.setItem(slot, fillerItem);
    }

    if (page > 0) {
      final var prevItem = this.createItem(
          Material.valueOf(cfg.items().prev().material()),
          cfg.items().prev().name(),
          cfg.items().prev().lore(),
          Collections.emptyMap()
      );
      inv.setItem(cfg.slots().prevSlot(), prevItem);
    }

    final var totalPages = (int) Math.ceil((double) allArenas.size() / pageSize);
    if (page < totalPages - 1) {
      final var nextItem = this.createItem(
          Material.valueOf(cfg.items().next().material()),
          cfg.items().next().name(),
          cfg.items().next().lore(),
          Collections.emptyMap()
      );
      inv.setItem(cfg.slots().nextSlot(), nextItem);
    }
  }

  private ItemStack createArenaItem(final String arenaId, final Config.Arena arena, final boolean isOccupied) {
    final var cfg = this.config.get().gui().arenas();
    final var maxPlayers = this.bosses.getMaxPlayers(arena);
    final var price = this.bosses.getPrice(arena);
    final var mythicMobId = arena.mythicMobId() != null ? arena.mythicMobId() : "N/A";

    final var placeholders = Map.of(
        "arena", arenaId,
        "status", isOccupied ? cfg.items().arena().statusOccupied() : cfg.items().arena().statusAvailable(),
        "maxPlayers", String.valueOf(maxPlayers),
        "price", String.format("%.2f", price),
        "boss", mythicMobId
    );

    final var material = isOccupied
        ? Material.valueOf(cfg.items().arena().materialOccupied())
        : Material.valueOf(cfg.items().arena().materialAvailable());

    final var lore = isOccupied
        ? cfg.items().arena().loreOccupied()
        : cfg.items().arena().loreAvailable();

    return this.createItem(material, cfg.items().arena().name(), lore, placeholders);
  }

  private ItemStack createItem(final Material material, final String name, final List<String> lore, final Map<String, String> placeholders) {
    final var item = new ItemStack(material);
    final var meta = item.getItemMeta();
    if (meta == null) return item;

    var finalName = name;
    var finalLore = new ArrayList<>(lore);

    for (final var entry : placeholders.entrySet()) {
      finalName = finalName.replace("{" + entry.getKey() + "}", entry.getValue());
      finalLore = finalLore.stream()
          .map(line -> line.replace("{" + entry.getKey() + "}", entry.getValue()))
          .collect(Collectors.toCollection(ArrayList::new));
    }

    meta.displayName(ComponentUtils.miniMessage(finalName));
    meta.lore(finalLore.stream()
        .map(ComponentUtils::miniMessage)
        .collect(Collectors.toList()));

    item.setItemMeta(meta);
    return item;
  }
}

