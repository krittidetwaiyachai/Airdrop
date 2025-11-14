package xyz.kaijiieow.airdrop.loot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * (ข้อ 4)
 * Interface "สะพาน"
 * สำหรับปลั๊กอิน NBT อื่นๆ (MMOItems, etc.)
 */
public interface ItemProvider {

    /**
     * @return ชื่อของ Provider (เช่น "MMOITEMS", "VANILLA")
     */
    String getName();

    /**
     * สร้าง ItemStack จาก ConfigurationSection
     * @param configSection Section ของไอเทม (เช่น "provider: MMOITEMS", "id: SWORD1")
     * @return ItemStack ที่สร้างแล้ว
     */
    ItemStack getItem(ConfigurationSection configSection);
}