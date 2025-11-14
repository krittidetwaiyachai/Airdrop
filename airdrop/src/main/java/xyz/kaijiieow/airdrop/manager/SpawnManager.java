package com.yourplugin.airdrop.manager;

import com.yourplugin.airdrop.AirdropPlugin;
import com.yourplugin.airdrop.data.DataManager;
import com.yourplugin.airdrop.tasks.SpawnTimerTask;

public class SpawnManager {

    private final AirdropPlugin plugin;
    private final DataManager dataManager;

    public SpawnManager(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    public void initScheduler() {
        if (!plugin.getConfig().getBoolean("spawn-settings.enabled", true)) return;

        long saved = dataManager.getNextSpawnTimestamp();
        long now = System.currentTimeMillis();
        long intervalMinutes = plugin.getConfig().getLong("spawn-settings.interval-minutes", 30L);
        long intervalMillis = intervalMinutes * 60_000L;

        long next;
        if (saved <= 0 || saved < now) {
            next = now + intervalMillis;
        } else {
            next = saved;
        }

        scheduleNext(next);
    }

    public void scheduleNext(long timestamp) {
        dataManager.saveNextSpawnTimestamp(timestamp);
        long delayMillis = Math.max(0, timestamp - System.currentTimeMillis());
        long ticks = Math.max(1L, delayMillis / 50L);

        SpawnTimerTask task = new SpawnTimerTask(plugin);
        task.runTaskLater(plugin, ticks);
    }
}
