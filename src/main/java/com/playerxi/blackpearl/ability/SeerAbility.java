package com.playerxi.blackpearl.ability;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Vigilance (originally "Seer"): sneak while holding the sword to reveal nearby
 * entities through walls with Glowing.
 *
 * <p>Buffed twice over from the original design: 20 blocks - the old maximum -
 * is now the quick default, reached in well under a second, and sneaking a
 * little longer pushes the radius all the way out to 40. The channeling
 * debuff is gone entirely (it used to cost Slowness, Hunger and Weakness).</p>
 */
public final class SeerAbility {

    private static final int SENSING_START_TICK = 6; // 0.3s: warning cue, radius jumps to the first step

    /** {ticks after sensing starts, radius} - 20 blocks lands in under a second, 40 by ~2.3s. */
    private static final int[] STEP_TICKS = {0, 8, 16, 24, 32, 40};
    private static final double[] STEP_RADIUS = {10, 20, 25, 30, 35, 40};

    private static final int LINGER_TICKS = 100; // 5s of glow after you stop sneaking

    private final JavaPlugin plugin;
    private final Map<UUID, Channel> active = new HashMap<>();

    public SeerAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isChanneling(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    /** Begins channeling. Supply a check that returns false once channeling should stop. */
    public void start(Player player, Predicate<Player> shouldContinue) {
        if (active.containsKey(player.getUniqueId())) {
            return;
        }

        Channel channel = new Channel();

        BukkitRunnable runnable = new BukkitRunnable() {
            int elapsed = 0;
            int nextStep = 0;

            @Override
            public void run() {
                try {
                    tick();
                } catch (Exception e) {
                    plugin.getLogger().warning("Vigilance tick failed for " + player.getName()
                            + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    stop(player);
                }
            }

            private void tick() {
                if (!player.isOnline() || player.isDead() || !shouldContinue.test(player)) {
                    stop(player);
                    return;
                }

                elapsed++;

                if (elapsed == SENSING_START_TICK) {
                    player.sendActionBar(Component.text("Sensing...", NamedTextColor.AQUA));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.4f);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 1f);
                    player.getWorld().spawnParticle(Particle.NAUTILUS, player.getEyeLocation(), 12, 0.4, 0.4, 0.4, 0.1);
                }

                if (elapsed >= SENSING_START_TICK && nextStep < STEP_TICKS.length
                        && elapsed - SENSING_START_TICK >= STEP_TICKS[nextStep]) {
                    channel.radius = STEP_RADIUS[nextStep];
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    nextStep++;
                }

                if (channel.radius > 0) {
                    revealNearby(player, channel.radius, 4);
                }
            }
        };

        channel.task = runnable.runTaskTimer(plugin, 0L, 1L);
        active.put(player.getUniqueId(), channel);
    }

    public void stop(Player player) {
        Channel channel = active.remove(player.getUniqueId());
        if (channel == null) {
            return;
        }
        channel.task.cancel();

        // Let the reveal linger for a few seconds instead of cutting off instantly.
        if (channel.radius > 0 && player.isOnline()) {
            revealNearby(player, channel.radius, LINGER_TICKS);
        }
    }

    private void revealNearby(Player player, double radius, int glowDurationTicks) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity.getType() == EntityType.ARMOR_STAND) {
                continue;
            }
            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDurationTicks, 0, true, false, false));
        }
    }

    /** Small mutable holder so stop() can see the radius the runnable reached. */
    private static final class Channel {
        BukkitTask task;
        double radius = 0;
    }
}
