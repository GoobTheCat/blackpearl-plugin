package com.playerxi.blackpearl.combat;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Deals "true" damage that bypasses armor, enchantments and resistance -
 * matching the original datapack's use of the {@code minecraft:out_of_world}
 * damage type for Execution's finishing blow (that damage type is tagged as
 * armor/enchantment-bypassing in vanilla's damage type registry).
 *
 * <p>Rather than routing through {@link LivingEntity#damage(double)} (which would
 * let armor and Resistance reduce it back down), this reduces absorption/health
 * directly, then plays the normal hurt animation and sound so it still feels and
 * sounds like a real hit.</p>
 */
public final class TrueDamage {

    private TrueDamage() {
    }

    public static void apply(LivingEntity target, double amount) {
        if (amount <= 0 || target.isDead()) {
            return;
        }

        double remaining = amount;

        // Absorption hearts soak up damage first, same as any other damage type.
        double absorption = target.getAbsorptionAmount();
        if (absorption > 0) {
            double consumed = Math.min(absorption, remaining);
            target.setAbsorptionAmount(absorption - consumed);
            remaining -= consumed;
        }

        if (remaining > 0) {
            double newHealth = Math.max(0.0, target.getHealth() - remaining);
            target.setHealth(newHealth);
        }

        target.playHurtAnimation(0f);
        Sound hurtSound = (target instanceof Player) ? Sound.ENTITY_PLAYER_HURT : Sound.ENTITY_GENERIC_HURT;
        target.getWorld().playSound(target.getLocation(), hurtSound, 1f, 1f);

        if (target.getHealth() <= 0.0 && target instanceof Player player) {
            // setHealth(0) doesn't always trigger the normal death flow on its own
            // for players in every server implementation, so nudge it explicitly.
            player.damage(0.0);
        }
    }
}
