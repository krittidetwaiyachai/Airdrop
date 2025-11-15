package xyz.kaijiieow.airdrop.commands;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.kaijiieow.airdrop.services.MessageService;

public class AirdropCommand implements CommandExecutor {

    private final AirdropPlugin plugin;

    public AirdropCommand(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessageService();
        if (!sender.hasPermission("airdrop.admin")) {
            sender.sendMessage(messages.get("no-permission", "&cคุณไม่มีสิทธิ์ใช้คำสั่งนี้"));
            return true;
        }

        if (args.length == 0) {
            for (String line : messages.getList("commands.help", java.util.List.of(
                    "&e/air reload &7- รีโหลด config & loot",
                    "&e/air spawn [table] &7- สร้าง airdrop ที่เท้าคนพิมพ์"
            ))) {
                sender.sendMessage(line);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getLootManager().reload();
                sender.sendMessage(messages.get("commands.reload", "&aรีโหลด config และ loot แล้ว"));
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.get("commands.in-game-only", "&cต้องใช้ในเกมเท่านั้น"));
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

                sender.sendMessage(messages.format(
                        "commands.spawn-success",
                        "&aสร้าง airdrop ที่ตำแหน่งคุณแล้ว (loot={table})",
                        java.util.Map.of("table", table)
                ));
            }
            default -> sender.sendMessage(messages.get("commands.unknown", "&cคำสั่งไม่ถูกต้อง"));
        }
        return true;
    }
}
