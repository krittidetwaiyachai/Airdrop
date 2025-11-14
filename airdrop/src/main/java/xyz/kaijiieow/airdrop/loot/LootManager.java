package xyz.kaijiieow.airdrop.loot;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class LootManager {

    private final AirdropPlugin plugin;
    private final Map<String, LootTable> tables = new HashMap<>();
    private final Random random = new Random();

    public LootManager(AirdropPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tables.clear();
        File file = new File(plugin.getDataFolder(), "loot.yml");
        FileConfiguration lootCfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection root = lootCfg.getConfigurationSection("loot-tables");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            int minItems = sec.getInt("min-items", 1);
            int maxItems = sec.getInt("max-items", 3);
            LootTable table = new LootTable(id, minItems, maxItems);

            for (Object o : sec.getList("entries", Collections.emptyList())) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) o;

                String entryId = (String) map.getOrDefault("id", id + "_e");
                String typeStr = (String) map.getOrDefault("type", "VANILLA");
                LootEntry.Type type = LootEntry.Type.valueOf(typeStr.toUpperCase());

                // --- FIX: อ่าน mmo-type ให้ถูก ---
                String materialOrType;
                if (type == LootEntry.Type.MMOITEMS) {
                    materialOrType = (String) map.getOrDefault("mmo-type", ""); // Use mmo-type
                } else {
                    materialOrType = (String) map.getOrDefault("material", "STONE"); // Use material
                }
                String mmoId = (String) map.getOrDefault("mmo-id", "");
                // --- END FIX ---

                int minAmount = ((Number) map.getOrDefault("min-amount", 1)).intValue();
                int maxAmount = ((Number) map.getOrDefault("max-amount", 1)).intValue();
                int weight = ((Number) map.getOrDefault("weight", 1)).intValue();

                LootEntry entry = new LootEntry(entryId, type, materialOrType, mmoId, minAmount, maxAmount, weight);
                table.addEntry(entry);
            }

            tables.put(id, table);
        }
    }

    public LootTable getTable(String id) {
        if (id != null && tables.containsKey(id)) return tables.get(id);
        String def = plugin.getConfig().getString("loot.default-table", "default");
        return tables.get(def);
    }

    public List<ItemStack> generateLoot(String tableId) {
        LootTable table = getTable(tableId);
        if (table == null) return Collections.emptyList();

        List<ItemStack> items = new ArrayList<>();
        for (LootEntry entry : table.roll()) {
            
            switch (entry.getType()) {
                case VANILLA -> {
                    Material mat = Material.matchMaterial(entry.getMaterialOrType());
                    if (mat != null) {
                        // VANILLA สุ่มจำนวนตรงนี้
                        int amount = entry.getMinAmount() + random.nextInt(Math.max(1,
                                entry.getMaxAmount() - entry.getMinAmount() + 1));
                        items.add(new ItemStack(mat, amount));
                    } else {
                        plugin.getLogger().warning("Invalid VANILLA material in loot.yml: " + entry.getMaterialOrType());
                        items.add(new ItemStack(Material.BARRIER));
                    }
                }
                case MMOITEMS -> {
                    // --- FIX: เรียกใช้ Provider จริงๆ ---
                    ItemProvider provider = plugin.getItemProvider(LootEntry.Type.MMOITEMS.name());
                    if (provider == null) {
                        plugin.getLogger().warning("MMOItems provider not found, but loot.yml requests it. (MMOItems enabled?)");
                        items.add(new ItemStack(Material.BARRIER));
                        continue;
                    }

                    // สร้าง Fake ConfigurationSection ให้ Provider (เพราะดีไซน์มึงมั่ว)
                    ConfigurationSection fakeConfig = new YamlConfiguration().createSection("entry");
                    fakeConfig.set("type", entry.getMaterialOrType()); // This is the MMOItem Type (e.g., "SWORD")
                    fakeConfig.set("id", entry.getMmoId());       // This is the MMOItem ID (e.g., "LEGENDARY_SWORD")
                    fakeConfig.set("amount-min", entry.getMinAmount());
                    fakeConfig.set("amount-max", entry.getMaxAmount());

                    // Provider (MMOItemsProvider) จะสุ่ม amount ให้เอง
                    items.add(provider.getItem(fakeConfig));
                    // --- END FIX ---
                }
            }
        }
        return items;
    }
}