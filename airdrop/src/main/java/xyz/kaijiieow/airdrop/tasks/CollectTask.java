package com.yourplugin.airdrop.tasks;

import com.yourplugin.airdrop.AirdropPlugin;
import com.yourplugin.airdrop.core.Airdrop;
import com.yourplugin.airdrop.manager.AirdropManager;
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
