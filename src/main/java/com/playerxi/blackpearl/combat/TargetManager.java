package com.playerxi.blackpearl.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks each player's marked Execution targets - up to {@link #MAX_TARGETS}
 * at once. Attacking something new adds it to the list; once you're at the
 * cap, the oldest mark is dropped to make room (a rolling FIFO window). Each
 * mark stays locked up to {@link #MAX_RANGE} blocks away (with a warning
 * particle past {@link #WARN_RANGE}), and auto-clears if it dies, changes
 * worlds, or you get too far away.
 */
public final class TargetManager {

    public static final int MAX_TARGETS = 3;
    public static final double WARN_RANGE = 7.0;
    public static final double MAX_RANGE = 12.0;

    private final Map<UUID, List<UUID>> targets = new HashMap<>();

    public void mark(Player player, LivingEntity target) {
        List<UUID> list = targets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        UUID targetId = target.getUniqueId();

        list.remove(targetId); // if already marked, move it to "most recent"
        list.add(targetId);
        while (list.size() > MAX_TARGETS) {
            list.remove(0); // drop the oldest mark
        }
    }

    public void clear(Player player) {
        targets.remove(player.getUniqueId());
    }

    /**
     * Resolves the player's current targets, enforcing the range rule and
     * automatically dropping any that are gone, dead, or too far away.
     * Returns an empty list if there are none.
     */
    public List<LivingEntity> resolve(Player player) {
        List<UUID> list = targets.get(player.getUniqueId());
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        List<LivingEntity> valid = new ArrayList<>();
        List<UUID> stillValid = new ArrayList<>();

        for (UUID id : list) {
            LivingEntity target = findLiving(player, id);
            if (target == null || target.isDead() || !target.getWorld().equals(player.getWorld())) {
                continue;
            }
            if (target.getLocation().distance(player.getLocation()) > MAX_RANGE) {
                continue;
            }
            valid.add(target);
            stillValid.add(id);
        }

        if (stillValid.isEmpty()) {
            targets.remove(player.getUniqueId());
        } else {
            targets.put(player.getUniqueId(), stillValid);
        }

        return valid;
    }

    public boolean hasAnyTarget(Player player) {
        return !resolve(player).isEmpty();
    }

    /** Spawns the marker particle on a target - wax-on up close, crimson spore fading out further away. */
    public void showMarkerParticle(Player player, LivingEntity target) {
        double distance = target.getLocation().distance(player.getLocation());
        Location loc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        Particle particle = (distance <= WARN_RANGE) ? Particle.WAX_ON : Particle.CRIMSON_SPORE;
        target.getWorld().spawnParticle(particle, loc, 3, 0.3, 0.3, 0.3, 0.0);
    }

    public void forget(Player player) {
        targets.remove(player.getUniqueId());
    }

    private LivingEntity findLiving(Player player, UUID id) {
        var entity = player.getServer().getEntity(id);
        return (entity instanceof LivingEntity living) ? living : null;
    }
}
