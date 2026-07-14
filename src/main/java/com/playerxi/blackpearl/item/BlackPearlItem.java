package com.playerxi.blackpearl.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Builds and identifies the Black Pearl sword.
 *
 * <p>The base item is a real {@code diamond_sword} (not the disguised
 * warped-fungus-on-a-stick the datapack used) so it behaves like a normal
 * Minecraft sword: sweep attacks work properly, and - importantly - it's
 * eligible for the "sword" enchantment category, so you can add your own
 * enchantments at an anvil/table. No enchantments are baked in.</p>
 *
 * <p>Stats are a completely plain, unmodified diamond sword - no custom
 * attribute modifiers at all. Durability is real (diamond's native 1561),
 * not Unbreakable - you can re-enchant/repair/whatever you like, same as any
 * other sword.</p>
 */
public final class BlackPearlItem {

    /** Resource pack namespace - must match the folder under assets/ in the resource pack. */
    public static final String NAMESPACE = "blackpearl";

    private final NamespacedKey markerKey;
    private final NamespacedKey itemModelKey;

    public BlackPearlItem(org.bukkit.plugin.Plugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "blackpearl_sword");
        this.itemModelKey = new NamespacedKey(NAMESPACE, "blackpearl");
    }

    public NamespacedKey markerKey() {
        return markerKey;
    }

    /** Builds a brand new Black Pearl ItemStack, fully configured. */
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Black Pearl", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("- - - - - - - - - - -", NamedTextColor.WHITE)
                        .decoration(TextDecoration.STRIKETHROUGH, true)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("It doesn't obey a captain - it makes one.", NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, true),
                Component.text("- - - - - - - - - - -", NamedTextColor.WHITE)
                        .decoration(TextDecoration.STRIKETHROUGH, true)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        // No baked-in enchantments and no custom attribute modifiers - this is
        // a plain diamond sword (7.0 attack damage / 1.6 attack speed,
        // whatever those numbers are on the live version) plus the special
        // abilities layered on top by the plugin's event listeners. Durability
        // is real (not Unbreakable), and you're free to enchant it however
        // you like at an anvil/table.

        // Unique legendary weapon - one per stack.
        meta.setMaxStackSize(1);

        // Point the client at our custom model (assets/blackpearl/models/item/blackpearl.json)
        meta.setItemModel(itemModelKey);

        // Hidden marker so we can reliably identify this exact item later.
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    /** Returns true if the given item is a Black Pearl sword. */
    public boolean isBlackPearl(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        Boolean flag = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(flag);
    }
}
