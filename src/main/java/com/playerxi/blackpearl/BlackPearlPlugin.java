package com.playerxi.blackpearl;

import com.playerxi.blackpearl.ability.EnduranceAbility;
import com.playerxi.blackpearl.ability.ExecutionAbility;
import com.playerxi.blackpearl.ability.HydrasWillAbility;
import com.playerxi.blackpearl.ability.SeerAbility;
import com.playerxi.blackpearl.combat.TargetManager;
import com.playerxi.blackpearl.item.BlackPearlItem;
import com.playerxi.blackpearl.listener.BlackPearlListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlackPearlPlugin extends JavaPlugin {

    private BlackPearlItem item;
    private TargetManager targetManager;
    private ExecutionAbility execution;
    private SeerAbility seer;
    private HydrasWillAbility hydrasWill;
    private EnduranceAbility endurance;

    @Override
    public void onEnable() {
        this.item = new BlackPearlItem(this);
        this.targetManager = new TargetManager();
        this.execution = new ExecutionAbility(this, targetManager);
        this.seer = new SeerAbility(this);
        this.hydrasWill = new HydrasWillAbility();
        this.endurance = new EnduranceAbility(this);

        getServer().getPluginManager().registerEvents(
                new BlackPearlListener(this, item, targetManager, execution, seer, hydrasWill, endurance),
                this);

        registerRecipe();
        startAmbientTask();

        // Matches the original datapack's load-time announcement.
        Component announcement = Component.text(
                "The king of swords has chosen it's new owner...", NamedTextColor.WHITE);
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendMessage(announcement);
        }

        getLogger().info("Black Pearl loaded - Execution, Endurance, Vigilance, Hydra's Will.");
    }

    private void registerRecipe() {
        NamespacedKey key = new NamespacedKey(this, "pearl_craft");
        ShapedRecipe recipe = new ShapedRecipe(key, item.create());
        recipe.shape("  #", "NG ", "IN ");
        recipe.setIngredient('#', new RecipeChoice.MaterialChoice(Material.HEART_OF_THE_SEA));
        recipe.setIngredient('N', new RecipeChoice.MaterialChoice(Material.DIAMOND));
        recipe.setIngredient('G', new RecipeChoice.MaterialChoice(Material.GOLD_BLOCK));
        recipe.setIngredient('I', new RecipeChoice.MaterialChoice(Material.IRON_SWORD));
        getServer().addRecipe(recipe);
    }

    /** One ticking task drives Endurance's water/rain check and target marker particles. */
    private void startAmbientTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                try {
                    boolean held = item.isBlackPearl(player.getInventory().getItemInMainHand());
                    endurance.update(player, held);

                    for (LivingEntity target : targetManager.resolve(player)) {
                        targetManager.showMarkerParticle(player, target);
                    }
                } catch (Exception e) {
                    getLogger().warning("Ambient tick failed for " + player.getName()
                            + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
        }, 5L, 5L);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                              String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("blackpearl")) {
            return false;
        }

        Player target;
        if (args.length >= 1) {
            target = getServer().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Console must specify a player.", NamedTextColor.RED));
            return true;
        }

        ItemStack sword = item.create();
        target.getInventory().addItem(sword);
        target.sendMessage(Component.text("The Black Pearl has been placed in your inventory.", NamedTextColor.GOLD));
        return true;
    }
}
