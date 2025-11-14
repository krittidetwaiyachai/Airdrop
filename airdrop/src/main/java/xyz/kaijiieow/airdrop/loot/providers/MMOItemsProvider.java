package xyz.kaijiieow.airdrop.loot.providers;

import ioL.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.loot.ItemProvider;

/**
 * (ข้อ 4)
 * Bridge: "สะพาน" เชื่อม API (MMOItems)
 */
public class MMOItemsProvider implements ItemProvider {

    private final AirdropPlugin plugin;
    private final boolean mmoItemsEnabled;

    public MMOItemsProvider(AirdropPlugin plugin) {
        this.plugin = plugin;
        this.mmoItemsEnabled = Bukkit.getPluginManager().isPluginEnabled("MMOItems");
        if (!mmoItemsEnabled) {
            plugin.getLogger().warning("MMOItems not found. The 'MMOITEMS' provider will not function.");
        }
    }

    @Override
    public String getName() {
        return "MMOITEMS";
    }

    @Override
    public ItemStack getItem(ConfigurationSection config) {
        if (!mmoItemsEnabled) {
            plugin.getLogger().severe("Attempted to generate an MMOItem, but MMOItems is not enabled!");
            return new ItemStack(Material.BARRIER);
        }

        String typeName = config.getString("type");
        String id = config.getString("id");

        if (typeName == null || id == null) {
            plugin.getLogger().severe("MMOItems config is missing 'type' or 'id'.");
            return new ItemStack(Material.BARRIER);
        }

        Type type = MMOItems.plugin.getTypes().get(typeName.toUpperCase());
        if (type == null) {
            plugin.getLogger().severe("MMOItems type '" + typeName + "' not found.");
            return new ItemStack(Material.BARRIER);
        }

        MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplate(type, id.toUpperCase());
        if (template == null) {
            plugin.getLogger().severe("MMOItem '" + id + "' of type '" + typeName + "' not found.");
            return new ItemStack(Material.BARRIER);
        }

        // สร้างไอเทม
        // (Logic นี้อาจจะต้องปรับตามเวอร์ชัน MMOItems ที่มึงใช้)
        ItemStack item = template.newBuilder(0, null).build().newBuilder().build(false);

        // (ข้อ 4) สุ่ม Min/Max Amount
        int minAmount = config.getInt("amount-min", 1);
        int maxAmount = config.getInt("amount-max", 1);
        int amount = (minAmount == maxAmount) ? minAmount : 
                java.util.concurrent.ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        
        item.setAmount(amount);
        return item;
    }
}