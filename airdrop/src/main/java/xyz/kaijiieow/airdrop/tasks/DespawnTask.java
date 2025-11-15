package xyz.kaijiieow.airdrop.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;

public class DespawnTask extends BukkitRunnable {

    private final AirdropPlugin plugin;
    private final Airdrop airdrop;

    public DespawnTask(AirdropPlugin plugin, Airdrop airdrop) {
        this.plugin = plugin;
        this.airdrop = airdrop;
    }

    @Override
    public void run() {
        if (airdrop.getState() != AirdropState.LOCKED) return;
        Location loc = airdrop.getLocation();
        if (loc == null) return;

        Block block = loc.getBlock();
        block.setType(org.bukkit.Material.AIR);
        airdrop.setState(AirdropState.DESPAWNED);

        plugin.getAirdropManager().removeAirdrop(airdrop, false);
        plugin.getEffectService().playDespawn(loc);

        String world = loc.getWorld().getName();
        String x = String.valueOf(loc.getBlockX());
        String y = String.valueOf(loc.getBlockY());
        String z = String.valueOf(loc.getBlockZ());
        Bukkit.broadcastMessage(plugin.getMessageService().format(
                "broadcast.despawn",
                "&6[Airdrop] &cกล่องที่ &e{world} &7({x}, {y}, {z}) &cหายไปเพราะไม่มีใครเปิดทันเวลา",
                java.util.Map.of("world", world, "x", x, "y", y, "z", z)
        ));
        
        // --- (แก้ตรงนี้) ---
        plugin.getLoggingService().logLockedDespawn(loc);
        // --- (จบ) ---
    }
}
