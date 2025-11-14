package xyz.kaijiieow.airdrop.manager;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import xyz.kaijiieow.airdrop.tasks.CollectTask;
import xyz.kaijiieow.airdrop.tasks.DespawnTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;

import java.util.*;

public class AirdropManager {

    private final AirdropPlugin plugin;
    private final Map<UUID, Airdrop> airdrops = new HashMap<>();

    public AirdropManager(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadExisting(Collection<Airdrop> list) {
        for (Airdrop a : list) {
            airdrops.put(a.getId(), a);

            if (a.getState() == AirdropState.LOCKED) {
                if (plugin.getConfig().getBoolean("airdrop-despawn.enabled")) {
                    long remainingMillis = a.getDespawnExpireAt() != null
                            ? a.getDespawnExpireAt() - System.currentTimeMillis()
                            : plugin.getConfig().getLong("airdrop-despawn.despawn-time-seconds", 300L) * 1000L;
                    if (remainingMillis > 0) {
                        scheduleDespawn(a, remainingMillis);
                    } else {
                        removeAirdrop(a, true);
                        continue;
                    }
                }
                plugin.getHologramService().showLocked(a);
            }

            if (a.getState() == AirdropState.COLLECTING && a.getCollectExpireAt() != null) {
                long remaining = a.getCollectExpireAt() - System.currentTimeMillis();
                if (remaining > 0) {
                    scheduleCollect(a, remaining);
                }
            }

            plugin.getEffectService().startAmbientEffect(a);
        }
    }

    public Collection<Airdrop> getAllAirdrops() {
        return airdrops.values();
    }

    public Airdrop createAirdrop(Location loc) {
        Block block = loc.getBlock();
        block.setType(org.bukkit.Material.CHEST);

        UUID id = UUID.randomUUID();
        Airdrop a = new Airdrop(id, block.getLocation(), System.currentTimeMillis());
        airdrops.put(id, a);

        if (plugin.getConfig().getBoolean("airdrop-despawn.enabled")) {
            scheduleDespawn(a);
        }

        plugin.getEffectService().playSpawnEffect(loc);
        plugin.getEffectService().startAmbientEffect(a);
        plugin.getHologramService().showLocked(a);
        plugin.getLoggingService().info("Airdrop spawned at " + loc.toString());
        broadcastLocation(loc);

        return a;
    }

    public Optional<Airdrop> getAirdropAt(Block block) {
        if (block == null) return Optional.empty();
        Location loc = block.getLocation();
        return airdrops.values().stream()
                .filter(a -> {
                    Location l = a.getLocation();
                    return l != null && l.getWorld().equals(loc.getWorld())
                            && l.getBlockX() == loc.getBlockX()
                            && l.getBlockY() == loc.getBlockY()
                            && l.getBlockZ() == loc.getBlockZ();
                })
                .findFirst();
    }

    public void handleUnlock(Airdrop airdrop, Player player) {
        airdrop.setOwnerUuid(player.getUniqueId());
        airdrop.setUnlockedAt(System.currentTimeMillis());
        airdrop.setState(AirdropState.UNLOCKED_OWNED);

        cancelDespawn(airdrop);

        long collectSeconds = plugin.getConfig().getLong("airdrop-collect.collect-time-seconds", 60L);
        scheduleCollect(airdrop, collectSeconds * 1000L);

        plugin.getHologramService().showOwned(airdrop, player);

        Block block = airdrop.getLocation().getBlock();
        if (block.getState() instanceof Container container) {
            player.openInventory(container.getInventory());
        }

        Bukkit.broadcastMessage("§a[AirDrop] §f" + player.getName() + " §aปลดล็อกกล่อง Airdrop ได้แล้ว!");
        plugin.getEffectService().playSuccess(player.getLocation());
    }

    public void scheduleDespawn(Airdrop airdrop) {
        long seconds = plugin.getConfig().getLong("airdrop-despawn.despawn-time-seconds", 300L);
        scheduleDespawn(airdrop, seconds * 1000L);
    }

    private void scheduleDespawn(Airdrop airdrop, long millis) {
        long ticks = Math.max(1L, millis / 50L);
        DespawnTask task = new DespawnTask(plugin, airdrop);
        int taskId = task.runTaskLater(plugin, ticks).getTaskId();
        airdrop.setDespawnTaskId(taskId);
        airdrop.setDespawnExpireAt(System.currentTimeMillis() + millis);
    }

    public void cancelDespawn(Airdrop airdrop) {
        Integer id = airdrop.getDespawnTaskId();
        if (id != null) {
            Bukkit.getScheduler().cancelTask(id);
            airdrop.setDespawnTaskId(null);
        }
        airdrop.setDespawnExpireAt(null);
    }

    public void scheduleCollect(Airdrop airdrop, long millis) {
        airdrop.setState(AirdropState.COLLECTING);
        long ticks = Math.max(1L, millis / 50L);
        CollectTask task = new CollectTask(plugin, airdrop);
        int taskId = task.runTaskLater(plugin, ticks).getTaskId();
        airdrop.setCollectTaskId(taskId);
        airdrop.setCollectExpireAt(System.currentTimeMillis() + millis);
    }

    public void removeAirdrop(Airdrop airdrop, boolean removeBlock) {
        plugin.getEffectService().stopAmbientEffect(airdrop);
        if (removeBlock && airdrop.getLocation() != null) {
            airdrop.getLocation().getBlock().setType(org.bukkit.Material.AIR);
        }
        cancelDespawn(airdrop);
        Integer cId = airdrop.getCollectTaskId();
        if (cId != null) Bukkit.getScheduler().cancelTask(cId);

        airdrop.setState(AirdropState.DESPAWNED);
        airdrops.remove(airdrop.getId());
        plugin.getHologramService().remove(airdrop);
    }

    public void collectTimeout(Airdrop airdrop) {
        Location loc = airdrop.getLocation();
        if (loc == null) return;

        Block block = loc.getBlock();
        if (block.getState() instanceof Container container) {
            container.getInventory().forEach(item -> {
                if (item != null) {
                    block.getWorld().dropItemNaturally(
                            loc.clone().add(0.5, 1, 0.5),
                            item
                    );
                }
            });
            container.getInventory().clear();
        }

        removeAirdrop(airdrop, true);
        plugin.getEffectService().playDespawn(loc);
        plugin.getLoggingService().info("Airdrop at " + loc.toString() + " expired & removed.");
    }

    public void onInventoryClosed(Player player, Container container) {
        getAirdropAt(container.getBlock()).ifPresent(ad -> {
            if (ad.getOwnerUuid() == null || !ad.getOwnerUuid().equals(player.getUniqueId())) return;

            boolean empty = Arrays.stream(container.getInventory().getStorageContents())
                    .allMatch(Objects::isNull);

            if (empty) {
                Location loc = ad.getLocation();
                removeAirdrop(ad, true);
                if (loc != null) {
                    plugin.getEffectService().playDespawn(loc);
                    plugin.getLoggingService().info("Airdrop at " + loc.toString() + " emptied by owner & removed.");
                }
            }
        });
    }

    private void broadcastLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        String worldName = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        Bukkit.broadcastMessage("§a[AirDrop] §fพบกล่องตกที่ §e" + worldName
                + " §7(" + x + ", " + y + ", " + z + ")");
        showSpawnTitle(worldName, x, y, z);
    }

    private void showSpawnTitle(String world, int x, int y, int z) {
        String subtitle = "§7" + world + " §f(" + x + ", " + y + ", " + z + ")";
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendTitle("§6✦ Airdrop ปรากฏ ✦", subtitle, 10, 60, 10)
        );
    }
}
