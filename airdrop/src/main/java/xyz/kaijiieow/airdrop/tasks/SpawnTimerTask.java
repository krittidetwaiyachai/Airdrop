package xyz.kaijiieow.airdrop.tasks;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import xyz.kaijiieow.airdrop.manager.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SpawnTimerTask extends BukkitRunnable {

    private final AirdropPlugin plugin;
    private final Random random = new Random();

    public SpawnTimerTask(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("spawn-settings.enabled", true)) return;

        AirdropManager mgr = plugin.getAirdropManager();
        SpawnManager spawnMgr = plugin.getSpawnManager();

        String worldName = plugin.getConfig().getString("spawn-settings.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        int centerX = plugin.getConfig().getInt("spawn-settings.center-x", 0);
        int centerZ = plugin.getConfig().getInt("spawn-settings.center-z", 0);
        int radius = plugin.getConfig().getInt("spawn-settings.radius", 500);
        boolean surfaceOnly = plugin.getConfig().getBoolean("spawn-settings.surface-only", true);

        int x = centerX + random.nextInt(radius * 2 + 1) - radius;
        int z = centerZ + random.nextInt(radius * 2 + 1) - radius;

        int y;
        if (surfaceOnly) {
            y = world.getHighestBlockYAt(x, z);
        } else {
            y = 64;
        }

        Location loc = new Location(world, x, y, z);
        Block block = world.getBlockAt(loc);
        if (!block.getType().isAir()) {
            loc = loc.getBlock().getRelative(BlockFace.UP).getLocation();
        }

        var airdrop = mgr.createAirdrop(loc);

        // เติม loot default
        if (loc.getBlock().getState() instanceof org.bukkit.block.Container container) {
            var items = plugin.getLootManager().generateLoot(
                    plugin.getConfig().getString("loot.default-table", "default")
            );
            for (var item : items) {
                container.getInventory().addItem(item);
            }
        }

        long intervalMinutes = plugin.getConfig().getLong("spawn-settings.interval-minutes", 30L);
        long next = System.currentTimeMillis() + intervalMinutes * 60_000L;
        spawnMgr.scheduleNext(next);
    }
}
