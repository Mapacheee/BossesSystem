package me.mapacheee.bossessystem.shared.economy;

import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public final class EconomyService {

  private static final Logger logger = LoggerFactory.getLogger(EconomyService.class);

  private @Nullable Economy economy;

  @OnEnable
  void hook() {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
      logger.warn("Vault not found, economy features disabled");
      this.economy = null;
      return;
    }

    logger.info("Vault plugin found, attempting to hook into economy...");

    final var servicesManager = Bukkit.getServer().getServicesManager();
    final var registration = servicesManager.getRegistration(Economy.class);

    if (registration == null) {
      logger.warn("Vault Economy registration not found. Make sure you have an economy plugin installed (like EssentialsX)");
      this.economy = null;
      return;
    }

    this.economy = registration.getProvider();

    if (this.economy != null) {
      logger.info("Vault Economy hooked successfully! Provider: {}", this.economy.getClass().getSimpleName());
    } else {
      logger.warn("Vault Economy provider is null");
    }
  }

  public boolean isAvailable() {
    return this.economy != null;
  }

  public boolean has(final OfflinePlayer player, final double amount) {
    if (this.economy == null) {
      return false;
    }

    if (!this.economy.hasAccount(player)) {
      this.economy.createPlayerAccount(player);
    }

    return this.economy.has(player, amount);
  }

  public boolean withdraw(final OfflinePlayer player, final double amount) {
    if (this.economy == null) {
      return false;
    }

    final var response = this.economy.withdrawPlayer(player, amount);

    if (!response.transactionSuccess()) {
      logger.warn("Failed to withdraw {} from {}: {}", amount, player.getName(), response.errorMessage);
    }

    return response.transactionSuccess();
  }

  public void deposit(final OfflinePlayer player, final double amount) {
    if (this.economy == null) {
      return;
    }

    final var response = this.economy.depositPlayer(player, amount);

    if (!response.transactionSuccess()) {
      logger.error("Failed to deposit {} to {}: {}", amount, player.getName(), response.errorMessage);
    }
  }
}
