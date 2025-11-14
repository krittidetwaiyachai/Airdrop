package xyz.kaijiieow.airdrop.tasks;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CollectTask extends BukkitRunnable {

    private final AirdropPlugin plugin;
    private final Airdrop airdrop;

    public CollectTask(AirdropPlugin plugin, Airdrop airdrop) {
        this.plugin = plugin;
        this.airdrop = airdrop;
    }

    @Override
    public void run() {
        AirdropManager mgr = plugin.getAirdropManager();
        mgr.collectTimeout(airdrop);
    }
}
