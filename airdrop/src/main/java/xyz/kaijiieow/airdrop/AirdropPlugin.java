package xyz.kaijiieow.airdrop;

import xyz.kaijiieow.airdrop.commands.AirdropCommand;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.data.DataManager;
import xyz.kaijiieow.airdrop.listeners.OwnershipListener;
import xyz.kaijiieow.airdrop.listeners.PlayerInteractListener;
import xyz.kaijiieow.airdrop.listeners.ProtectionListener;
import xyz.kaijiieow.airdrop.loot.ItemProvider;
import xyz.kaijiieow.airdrop.loot.LootManager;
import xyz.kaijiieow.airdrop.loot.MMOItemsProvider;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import xyz.kaijiieow.airdrop.manager.SpawnManager;
import xyz.kaijiieow.airdrop.services.EffectService;
import xyz.kaijiieow.airdrop.services.HologramService;
import xyz.kaijiieow.airdrop.services.LoggingService;
import xyz.kaijiieow.airdrop.services.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AirdropPlugin extends JavaPlugin {

    private static AirdropPlugin instance;

    private DataManager dataManager;
    private LootManager lootManager;
    private AirdropManager airdropManager;
    private SpawnManager spawnManager;
    private LoggingService loggingService;
    private HologramService hologramService;
    private EffectService effectService;
    private MessageService messageService;

    private final Map<String, ItemProvider> itemProviders = new HashMap<>();

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
        this.messageService = new MessageService(this);
        this.loggingService = new LoggingService(this);
        this.hologramService = new HologramService(this);
        this.effectService = new EffectService(this);
        this.lootManager = new LootManager(this);

        // Register item providers AFTER lootManager
        if (getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            registerItemProvider(new MMOItemsProvider(this));
        }

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

        // command registration is deferred to avoid Paper's async command tree race
        getServer().getScheduler().runTask(this, this::registerCommands);
        getServer().getPluginManager().registerEvents(new ServerLifecycleListener(), this);

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

    public void registerItemProvider(ItemProvider provider) {
        itemProviders.put(provider.getName().toUpperCase(), provider);
        getLogger().info("Registered item provider: " + provider.getName());
    }

    public ItemProvider getItemProvider(String name) {
        return itemProviders.get(name.toUpperCase());
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

    public MessageService getMessageService() {
        return messageService;
    }

    private void registerCommands() {
        if (getCommand("airdrop") != null) {
            getCommand("airdrop").setExecutor(new AirdropCommand(this));
        } else {
            getLogger().warning("Command 'airdrop' missing from plugin.yml; unable to register executor.");
        }
    }

    private class ServerLifecycleListener implements Listener {
        @EventHandler
        public void onServerLoad(ServerLoadEvent event) {
            registerCommands();
        }
    }
}
