package xyz.kaijiieow.airdrop.services;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HologramService {

    private final AirdropPlugin plugin;
    private final boolean decentHologramsEnabled;

    private final Map<UUID, String> hologramIds = new HashMap<>();
    private final Map<UUID, Integer> activeHoloTasks = new HashMap<>();

    public HologramService(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.decentHologramsEnabled = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        if (!decentHologramsEnabled) {
            plugin.getLogger().warning("DecentHolograms not found. Holograms will be disabled.");
        }
    }

    public void showLocked(Airdrop airdrop) {
        if (!decentHologramsEnabled) return;

        Location loc = airdrop.getLocation();
        if (loc == null) return;
        Location holoLoc = loc.clone().add(0.5, 2.2, 0.5); // 2.2 blocks above chest

        String id = "airdrop_" + airdrop.getId();
        hologramIds.put(airdrop.getId(), id);

        // ลบของเก่า (ถ้ามี)
        if (DHAPI.getHologram(id) != null) {
            DHAPI.deleteHologram(id);
        }
        
        // สร้างใหม่
        List<String> lines = List.of(
                "§c§l[AIRDROP LOCKED]",
                "§eClick to Unlock"
        );
        DHAPI.createHologram(id, holoLoc, lines);
    }

    public void showOwned(Airdrop airdrop, Player owner) {
        if (!decentHologramsEnabled) return;

        String id = hologramIds.get(airdrop.getId());
        if (id == null) return;
        Hologram holo = DHAPI.getHologram(id);
        if (holo == null) return;

        // ยกเลิก Task เก่า (ถ้ามี)
        Integer oldTaskId = activeHoloTasks.remove(airdrop.getId());
        if (oldTaskId != null) Bukkit.getScheduler().cancelTask(oldTaskId);
        
        Long expireAt = airdrop.getCollectExpireAt();
        if (expireAt == null) return; // Should not happen

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (airdrop.getState() != AirdropState.COLLECTING && airdrop.getState() != AirdropState.UNLOCKED_OWNED) {
                    cancel();
                    remove(airdrop); // Airdrop ถูกลบไปแล้ว
                    return;
                }

                long remainingMillis = expireAt - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    // เวลาหมด, hologram จะถูกลบโดย AirdropManager
                    cancel();
                    return;
                }

                long remainingSeconds = (remainingMillis / 1000) + 1;

                List<String> lines = List.of(
                        "§a§l[AIRDROP UNLOCKED]",
                        "§fOwner: §e" + owner.getName(),
                        "§fCollect Time: §e" + remainingSeconds + "s"
                );
                DHAPI.setHologramLines(holo, lines);
            }
        };
        
        int taskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
        activeHoloTasks.put(airdrop.getId(), taskId);
    }

    public void remove(Airdrop airdrop) {
        if (!decentHologramsEnabled) return;

        String id = hologramIds.remove(airdrop.getId());
        
        // ยกเลิก Task อัปเดตเวลา
        Integer taskId = activeHoloTasks.remove(airdrop.getId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (id == null) return;
        
        // ลบ Hologram จริง
        Hologram holo = DHAPI.getHologram(id);
        if (holo != null) {
            DHAPI.deleteHologram(id);
        }
    }
}