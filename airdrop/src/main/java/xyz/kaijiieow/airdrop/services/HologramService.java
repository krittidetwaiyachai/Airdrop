package xyz.kaijiieow.airdrop.services;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;

import java.util.*;

public class HologramService {

    private final AirdropPlugin plugin;
    private final boolean fancyHologramsEnabled;
    private final HologramManager holoManager;

    // map airdropId -> hologram object
    private final Map<UUID, Hologram> activeHolograms = new HashMap<>();
    private final Map<UUID, Integer> activeHoloTasks = new HashMap<>();

    public HologramService(AirdropPlugin plugin) {
        this.plugin = plugin;

        boolean enabled = Bukkit.getPluginManager().isPluginEnabled("FancyHolograms");
        HologramManager manager = null;

        if (enabled) {
            try {
                // entrypoint ตาม docs: FancyHologramsPlugin.get().getHologramManager()
                manager = FancyHologramsPlugin.get().getHologramManager();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook FancyHolograms API: " + e.getMessage());
                enabled = false;
            }
        }

        this.fancyHologramsEnabled = enabled && manager != null;
        this.holoManager = manager;

        if (!this.fancyHologramsEnabled) {
            plugin.getLogger().warning("FancyHolograms not found or API not available. Holograms will be disabled.");
        }
    }

    public void showLocked(Airdrop airdrop) {
        if (!fancyHologramsEnabled) return;

        Location loc = airdrop.getLocation();
        if (loc == null) return;
        Location holoLoc = loc.clone().add(0.5, 2.2, 0.5); // ขึ้นเหนือกล่องหน่อย

        String name = "airdrop_" + airdrop.getId();

        // เคลียร์ของเก่าถ้ามี
        holoManager.getHologram(name).ifPresent(h -> {
            holoManager.removeHologram(h);
        });
        activeHolograms.remove(airdrop.getId());
        cancelTask(airdrop.getId());

        // ใช้ TextHologramData ตาม API จริง
        TextHologramData data = new TextHologramData(name, holoLoc);
        data.setText(lockedLines(formatDespawnTime(airdrop)));

        Hologram hologram = holoManager.create(data);
        holoManager.addHologram(hologram);

        activeHolograms.put(airdrop.getId(), hologram);
        startLockedCountdown(airdrop, hologram);
    }

    public void showOwned(Airdrop airdrop, Player owner) {
        if (!fancyHologramsEnabled) return;

        Hologram holo = activeHolograms.get(airdrop.getId());
        if (holo == null) {
            // ลองดึงจาก manager เผื่อ plugin reload / map หาย
            String name = "airdrop_" + airdrop.getId();
            holo = holoManager.getHologram(name).orElse(null);
            if (holo == null) return;
            activeHolograms.put(airdrop.getId(), holo);
        }

        cancelTask(airdrop.getId());

        Long expireAt = airdrop.getCollectExpireAt();
        if (expireAt == null) return;

        Hologram finalHolo = holo;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // ถ้า state หลุดจากช่วงเก็บของแล้ว ไม่ต้องอัปเดตต่อ
                if (airdrop.getState() != AirdropState.COLLECTING
                        && airdrop.getState() != AirdropState.UNLOCKED_OWNED) {
                    activeHoloTasks.remove(airdrop.getId());
                    cancel();
                    remove(airdrop);
                    return;
                }

                long remainingMillis = expireAt - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    // หมดเวลา เดี๋ยว AirdropManager จัดการลบกล่องเอง
                    activeHoloTasks.remove(airdrop.getId());
                    cancel();
                    return;
                }

                long remainingSeconds = (remainingMillis / 1000L) + 1L;

                HologramData rawData = finalHolo.getData();
                if (rawData instanceof TextHologramData textData) {
                    textData.setText(List.of(
                            "<green><bold>[AIRDROP UNLOCKED]</bold></green>",
                            "<white>Owner: <aqua>" + owner.getName() + "</aqua></white>",
                            "<gray>Collect time: <yellow>" + remainingSeconds + "s</yellow></gray>"
                    ));

                    finalHolo.queueUpdate();
                    finalHolo.refreshForViewers();
                }
            }
        };

        int taskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
        activeHoloTasks.put(airdrop.getId(), taskId);
    }

    public void remove(Airdrop airdrop) {
        if (!fancyHologramsEnabled) return;

        // ยกเลิก task นับถอยหลัง
        cancelTask(airdrop.getId());

        Hologram holo = activeHolograms.remove(airdrop.getId());
        if (holo != null) {
            holoManager.removeHologram(holo);
        }
    }

    private void startLockedCountdown(Airdrop airdrop, Hologram hologram) {
        Long despawnAt = airdrop.getDespawnExpireAt();
        if (despawnAt == null) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (airdrop.getState() != AirdropState.LOCKED) {
                    activeHoloTasks.remove(airdrop.getId());
                    cancel();
                    return;
                }
                long remainingMillis = airdrop.getDespawnExpireAt() - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    activeHoloTasks.remove(airdrop.getId());
                    cancel();
                    return;
                }
                long remainingSeconds = (remainingMillis / 1000L) + 1L;
                HologramData rawData = hologram.getData();
                if (rawData instanceof TextHologramData textData) {
                    textData.setText(lockedLines(String.valueOf(remainingSeconds) + "s"));
                    hologram.queueUpdate();
                    hologram.refreshForViewers();
                }
            }
        };

        int taskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
        activeHoloTasks.put(airdrop.getId(), taskId);
    }

    private List<String> lockedLines(String timeText) {
        return List.of(
                "<red><bold>[AIRDROP LOCKED]</bold></red>",
                "<yellow>Click to unlock</yellow>",
                "<gray>Despawn in: <yellow>" + timeText + "</yellow></gray>"
        );
    }

    private String formatDespawnTime(Airdrop airdrop) {
        Long target = airdrop.getDespawnExpireAt();
        if (target == null) return "--";
        long remaining = target - System.currentTimeMillis();
        if (remaining <= 0) return "0s";
        long seconds = (remaining / 1000L) + 1L;
        return seconds + "s";
    }

    private void cancelTask(UUID id) {
        Integer taskId = activeHoloTasks.remove(id);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
