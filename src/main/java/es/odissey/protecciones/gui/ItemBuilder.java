package es.odissey.protecciones.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helper to build {@link ItemStack}s used by the menu system.
 */
public class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material == null ? Material.STONE : material, Math.max(1, Math.min(64, amount)));
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (lore == null) return this;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(ColorUtil.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (!glow) return this;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder modelData(int modelData) {
        if (modelData < 0) return this;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(modelData);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder skullOwner(String playerName) {
        if (playerName == null || playerName.isEmpty()) return this;
        if (!(item.getItemMeta() instanceof SkullMeta)) return this;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(playerName);
        meta.setOwningPlayer(owner);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder flags(ItemFlag... itemFlags) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(itemFlags);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }
}
