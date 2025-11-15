package xyz.kaijiieow.airdrop.services;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xyz.kaijiieow.airdrop.AirdropPlugin;
import xyz.kaijiieow.airdrop.core.Airdrop;
import xyz.kaijiieow.airdrop.core.AirdropState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectService {

    private final AirdropPlugin plugin;
    private final Map<UUID, BukkitTask> ambientTasks = new HashMap<>();

    public EffectService(AirdropPlugin plugin) {
        this.plugin = plugin;
    }

    public void playSpawnEffect(Location loc) {
        playSound(loc, plugin.getConfig().getString("sounds.spawn", "ENTITY_ENDER_DRAGON_GROWL"));
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 1, 0.5), 10);
    }

    public void playSuccess(Location loc) {
        playSound(loc, plugin.getConfig().getString("sounds.success", "ENTITY_PLAYER_LEVELUP"));
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1, 0.5), 15);
    }

    public void playDespawn(Location loc) {
        playSound(loc, plugin.getConfig().getString("sounds.despawn", "ENTITY_GENERIC_EXPLODE"));
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0.5, 1, 0.5), 20);
    }

    private void playSound(Location loc, String name) {
        if (loc == null || loc.getWorld() == null) return;
        try {
            loc.getWorld().playSound(loc, Sound.valueOf(name), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void startAmbientEffect(Airdrop airdrop) {
        stopAmbientEffect(airdrop);
        var config = plugin.getConfig();
        if (!config.getBoolean("effects.ambient-enabled", true)) {
            return;
        }

        Location loc = airdrop.getLocation();
        if (loc == null) return;

        final boolean coreEnabled = config.getBoolean("effects.sections.core-burst", true);
        final boolean swirlEnabled = config.getBoolean("effects.sections.swirl-trails", true);
        final boolean pulseEnabled = config.getBoolean("effects.sections.pulse", true);
        final boolean beamEnabled = config.getBoolean("effects.sections.beam", true);
        if (!coreEnabled && !swirlEnabled && !pulseEnabled && !beamEnabled) {
            return;
        }

        long interval = Math.max(1L, config.getLong("effects.ambient-interval-ticks", 10L));
        Color primaryColor = resolveColor("effects.primary-color", Color.fromRGB(192, 32, 32));
        Color secondaryColor = resolveColor("effects.secondary-color", Color.fromRGB(255, 94, 0));
        Color accentColor = resolveColor("effects.accent-color", Color.fromRGB(255, 205, 102));
        Particle.DustOptions primaryDust = new Particle.DustOptions(primaryColor, 1.5f);
        Particle.DustOptions secondaryDust = new Particle.DustOptions(secondaryColor, 1.1f);
        DustTransition swirlTransition = new DustTransition(primaryColor, secondaryColor, 1.3f);
        DustTransition pulseTransition = new DustTransition(secondaryColor, accentColor, 1.2f);
        DustTransition beamTransition = new DustTransition(primaryColor, accentColor, 1.9f);
        final double[] angle = {0};
        double swirlRadius = Math.max(0.6, config.getDouble("effects.ambient-radius", 1.3));
        double beamHeight = Math.max(3.0, config.getDouble("effects.beam-height", 5.0));

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (airdrop.getState() == AirdropState.DESPAWNED) {
                    cancel();
                    ambientTasks.remove(airdrop.getId());
                    return;
                }
                Location current = airdrop.getLocation();
                if (current == null || current.getWorld() == null) {
                    cancel();
                    ambientTasks.remove(airdrop.getId());
                    return;
                }
                var world = current.getWorld();
                Location base = current.clone().add(0.5, 0.2, 0.5);

                // core burst
                if (coreEnabled) {
                    world.spawnParticle(
                            Particle.DUST,
                            base.clone().add(0, 1.2, 0),
                            28,
                            0.3, 0.45, 0.3,
                            0,
                            primaryDust
                    );
                    world.spawnParticle(Particle.FLAME,
                            base.clone().add(0, 1.6, 0),
                            6,
                            0.2, 0.3, 0.2,
                            0.01);
                    world.spawnParticle(Particle.CRIT,
                            base.clone().add(0, 1.35, 0),
                            8,
                            0.25, 0.25, 0.25,
                            0.02);
                }

                // swirling trails
                if (swirlEnabled) {
                    double height = 1.8;
                    for (int i = 0; i < 3; i++) {
                        double phase = Math.toRadians(angle[0] + (i * 120));
                        double x = Math.cos(phase) * swirlRadius;
                        double z = Math.sin(phase) * swirlRadius;
                        double y = 0.3 + ((angle[0] + i * 30) % 360) / 360d * height;

                        Location swirl = base.clone().add(x, y, z);
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, swirl, 1, 0, 0, 0, 0, swirlTransition);
                        world.spawnParticle(Particle.DUST, swirl.clone().add(0, 0.18, 0), 1, 0, 0, 0, 0, secondaryDust);
                    }
                }

                // occasional totem pulse
                if (pulseEnabled && (angle[0] / 20) % 4 == 0) {
                    world.spawnParticle(Particle.DUST_COLOR_TRANSITION, base.clone().add(0, 1.8, 0), 2, 0.2, 0.2, 0.2, 0, pulseTransition);
                    world.spawnParticle(Particle.LAVA, base.clone().add(0, 1.85, 0), 2, 0.1, 0.15, 0.1, 0.01);
                }

                // vertical beam to sky
                if (beamEnabled) {
                    for (double y = 0; y <= beamHeight; y += 0.5) {
                        Location beamLoc = base.clone().add(0, y, 0);
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, beamLoc, 1, 0, 0, 0, 0, beamTransition);
                        world.spawnParticle(Particle.DUST, beamLoc, 1, 0.12, 0.18, 0.12, 0, secondaryDust);
                    }
                }

                angle[0] = (angle[0] + 15) % 360;
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, 0L, interval);
        ambientTasks.put(airdrop.getId(), task);
    }

    public void stopAmbientEffect(Airdrop airdrop) {
        BukkitTask task = ambientTasks.remove(airdrop.getId());
        if (task != null) {
            task.cancel();
        }
    }

    private Color resolveColor(String path, Color fallback) {
        String raw = plugin.getConfig().getString(path);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        raw = raw.trim();
        try {
            if (raw.startsWith("#")) {
                raw = raw.substring(1);
            }
            if (raw.contains(",")) {
                String[] parts = raw.split(",");
                int r = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
                int g = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                int b = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                return Color.fromRGB(clampColor(r), clampColor(g), clampColor(b));
            }
            int rgb = Integer.parseInt(raw, 16);
            return Color.fromRGB(rgb);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid color '" + raw + "' configured at " + path + ", using fallback.");
            return fallback;
        }
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
