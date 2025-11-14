package xyz.kaijiieow.airdrop.services;

import xyz.kaijiieow.airdrop.AirdropPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class EffectService {

    private final AirdropPlugin plugin;

    public EffectService(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public void playSpawnEffect(Location loc) {
        loc.getWorld().playSound(
                loc,
                Sound.valueOf(plugin.getConfig().getString("sounds.spawn", "ENTITY_ENDER_DRAGON_GROWL")),
                1f, 1f
        );
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 1, 0.5), 10);
    }

    public void playSuccess(Location loc) {
        loc.getWorld().playSound(
                loc,
                Sound.valueOf(plugin.getConfig().getString("sounds.success", "ENTITY_PLAYER_LEVELUP")),
                1f, 1f
        );
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 1, 0.5), 15);
    }

    public void playDespawn(Location loc) {
        loc.getWorld().playSound(
                loc,
                Sound.valueOf(plugin.getConfig().getString("sounds.despawn", "ENTITY_GENERIC_EXPLODE")),
                1f, 1f
        );
        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0.5, 1, 0.5), 20);
    }
}
