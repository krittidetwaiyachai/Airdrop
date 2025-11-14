package com.yourplugin.airdrop;

import com.yourplugin.airdrop.commands.AirdropCommand;
import com.yourplugin.airdrop.core.Airdrop;
import com.yourplugin.airdrop.data.DataManager;
import com.yourplugin.airdrop.listeners.OwnershipListener;
import com.yourplugin.airdrop.listeners.PlayerInteractListener;
import com.yourplugin.airdrop.listeners.ProtectionListener;
import com.yourplugin.airdrop.loot.LootManager;
import com.yourplugin.airdrop.manager.AirdropManager;
import com.yourplugin.airdrop.manager.SpawnManager;
import com.yourplugin.airdrop.services.EffectService;
import com.yourplugin.airdrop.services.HologramService;
import com.yourplugin.airdrop.services.LoggingService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class AirdropPlugin extends JavaPlugin {

    private static AirdropPlugin instance;

    private DataManager dataManager;
    private LootManager lootManager;
    private AirdropManager airdropManager;
    private SpawnManager spawnManager;
    private LoggingService loggingService;
    private HologramService hologramService;
    private EffectService effectService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // ensure loot.yml
        File lootFile = new File(getDataFolder(), "loot.yml");
        if (!lootFile.exists()) {
            saveResource("loot.yml", false);
        }

        this.dataManager = new DataManager(this);
        this.loggingService = new LoggingService(this);
        this.hologramService = new HologramService(this);
        this.effectService = new EffectService(this);
        this.lootManager = new LootManager(this);
        this.airdropManager = new AirdropManager(this);
        this.spawnManager = new SpawnManager(this);

        // load persisted airdrops
        List<Airdrop> loaded = dataManager.loadAirdrops();
        this.airdropManager.loadExisting(loaded);
        this.spawnManager.initScheduler();

        // listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new OwnershipListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // command
        if (getCommand("air") != null) {
            getCommand("air").setExecutor(new AirdropCommand(this));
        }

        getLogger().info("AirdropPlugin enabled.");
    }

    @Override
    public void onDisable() {
        dataManager.saveAirdrops(airdropManager.getAllAirdrops());
        getLogger().info("AirdropPlugin disabled.");
    }

    public static AirdropPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public AirdropManager getAirdropManager() {
        return airdropManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public LoggingService getLoggingService() {
        return loggingService;
    }

    public HologramService getHologramService() {
        return hologramService;
    }

    public EffectService getEffectService() {
        return effectService;
    }
}
