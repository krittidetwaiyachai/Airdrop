package com.yourplugin.airdrop.commands;

import com.yourplugin.airdrop.AirdropPlugin;
import com.yourplugin.airdrop.manager.AirdropManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AirdropCommand implements CommandExecutor {

    private final AirdropPlugin plugin;

    public AirdropCommand(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("airdrop.admin")) {
            sender.sendMessage("§cคุณไม่มีสิทธิ์ใช้คำสั่งนี้");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e/air reload §7- รีโหลด config & loot");
            sender.sendMessage("§e/air spawn [table] §7- สร้าง airdrop ที่เท้าคนพิมพ์");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getLootManager().reload();
                sender.sendMessage("§aรีโหลด config และ loot แล้ว");
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("ต้องใช้ในเกมเท่านั้น");
                    return true;
                }
                String table = args.length >= 2
                        ? args[1]
                        : plugin.getConfig().getString("loot.default-table", "default");

                Location loc = player.getLocation().getBlock().getLocation();
                var airdrop = plugin.getAirdropManager().createAirdrop(loc);

                if (loc.getBlock().getState() instanceof org.bukkit.block.Container container) {
                    var items = plugin.getLootManager().generateLoot(table);
                    for (var item : items) {
                        container.getInventory().addItem(item);
                    }
                }

                sender.sendMessage("§aสร้าง airdrop ที่ตำแหน่งคุณแล้ว (loot=" + table + ")");
            }
            default -> sender.sendMessage("§cคำสั่งไม่ถูกต้อง");
        }
        return true;
    }
}
