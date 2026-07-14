package com.playerxi.blackpearl.ability;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Endurance: while wielding the sword, standing in water or out in the rain
 * both grant Resistance II, Strength I, and a knockback resistance boost.
 * Vanilla has no potion effect for knockback resistance (it's an attribute,
 * not a status effect), so that part is applied as a toggled AttributeModifier
 * instead of a potion effect.
 *
 * <p>Each condition also gets its own extra perk on top of that shared base:
 * rain adds Speed III, water adds Dolphin's Grace III.</p>
 */
public final class EnduranceAbility {

    private static final double KNOCKBACK_RESISTANCE_BONUS = 0.6;

    private final NamespacedKey knockbackKey;
    private final Set<UUID> knockbackApplied = new HashSet<>();

    public EnduranceAbility(Plugin plugin) {
        this.knockbackKey = new NamespacedKey(plugin, "endurance_knockback_resistance");
    }

    /** Call periodically for a player holding the sword in their main hand. */
    public void update(Player player, boolean holdingSword) {
        boolean inWater = holdingSword && player.isInWater();
        boolean inRain = holdingSword && player.isInRain();
        boolean active = inWater || inRain;

        if (active) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 1, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, true, false, false));
            addKnockbackResistance(player);
        } else {
            removeKnockbackResistance(player);
        }

        if (inRain) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 2, true, false, false));
        }
        if (inWater) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 30, 2, true, false, false));
        }
    }

    public void forget(Player player) {
        removeKnockbackResistance(player);
    }

    private void addKnockbackResistance(Player player) {
        if (!knockbackApplied.add(player.getUniqueId())) {
            return; // already tracked as applied
        }
        AttributeInstance attribute = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute == null || attribute.getModifier(knockbackKey) != null) {
            return;
        }
        try {
            attribute.addModifier(new AttributeModifier(
                    knockbackKey, KNOCKBACK_RESISTANCE_BONUS, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        } catch (IllegalArgumentException e) {
            // Already present under the hood (e.g. persisted from a previous
            // server session before a restart) - nothing to do, just make sure
            // our tracking agrees so we don't keep retrying every tick.
        }
    }

    private void removeKnockbackResistance(Player player) {
        if (!knockbackApplied.remove(player.getUniqueId())) {
            return; // wasn't applied
        }
        AttributeInstance attribute = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            attribute.removeModifier(knockbackKey);
        }
    }
}
