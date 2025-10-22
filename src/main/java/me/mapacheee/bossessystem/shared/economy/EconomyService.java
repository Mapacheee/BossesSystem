package me.mapacheee.bossessystem.shared.economy;

import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

@Service
public final class EconomyService {

  private @Nullable Object economy;

  @OnEnable
  void hook() {
    try {
      final var economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
      final var rsp = Bukkit.getServer().getServicesManager().getRegistration(economyClass);
      this.economy = rsp == null ? null : rsp.getProvider();
    } catch (ClassNotFoundException e) {
      this.economy = null;
    }
  }

  public boolean isAvailable() {
    return this.economy != null;
  }

  public boolean has(final OfflinePlayer player, final double amount) {
    if (this.economy == null) return false;
    try {
      final var hasMethod = this.economy.getClass().getMethod("has", OfflinePlayer.class, double.class);
      return (boolean) hasMethod.invoke(this.economy, player, amount);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean withdraw(final OfflinePlayer player, final double amount) {
    if (this.economy == null) return false;
    try {
      final var withdrawMethod = this.economy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
      final var result = withdrawMethod.invoke(this.economy, player, amount);
      final var transactionSuccessMethod = result.getClass().getMethod("transactionSuccess");
      return (boolean) transactionSuccessMethod.invoke(result);
    } catch (Exception e) {
      return false;
    }
  }

  public void deposit(final OfflinePlayer player, final double amount) {
    if (this.economy == null) return;
    try {
      final var depositMethod = this.economy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
      depositMethod.invoke(this.economy, player, amount);
    } catch (Exception ignored) {}
  }
}
