package me.usainsrht.ujobs;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

public final class UJobs extends JavaPlugin {

    public static UJobs instance;
    public static MiniMessage miniMessage;
    public static Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        miniMessage = MiniMessage.miniMessage();
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        }

        loadConfig();
    }

    @Override
    public void onDisable() {

    }

    public void reload() {
        reloadConfig();

        loadConfig();
    }

    public void loadConfig() {

    }


}
