package xyz.kaijiieow.airdrop.listeners;

import net.kyori.adventure.dialog.Dialog;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;
import xyz.kaijiieow.airdrop.manager.AirdropManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class PlayerInteractListener implements Listener {

    private final AirdropPlugin plugin;
    private final AirdropManager airdropManager;
    private final Random random = new Random();
    
    // Map: Player UUID -> Pair<Airdrop, CorrectCode>
    private final Map<UUID, Pair<Airdrop, String>> pendingInteractions = new HashMap<>();

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
            // ถ้าผู้เล่นคนนี้กำลังกรอกโค้ดกล่องอื่นอยู่ ไม่ต้องเปิดซ้ำ
            if (pendingInteractions.containsKey(player.getUniqueId())) {
                player.sendMessage("§cมึงกำลังกรอกโค้ดกล่องอื่นอยู่!");
                return;
            }
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
        int length = plugin.getConfig().getInt("minigame.code-length", 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(random.nextInt(10));
        String code = sb.toString();

        // เก็บไว้ว่าผู้เล่นคนนี้ กำลังพยายามเปิดกล่องนี้ ด้วยโค้ดนี้
        pendingInteractions.put(player.getUniqueId(), Pair.of(airdrop, code));

        plugin.getLoggingService().info("Generated code for " + player.getName() + " = " + code);

        // สร้าง Dialog API
        Dialog dialog = Dialog.dialog(Component.text("§lกรอกรหัส Airdrop"), ctx -> {
            // Player submitted the form
            String input = ctx.component(0);
            Pair<Airdrop, String> data = pendingInteractions.remove(player.getUniqueId());

            if (data == null) return; // Should not happen
            Airdrop currentAirdrop = data.getLeft();
            String correctCode = data.getRight();

            if (input != null && input.equals(correctCode)) {
                // ถูก! (ต้องรัน Unlock ใน Main Thread)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getLoggingService().info("Player " + player.getName() + " unlocked airdrop " + currentAirdrop.getId());
                    airdropManager.handleUnlock(currentAirdrop, player);
                });
            } else {
                // ผิด!
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cรหัสผิดไอ้โง่!");
                    player.playSound(player.getLocation(),
                            Sound.valueOf(plugin.getConfig().getString("sounds.fail", "BLOCK_ANVIL_LAND")),
                            1f, 1f);
                    double damage = plugin.getConfig().getDouble("minigame.damage-on-fail", 2.0);
                    if (damage > 0) {
                        player.damage(damage);
                    }
                });
            }

        }, Dialog.textInput(Component.text("รหัส " + length + " หลัก..."), Component.text(""), length, length));

        // Handler ตอนผู้เล่นปิด Dialog (เช่น กด ESC)
        dialog.onClose(ctx -> {
            // ถ้ายังไม่ได้ submit (เช่น กด ESC) ก็ลบออกจาก pending
            pendingInteractions.remove(player.getUniqueId());
        });

        player.openDialog(dialog);
    }
}