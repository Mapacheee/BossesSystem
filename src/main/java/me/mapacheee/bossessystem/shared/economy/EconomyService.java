package me.mapacheee.bossessystem.shared.economy;

import com.thewinterframework.service.annotation.Service;
import com.thewinterframework.service.annotation.lifecycle.OnEnable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public final class EconomyService {

  private static final Logger logger = LoggerFactory.getLogger(EconomyService.class);

  private @Nullable Object economy;

  @OnEnable
  void hook() {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
      logger.warn("Vault plugin not found, economy features disabled");
      this.economy = null;
      return;
    }

    logger.info("Vault plugin found, attempting to hook into economy...");

    try {
      final var economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
      final var servicesManager = Bukkit.getServer().getServicesManager();
      final var registration = servicesManager.getRegistration(economyClass);

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
    } catch (ClassNotFoundException e) {
      logger.error("Could not find Vault Economy class. Is Vault installed correctly?", e);
      this.economy = null;
    } catch (Exception e) {
      logger.error("Unexpected error while hooking into Vault", e);
      this.economy = null;
    }
  }

  public boolean isAvailable() {
    return this.economy != null;
  }

  public boolean has(final OfflinePlayer player, final double amount) {
    if (this.economy == null) {
      logger.debug("Economy is null when checking balance for {}", player.getName());
      return false;
    }
    try {
      final var hasAccountMethod = this.economy.getClass().getMethod("hasAccount", OfflinePlayer.class);
      boolean hasAccount = (boolean) hasAccountMethod.invoke(this.economy, player);

      if (!hasAccount) {
        final var createAccountMethod = this.economy.getClass().getMethod("createPlayerAccount", OfflinePlayer.class);
        createAccountMethod.invoke(this.economy, player);
        logger.debug("Account created for {}", player.getName());
      }

      final var getBalanceMethod = this.economy.getClass().getMethod("getBalance", OfflinePlayer.class);
      double balance = (double) getBalanceMethod.invoke(this.economy, player);

      final var hasMethod = this.economy.getClass().getMethod("has", OfflinePlayer.class, double.class);
      boolean result = (boolean) hasMethod.invoke(this.economy, player, amount);

      logger.debug("{} balance: {}, needs: {}, has enough: {}", player.getName(), balance, amount, result);

      return result;
    } catch (Exception e) {
      logger.error("Error checking balance for {}: {}", player.getName(), e.getMessage());
      return false;
    }
  }

  public boolean withdraw(final OfflinePlayer player, final double amount) {
    if (this.economy == null) return false;
    try {
      final var withdrawMethod = this.economy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
      final var result = withdrawMethod.invoke(this.economy, player, amount);
      final var transactionSuccessMethod = result.getClass().getMethod("transactionSuccess");
      boolean success = (boolean) transactionSuccessMethod.invoke(result);

      if (!success) {
        logger.warn("Failed to withdraw {} from {}", amount, player.getName());
      }

      return success;
    } catch (Exception e) {
      logger.error("Error withdrawing {} from {}: {}", amount, player.getName(), e.getMessage());
      return false;
    }
  }

  public void deposit(final OfflinePlayer player, final double amount) {
    if (this.economy == null) return;
    try {
      final var depositMethod = this.economy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
      depositMethod.invoke(this.economy, player, amount);
      logger.debug("Deposited {} to {}", amount, player.getName());
    } catch (Exception e) {
      logger.error("Error depositing {} to {}: {}", amount, player.getName(), e.getMessage());
    }
  }
}
