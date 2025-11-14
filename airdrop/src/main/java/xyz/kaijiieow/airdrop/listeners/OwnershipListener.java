package xyz.kaijiieow.airdrop.listeners;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class OwnershipListener implements Listener {

    private final AirdropPlugin plugin;
    private final AirdropManager airdropManager;

    public OwnershipListener(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.airdropManager = plugin.getAirdropManager();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Container container)) return;

        airdropManager.onInventoryClosed(player, container);
    }
}
