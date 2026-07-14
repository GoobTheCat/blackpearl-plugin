package com.playerxi.blackpearl.ability;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hydra's Will: every 5th successful hit with the sword heals you.
 *
 * <p>The original datapack checked for a damage-dealt stat spike of at least 7
 * points that tick as a proxy for "you just hit something with the sword" (since
 * the sword's own base damage already clears that bar). In the plugin we know
 * directly which weapon dealt the hit, so every landed hit with the sword counts
 * - same outcome, simpler check.</p>
 */
public final class HydrasWillAbility {

    private static final int HITS_TO_HEAL = 5;

    private final Map<UUID, Integer> hitStreak = new HashMap<>();

    /** Call this once for every successful hit the player lands using the sword. */
    public void registerHit(Player player) {
        int streak = hitStreak.merge(player.getUniqueId(), 1, Integer::sum);
        if (streak >= HITS_TO_HEAL) {
            heal(player);
            hitStreak.put(player.getUniqueId(), 0);
        }
    }

    private void heal(Player player) {
        // Instant Health I = 2 HP x 2^1 = 4 HP (2 hearts), matching the original
        // "effect give @s instant_health 1 0 true" exactly, including the vanilla
        // heal particle burst that comes for free with the potion effect.
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0));
    }

    public void forget(Player player) {
        hitStreak.remove(player.getUniqueId());
    }
}
