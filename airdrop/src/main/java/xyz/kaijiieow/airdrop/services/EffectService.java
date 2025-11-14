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
import java.util.Locale;
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
        Location loc = airdrop.getLocation();
        if (loc == null) return;

        Particle baseParticle = resolveAmbientParticle();
        long interval = Math.max(1L, plugin.getConfig().getLong("effects.ambient-interval-ticks", 10L));
        Particle fireworksParticle = resolveOptionalParticle(Particle.FLAME, "FIREWORKS_SPARK");
        Particle swirlParticle = resolveOptionalParticle(baseParticle, "REDSTONE");
        Particle pulseParticle = resolveOptionalParticle(Particle.PORTAL, "TOTEM", "TOTEM_OF_UNDYING");
        Particle beamParticle = resolveOptionalParticle(Particle.PORTAL, "DUST_COLOR_TRANSITION", "BEACON", "BEACON_BEAM", "SPELL_WITCH");
        boolean beamIsTransition = "DUST_COLOR_TRANSITION".equalsIgnoreCase(beamParticle.name());
        DustTransition beamTransition = new DustTransition(Color.fromRGB(120, 235, 255), Color.fromRGB(255, 255, 255), 1.8f);
        boolean swirlSupportsDust = isDustParticle(swirlParticle);
        Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 208, 53), 1.2f);
        Particle.DustOptions aquaDust = new Particle.DustOptions(Color.AQUA, 0.9f);
        final double[] angle = {0};
        double swirlRadius = Math.max(0.6, plugin.getConfig().getDouble("effects.ambient-radius", 1.3));
        double beamHeight = Math.max(3.0, plugin.getConfig().getDouble("effects.beam-height", 5.0));

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
                world.spawnParticle(
                        baseParticle,
                        base.clone().add(0, 1.2, 0),
                        20,
                        0.25, 0.4, 0.25,
                        0.02
                );
                world.spawnParticle(fireworksParticle,
                        base.clone().add(0, 1.6, 0),
                        6,
                        0.2, 0.3, 0.2,
                        0.01);

                // swirling trails
                double height = 1.8;
                for (int i = 0; i < 3; i++) {
                    double phase = Math.toRadians(angle[0] + (i * 120));
                    double x = Math.cos(phase) * swirlRadius;
                    double z = Math.sin(phase) * swirlRadius;
                    double y = 0.3 + ((angle[0] + i * 30) % 360) / 360d * height;

                    Location swirl = base.clone().add(x, y, z);
                    if (swirlSupportsDust) {
                        world.spawnParticle(swirlParticle, swirl, 1, 0, 0, 0, 0, goldDust);
                        world.spawnParticle(swirlParticle, swirl.clone().add(0, 0.2, 0), 1, 0, 0, 0, 0, aquaDust);
                    } else {
                        world.spawnParticle(swirlParticle, swirl, 3, 0, 0, 0, 0.01);
                    }
                }

                // occasional totem pulse
                if ((angle[0] / 30) % 4 == 0) {
                    world.spawnParticle(pulseParticle, base.clone().add(0, 1.8, 0), 4, 0.2, 0.2, 0.2, 0.02);
                }

                // vertical beam to sky
                for (double y = 0; y <= beamHeight; y += 0.5) {
                    Location beamLoc = base.clone().add(0, y, 0);
                    if (beamIsTransition) {
                        world.spawnParticle(beamParticle, beamLoc, 1, 0, 0, 0, 0, beamTransition);
                    } else {
                        world.spawnParticle(beamParticle, beamLoc, 3, 0.15, 0.3, 0.15, 0.02);
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

    private Particle resolveAmbientParticle() {
        String name = plugin.getConfig().getString("effects.ambient-particle", "END_ROD");
        if (name == null || name.isEmpty()) {
            return Particle.END_ROD;
        }
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Particle.END_ROD;
        }
    }

    private Particle resolveOptionalParticle(Particle fallback, String... preferredNames) {
        for (String name : preferredNames) {
            try {
                return Particle.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return fallback;
    }

    private boolean isDustParticle(Particle particle) {
        String name = particle.name();
        return "REDSTONE".equalsIgnoreCase(name) || "DUST".equalsIgnoreCase(name);
    }
}
