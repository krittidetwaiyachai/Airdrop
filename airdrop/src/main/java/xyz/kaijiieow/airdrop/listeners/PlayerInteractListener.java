package xyz.kaijiieow.airdrop.listeners;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.Random;

public class PlayerInteractListener implements Listener {

    private final AirdropPlugin plugin;
    private final AirdropManager airdropManager;
    private final Random random = new Random();

    public PlayerInteractListener(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.airdropManager = plugin.getAirdropManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        Optional<Airdrop> opt = airdropManager.getAirdropAt(block);
        if (opt.isEmpty()) return;

        Airdrop airdrop = opt.get();
        Player player = event.getPlayer();

        event.setCancelled(true);

        if (airdrop.getState() == AirdropState.LOCKED) {
            openCodeDialog(airdrop, player);
            return;
        }

        if (airdrop.getOwnerUuid() != null && airdrop.getOwnerUuid().equals(player.getUniqueId())) {
            if (block.getState() instanceof Container container) {
                player.openInventory(container.getInventory());
            }
        } else {
            player.sendMessage("§cกล่องนี้เป็นของคนอื่นแล้ว!");
        }
    }

    private void openCodeDialog(Airdrop airdrop, Player player) {
        // TODO: ตรงนี้ควรใช้ Paper Dialog API ของจริง
        // ตอนนี้ทำ demo: สร้างโค้ด, log, แล้ว auto-unlock ให้เลย

        int length = plugin.getConfig().getInt("minigame.code-length", 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(random.nextInt(10));
        String code = sb.toString();

        plugin.getLoggingService().info("Generated code for " + player.getName() + " = " + code);
        player.sendMessage("§e(เดโม) โค้ดของกล่องนี้คือ §f" + code + " §7(ในของจริงจะเด้ง Dialog API ให้กรอก)");

        // เดโม: ปล่อยให้ถือว่ากรอกถูกทันที
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLoggingService().info("(Demo) Auto-unlock airdrop for " + player.getName());
            airdropManager.handleUnlock(airdrop, player);
        });
    }
}
