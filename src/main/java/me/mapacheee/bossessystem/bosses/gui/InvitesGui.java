package me.mapacheee.bossessystem.bosses.gui;

import com.google.inject.Inject;
import com.thewinterframework.component.ComponentUtils;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.annotation.Service;
import me.mapacheee.bossessystem.bosses.entity.ArenaRegistryService;
import me.mapacheee.bossessystem.bosses.entity.BossRegistryService;
import me.mapacheee.bossessystem.bosses.entity.PartyService;
import me.mapacheee.bossessystem.shared.config.Config;
import me.mapacheee.bossessystem.shared.messages.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class InvitesGui {

  private final Container<Config> config;
  private final ArenaRegistryService arenas;
  private final BossRegistryService bosses;
  private final PartyService parties;
  private final MessageService messages;

  private final Map<UUID, State> stateByLeader = new HashMap<>();

  @Inject
  public InvitesGui(final Container<Config> config,
                    final ArenaRegistryService arenas,
                    final BossRegistryService bosses,
                    final PartyService parties,
                    final MessageService messages) {
    this.config = config;
    this.arenas = arenas;
    this.bosses = bosses;
    this.parties = parties;
    this.messages = messages;
  }

  private record State(String arenaId, Set<UUID> selected, int page) {}

  public static final class Holder implements InventoryHolder {
    final UUID leader;
    final String arenaId;

    Holder(final UUID leader, final String arenaId) { this.leader = leader; this.arenaId = arenaId; }

    @Override public Inventory getInventory() { return null; }
  }

  public void open(final Player leader, final String arenaId) {
    final var cfg = this.config.get().gui().invites();
    final var rows = Math.max(1, Math.min(cfg.rows(), 6));
    final var size = rows * 9;
    final var title = ComponentUtils.miniMessage(cfg.title());
    this.stateByLeader.put(leader.getUniqueId(), new State(arenaId, new HashSet<>(), 0));
    final var inv = Bukkit.createInventory(new Holder(leader.getUniqueId(), arenaId), size, title);
    this.render(inv, leader.getUniqueId());
    leader.openInventory(inv);
  }

  public void handleClick(final Player clicker, final Inventory inv, final int slot) {
    if (!(inv.getHolder() instanceof Holder holder)) return;
    final var state = this.stateByLeader.get(holder.leader);
    if (state == null) return;
    final var cfg = this.config.get().gui().invites();
    if (slot == cfg.slots().confirmSlot()) {
      this.confirm(clicker, holder.arenaId, state.selected());
      clicker.closeInventory();
      return;
    }
    if (slot == cfg.slots().prevSlot()) {
      this.stateByLeader.put(holder.leader, new State(state.arenaId(), state.selected(), Math.max(0, state.page() - 1)));
      this.render(inv, holder.leader);
      return;
    }
    if (slot == cfg.slots().nextSlot()) {
      this.stateByLeader.put(holder.leader, new State(state.arenaId(), state.selected(), state.page() + 1));
      this.render(inv, holder.leader);
      return;
    }
    final int start = cfg.slots().playersStartSlot();
    final var pageSize = cfg.pageSize();
    if (slot < start) return;
    final var index = slot - start + (state.page() * pageSize);
    final var candidates = this.buildCandidates(holder.leader, state.arenaId());
    if (index < 0 || index >= candidates.size()) return;
    final UUID target = candidates.get(index).getUniqueId();
    if (state.selected().contains(target)) {
      state.selected().remove(target);
    } else {
      final var arena = this.arenas.getArena(state.arenaId());
      if (arena != null) {
        int max = this.bosses.getMaxPlayers(arena);
        if (1 + state.selected().size() >= max) {
          this.messages.errorMaxPlayers(clicker, max);
          return;
        }
      }
      state.selected().add(target);
    }
    this.render(inv, holder.leader);
  }

  private void confirm(final Player leader, final String arenaId, final Set<UUID> selected) {
    final var players = selected.stream()
        .map(Bukkit::getPlayer)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    final var inv = this.parties.startInvites(leader, arenaId, players);

    if (!players.isEmpty()) {
      this.messages.flowInvitesSent(leader,
          players.stream().filter(Objects::nonNull).map(Player::getName).collect(Collectors.joining(", ")),
          this.config.get().general().invitation().autoExpireSeconds());

      final var arena = this.arenas.getArena(arenaId);
      final var bossId = arena != null ? arena.mythicMobId() : "";
      for (final var p : players) {
        this.messages.flowInviteReceived(p, leader.getName(), bossId, arenaId, this.config.get().general().invitation().autoExpireSeconds());
      }
    }
  }

  private List<Player> buildCandidates(final UUID leader, final String arenaId) {
    final var cfg = this.config.get().gui().invites();
    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> !p.getUniqueId().equals(leader))
        .collect(Collectors.toList());
  }

  private void render(final Inventory inv, final UUID leader) {
    if (!(inv.getHolder() instanceof Holder holder)) return;
    final var cfg = this.config.get().gui().invites();
    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);
    final var state = this.stateByLeader.get(leader);
    if (state == null) return;

    final var start = cfg.slots().playersStartSlot();
    final var pageSize = cfg.pageSize();
    final var all = this.buildCandidates(leader, state.arenaId());
    final var from = state.page() * pageSize;
    final var to = Math.min(from + pageSize, all.size());
    final var page = all.subList(from, to);

    int slot = start;
    for (final var p : page) {
      final boolean selected = state.selected().contains(p.getUniqueId());
      inv.setItem(slot++, this.playerHead(p, selected));
    }

    inv.setItem(cfg.slots().confirmSlot(), this.confirmItem(state.selected().size()));
    inv.setItem(cfg.slots().prevSlot(), simpleItem(cfg.items().prev().material(), cfg.items().prev().name(), cfg.items().prev().lore()));
    inv.setItem(cfg.slots().nextSlot(), simpleItem(cfg.items().next().material(), cfg.items().next().name(), cfg.items().next().lore()));

    for (final int s : cfg.slots().fillerSlots()) {
      if (inv.getItem(s) == null) inv.setItem(s, simpleItem(cfg.items().filler().material(), cfg.items().filler().name(), cfg.items().filler().lore()));
    }
  }

  private ItemStack playerHead(final OfflinePlayer player, final boolean selected) {
    final var cfg = this.config.get().gui().invites();
    final var head = new ItemStack(Material.PLAYER_HEAD);
    final var meta = (SkullMeta) head.getItemMeta();
    meta.setOwningPlayer(player);
    meta.displayName(ComponentUtils.miniMessage(cfg.items().player().name().replace("{player}", player.getName() == null ? "Player" : player.getName())));
    final var loreList = selected ? cfg.items().player().loreSelected() : cfg.items().player().loreUnselected();
    if (loreList != null && !loreList.isEmpty()) {
      final var loreComp = loreList.stream().map(ComponentUtils::miniMessage).toList();
      meta.lore(loreComp);
    }
    head.setItemMeta(meta);
    return head;
  }

  private ItemStack confirmItem(final int selectedCount) {
    final var cfg = this.config.get().gui().invites();
    final var item = new ItemStack(Objects.requireNonNull(Material.matchMaterial(cfg.items().confirm().material())));
    final var meta = item.getItemMeta();
    meta.displayName(ComponentUtils.miniMessage(cfg.items().confirm().name()));
    final var lore = cfg.items().confirm().lore().stream()
        .map(s -> s.replace("{selectedCount}", String.valueOf(selectedCount)))
        .map(ComponentUtils::miniMessage).toList();
    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  private static ItemStack simpleItem(final String matName, final String name, final List<String> lore) {
    final var mat = Material.matchMaterial(matName);
    final var item = new ItemStack(mat == null ? Material.PAPER : mat);
    final var meta = item.getItemMeta();
    if (name != null && !name.isEmpty()) meta.displayName(ComponentUtils.miniMessage(name));
    if (lore != null && !lore.isEmpty()) meta.lore(lore.stream().map(ComponentUtils::miniMessage).toList());
    item.setItemMeta(meta);
    return item;
  }
}
