package com.playerxi.blackpearl.listener;

import com.playerxi.blackpearl.ability.EnduranceAbility;
import com.playerxi.blackpearl.ability.ExecutionAbility;
import com.playerxi.blackpearl.ability.HydrasWillAbility;
import com.playerxi.blackpearl.ability.SeerAbility;
import com.playerxi.blackpearl.combat.TargetManager;
import com.playerxi.blackpearl.item.BlackPearlItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wires every ability to real events.
 *
 * <p>Right-click with a target marked fires Execution. Sneaking (Vigilance)
 * starts instantly the moment you hold sneak with the sword out.</p>
 */
public final class BlackPearlListener implements Listener {

    private final JavaPlugin plugin;
    private final BlackPearlItem item;
    private final TargetManager targets;
    private final ExecutionAbility execution;
    private final SeerAbility seer;
    private final HydrasWillAbility hydrasWill;
    private final EnduranceAbility endurance;

    public BlackPearlListener(JavaPlugin plugin, BlackPearlItem item, TargetManager targets,
                              ExecutionAbility execution, SeerAbility seer,
                              HydrasWillAbility hydrasWill, EnduranceAbility endurance) {
        this.plugin = plugin;
        this.item = item;
        this.targets = targets;
        this.execution = execution;
        this.seer = seer;
        this.hydrasWill = hydrasWill;
        this.endurance = endurance;
    }

    private boolean holdingSword(Player player) {
        return item.isBlackPearl(player.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid double-firing for the off-hand
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!holdingSword(player)) {
            return;
        }

        if (execution.hasValidTarget(player)) {
            ExecutionAbility.Result result = execution.tryTrigger(player);
            if (result == ExecutionAbility.Result.NOT_READY) {
                feedback(player, "Not ready yet...", Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO);
            }
            // FIRED is handled entirely inside ExecutionAbility.
        } else {
            feedback(player, "No Targets", Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO);
        }
    }

    private void feedback(Player player, String text, Sound sound) {
        player.sendActionBar(Component.text(text, NamedTextColor.GRAY));
        player.getWorld().playSound(player.getLocation(), sound, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || event.getFinalDamage() <= 0) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!holdingSword(player)) {
            return;
        }

        targets.mark(player, victim);
        hydrasWill.registerHit(player);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && holdingSword(player)) {
            seer.start(player, p -> p.isSneaking() && holdingSword(p));
        } else if (!event.isSneaking()) {
            seer.stop(player);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!item.isBlackPearl(event.getRecipe().getResult())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> celebrate(player));
    }

    private void celebrate(Player player) {
        // Above the hotbar, not a full-screen title - same spirit as the
        // original's advancement-style announcement, minus the screen takeover.
        player.sendActionBar(Component.text("Shiver me timbers! ", NamedTextColor.AQUA)
                .append(Component.text("Obtained the Black Pearl", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        targets.forget(player);
        hydrasWill.forget(player);
        endurance.forget(player);
        execution.cooldown().forget(player.getUniqueId());
        seer.stop(player);
    }
}
