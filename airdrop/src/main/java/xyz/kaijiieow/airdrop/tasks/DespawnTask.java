package xyz.kaijiieow.airdrop.tasks;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

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
        plugin.getLoggingService().info("Locked airdrop despawned at " + loc.toString());
    }
}
