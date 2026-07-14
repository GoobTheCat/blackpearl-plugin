package com.playerxi.blackpearl.ability;

import com.playerxi.blackpearl.combat.TargetManager;
import com.playerxi.blackpearl.combat.TrueDamage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Execution: mark up to 3 targets by hitting them, then right-click to unleash
 * 6 strikes - every strike lands on every currently-valid marked target at
 * once. 8 second cooldown, starts the moment you use it. The 6th strike deals
 * true damage that bypasses armor, with a bonus against drowned / guardians /
 * elder guardians.
 */
public final class ExecutionAbility {

    public static final long CHARGE_MS = 8000;
    private static final long CHARGE_TICKS = CHARGE_MS / 50; // 20 ticks/sec

    /** Ticks between each of the first 5 strikes, then a longer pause before the finisher. */
    private static final int[] STRIKE_DELAYS_TICKS = {0, 10, 20, 30, 40, 60};

    private static final double NORMAL_STRIKE_DAMAGE = 2.0;   // 1 heart, strikes 1-5
    private static final double FINISHER_BASE_DAMAGE = 4.0;   // 2 hearts, true damage
    private static final double FINISHER_BONUS_DAMAGE = 2.0;  // +1 heart vs aquatic threats
    private static final double COMBO_RADIUS = 10.0;          // titles/particles visible within this range

    public enum Result { FIRED, NOT_READY, NO_TARGET }

    private final JavaPlugin plugin;
    private final TargetManager targets;
    private final Cooldown cooldown = new Cooldown(CHARGE_MS);

    public ExecutionAbility(JavaPlugin plugin, TargetManager targets) {
        this.plugin = plugin;
        this.targets = targets;
    }

    public Cooldown cooldown() {
        return cooldown;
    }

    public boolean hasValidTarget(Player player) {
        return targets.hasAnyTarget(player);
    }

    /** Attempts to fire Execution. */
    public Result tryTrigger(Player player) {
        List<LivingEntity> currentTargets = targets.resolve(player);
        if (currentTargets.isEmpty()) {
            return Result.NO_TARGET;
        }
        if (!cooldown.isReady(player.getUniqueId())) {
            return Result.NOT_READY;
        }

        cooldown.consume(player.getUniqueId());

        List<UUID> targetIds = new ArrayList<>();
        for (LivingEntity target : currentTargets) {
            targetIds.add(target.getUniqueId());
        }
        runSequence(player.getUniqueId(), targetIds);

        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f);
            }
        }, CHARGE_TICKS);

        return Result.FIRED;
    }

    private void runSequence(UUID playerId, List<UUID> targetIds) {
        for (int i = 0; i < STRIKE_DELAYS_TICKS.length; i++) {
            int strikeNumber = i + 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    executeStrike(playerId, targetIds, strikeNumber);
                } catch (Exception e) {
                    plugin.getLogger().warning("Execution strike " + strikeNumber + " failed: "
                            + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }, STRIKE_DELAYS_TICKS[i]);
        }
    }

    private void executeStrike(UUID playerId, List<UUID> targetIds, int strikeNumber) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        List<LivingEntity> validTargets = new ArrayList<>();
        for (UUID targetId : targetIds) {
            if (!(Bukkit.getEntity(targetId) instanceof LivingEntity target) || target.isDead()) {
                continue;
            }
            if (!target.getWorld().equals(player.getWorld())
                    || target.getLocation().distance(player.getLocation()) > COMBO_RADIUS) {
                continue;
            }
            validTargets.add(target);
        }
        if (validTargets.isEmpty()) {
            return;
        }

        switch (strikeNumber) {
            case 1 -> flurryStrike(player, validTargets, "Game Over", true);
            case 2 -> flurryStrike(player, validTargets, "Game Over", false);
            case 3 -> flurryStrike(player, validTargets, "Elimination", true);
            case 4 -> flurryStrike(player, validTargets, "Elimination", false);
            case 5 -> flurryStrike(player, validTargets, "Perish", true);
            case 6 -> finisherStrike(player, validTargets);
            default -> throw new IllegalStateException("Unexpected strike number: " + strikeNumber);
        }
    }

    private void flurryStrike(Player player, List<LivingEntity> targetList, String text, boolean obfuscated) {
        for (LivingEntity target : targetList) {
            target.damage(NORMAL_STRIKE_DAMAGE, player);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
        }
        broadcastActionBar(player, text, obfuscated);
    }

    private void finisherStrike(Player player, List<LivingEntity> targetList) {
        for (LivingEntity target : targetList) {
            boolean aquaticBonus = target.getType() == EntityType.DROWNED
                    || target.getType() == EntityType.GUARDIAN
                    || target.getType() == EntityType.ELDER_GUARDIAN;

            double damage = FINISHER_BASE_DAMAGE + (aquaticBonus ? FINISHER_BONUS_DAMAGE : 0);
            TrueDamage.apply(target, damage);

            target.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1f, 0.6f);
            spawnFinisherParticles(target);
        }
        broadcastActionBar(player, "Perish", false);

        targets.clear(player);
    }

    private void spawnFinisherParticles(LivingEntity target) {
        // Stylised finishing flourish - an expanding ring plus an upward flash at
        // the target, standing in for the pixel-art particle drawing the original
        // datapack rendered along the player's view direction.
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        World world = target.getWorld();
        int points = 16;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = Math.cos(angle) * 1.2;
            double z = Math.sin(angle) * 1.2;
            world.spawnParticle(Particle.CRIT, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0, Color.WHITE);
        world.spawnParticle(Particle.SOUL, center, 12, 0.3, 0.6, 0.3, 0.02);
    }

    private void broadcastActionBar(Player player, String text, boolean obfuscated) {
        Component message = Component.text(text, NamedTextColor.DARK_RED)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.OBFUSCATED, obfuscated);

        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(player.getLocation()) <= COMBO_RADIUS) {
                nearby.sendActionBar(message);
            }
        }
    }
}
