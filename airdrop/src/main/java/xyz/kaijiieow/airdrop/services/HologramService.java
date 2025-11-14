package xyz.kaijiieow.airdrop.services;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramService {

    private final AirdropPlugin plugin;

    private final Map<UUID, String> hologramIds = new HashMap<>();

    public HologramService(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public void showLocked(Airdrop airdrop) {
        Location loc = airdrop.getLocation();
        if (loc == null) return;
        String id = "airdrop_" + airdrop.getId();
        hologramIds.put(airdrop.getId(), id);
        // TODO: เรียก DecentHolograms API สร้าง hologram [LOCKED]
    }

    public void showOwned(Airdrop airdrop, Player owner) {
        // TODO: อัปเดต hologram → "Owner: <name>" + เวลาเหลือจาก collect-expire-at
    }

    public void remove(Airdrop airdrop) {
        String id = hologramIds.remove(airdrop.getId());
        if (id == null) return;
        // TODO: ลบ hologram จริงจาก DecentHolograms
    }
}
