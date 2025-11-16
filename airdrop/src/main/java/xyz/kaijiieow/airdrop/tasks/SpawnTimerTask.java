package xyz.kaijiieow.airdrop.tasks;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import xyz.kaijiieow.airdrop.manager.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List; // (เพิ่ม import)
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SpawnTimerTask extends BukkitRunnable {

    private final AirdropPlugin plugin;
    private final Random random = new Random();

    public SpawnTimerTask(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("spawn-settings.enabled", true)) return;

        final AirdropManager mgr = plugin.getAirdropManager();
        final SpawnManager spawnMgr = plugin.getSpawnManager();

        // --- (แก้ตรงนี้) ---
        List<String> worldNames = plugin.getConfig().getStringList("spawn-settings.worlds");
        if (worldNames.isEmpty()) {
            plugin.getLogger().warning("No spawn worlds configured in config.yml under 'spawn-settings.worlds'. Airdrop spawn task will not run.");
            return;
        }

        // สุ่มโลกจาก List
        String worldName = worldNames.get(random.nextInt(worldNames.size()));
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' configured in config.yml not found. Skipping spawn.");
            return;
        }
        // --- (จบ) ---

        final int centerX = plugin.getConfig().getInt("spawn-settings.center-x", 0);
        final int centerZ = plugin.getConfig().getInt("spawn-settings.center-z", 0);
        final int radius = plugin.getConfig().getInt("spawn-settings.radius", 500);
        final boolean surfaceOnly = plugin.getConfig().getBoolean("spawn-settings.surface-only", true);
        final long intervalSeconds = plugin.getConfig().getLong("spawn-settings.interval-seconds", 1800L);
        final int fixedY = plugin.getConfig().getInt("spawn-settings.fixed-y", 64);
        final int maxAttempts = getMaxLocationAttempts();
        final int surfaceStartY = plugin.getConfig().getInt("spawn-settings.surface-search.start-y", 250);
        final int surfaceMinY = plugin.getConfig().getInt("spawn-settings.surface-search.min-y", world.getMinHeight());

        CompletableFuture<Location> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Location calculated = surfaceOnly
                        ? findSurfaceLocation(world, centerX, centerZ, radius, maxAttempts, surfaceStartY, surfaceMinY)
                        : findOpenLocation(world, centerX, centerZ, radius, fixedY, maxAttempts);
                future.complete(calculated);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        future.whenComplete((loc, throwable) -> {
            Location result = throwable == null ? loc : null;
            if (throwable != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to calculate airdrop spawn location asynchronously", throwable);
            }

            if (!plugin.isEnabled()) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    completeSpawn(mgr, spawnMgr, result, worldName, intervalSeconds, maxAttempts));
        });
    }

    private void completeSpawn(AirdropManager mgr, SpawnManager spawnMgr, Location loc, String worldName,
                               long intervalSeconds, int maxAttempts) {
        long next = System.currentTimeMillis() + intervalSeconds * 1000L;
        if (loc == null) {
            plugin.getLogger().warning("Unable to find a solid, non-liquid spawn location in world '" + worldName + "' after "
                    + maxAttempts + " attempts. Skipping this cycle.");
            spawnMgr.scheduleNext(next);
            return;
        }

        var airdrop = mgr.createAirdrop(loc);

        if (loc.getBlock().getState() instanceof org.bukkit.block.Container container) {
            var items = plugin.getLootManager().generateLoot(
                    plugin.getConfig().getString("loot.default-table", "default")
            );
            for (var item : items) {
                container.getInventory().addItem(item);
            }
        }

        spawnMgr.scheduleNext(next);
    }

    private Location findSurfaceLocation(World world, int centerX, int centerZ, int radius, int maxAttempts,
                                         int startY, int minY) {
        int highest = Math.min(world.getMaxHeight() - 2, startY);
        int lowest = Math.max(world.getMinHeight(), minY);
        if (lowest > highest) {
            lowest = world.getMinHeight();
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = centerX + random.nextInt(radius * 2 + 1) - radius;
            int z = centerZ + random.nextInt(radius * 2 + 1) - radius;
            int chunkX = Math.floorDiv(x, 16);
            int chunkZ = Math.floorDiv(z, 16);
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Location location = scanColumn(world, x, z, highest, lowest);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private Location scanColumn(World world, int x, int z, int startY, int minY) {
        for (int y = startY; y >= minY; y--) {
            Block ground = world.getBlockAt(x, y, z);
            if (!isValidGround(ground)) {
                continue;
            }
            Block headSpace = ground.getRelative(BlockFace.UP);
            if (!headSpace.isEmpty()) {
                continue;
            }
            return headSpace.getLocation();
        }
        return null;
    }

    private Location findOpenLocation(World world, int centerX, int centerZ, int radius, int fixedY, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = centerX + random.nextInt(radius * 2 + 1) - radius;
            int z = centerZ + random.nextInt(radius * 2 + 1) - radius;
            int chunkX = Math.floorDiv(x, 16);
            int chunkZ = Math.floorDiv(z, 16);
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Location candidate = new Location(world, x, fixedY, z);
            Block space = world.getBlockAt(candidate);
            if (!space.isEmpty()) {
                continue;
            }
            Block ground = space.getRelative(BlockFace.DOWN);
            if (!isValidGround(ground)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private boolean isValidGround(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (block.isEmpty() || block.isLiquid() || !type.isSolid()) {
            return false;
        }
        return type != Material.MAGMA_BLOCK && type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE;
    }

    private int getMaxLocationAttempts() {
        return Math.max(5, plugin.getConfig().getInt("spawn-settings.max-location-attempts", 50));
    }
}
