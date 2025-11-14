package com.yourplugin.airdrop.data;

import com.yourplugin.airdrop.AirdropPlugin;
import com.yourplugin.airdrop.core.Airdrop;
import com.yourplugin.airdrop.core.AirdropState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final AirdropPlugin plugin;
    private File file;
    private FileConfiguration config;

    public DataManager(AirdropPlugin plugin) {
        this.plugin = plugin;
        createFile();
    }

    private void createFile() {
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml");
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml");
            e.printStackTrace();
        }
    }

    public void saveNextSpawnTimestamp(long timestamp) {
        config.set("next-spawn-timestamp", timestamp);
        save();
    }

    public long getNextSpawnTimestamp() {
        return config.getLong("next-spawn-timestamp", -1L);
    }

    public void saveAirdrops(Collection<Airdrop> airdrops) {
        config.set("airdrops", null);
        for (Airdrop ad : airdrops) {
            String path = "airdrops." + ad.getId();
            Location loc = ad.getLocation();
            if (loc == null) continue;

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".state", ad.getState().name());
            config.set(path + ".owner", ad.getOwnerUuid() != null ? ad.getOwnerUuid().toString() : null);
            config.set(path + ".created-at", ad.getCreatedAt());
            config.set(path + ".unlocked-at", ad.getUnlockedAt());
            config.set(path + ".collect-expire-at", ad.getCollectExpireAt());
        }
        save();
    }

    public List<Airdrop> loadAirdrops() {
        List<Airdrop> list = new ArrayList<>();
        ConfigurationSection sec = config.getConfigurationSection("airdrops");
        if (sec == null) return list;

        for (String idStr : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(idStr);
            if (s == null) continue;

            String worldName = s.getString("world");
            int x = s.getInt("x");
            int y = s.getInt("y");
            int z = s.getInt("z");
            String stateStr = s.getString("state", "LOCKED");
            String ownerStr = s.getString("owner", null);

            long createdAt = s.getLong("created-at", System.currentTimeMillis());
            Long unlockedAt = s.contains("unlocked-at") ? s.getLong("unlocked-at") : null;
            Long collectExpireAt = s.contains("collect-expire-at") ? s.getLong("collect-expire-at") : null;

            if (Bukkit.getWorld(worldName) == null) continue;

            UUID id = UUID.fromString(idStr);
            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
            Airdrop airdrop = new Airdrop(id, loc, createdAt);

            airdrop.setState(AirdropState.valueOf(stateStr));
            if (ownerStr != null) {
                airdrop.setOwnerUuid(UUID.fromString(ownerStr));
            }
            airdrop.setUnlockedAt(unlockedAt);
            airdrop.setCollectExpireAt(collectExpireAt);

            list.add(airdrop);
        }
        return list;
    }
}
