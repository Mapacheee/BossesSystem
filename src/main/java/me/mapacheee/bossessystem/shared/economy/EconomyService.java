package me.mapacheee.bossessystem.shared.economy;

import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

@Service
public final class EconomyService {

  private @Nullable Economy economy;

  @OnEnable
  void hook() {
    final var rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
    this.economy = rsp == null ? null : rsp.getProvider();
  }

  public boolean isAvailable() {
    return this.economy != null;
  }

  public boolean has(final OfflinePlayer player, final double amount) {
    return this.economy != null && this.economy.has(player, amount);
  }

  public boolean withdraw(final OfflinePlayer player, final double amount) {
    return this.economy != null && this.economy.withdrawPlayer(player, amount).transactionSuccess();
  }

  public void deposit(final OfflinePlayer player, final double amount) {
    if (this.economy != null) this.economy.depositPlayer(player, amount);
  }
}

