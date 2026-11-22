package es.odissey.protecciones.gui;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

/**
 * Definition of a single configurable button inside a menu file. It is parsed from the YAML
 * and later turned into an {@link ItemStack} when the menu is opened.
 */
public class MenuButton {

    public final String key;
    public final int slot; // -1 means dynamic (assigned at runtime by the controller)
    public final String material;
    public final int amount;
    public final String name;
    public final List<String> lore;
    public final boolean glow;
    public final int modelData;
    public final String headOwner;
    public final String action;
    public final String leftAction;
    public final String rightAction;
    public final String sound;
    public final String permission;

    public MenuButton(String key, ConfigurationSection section) {
        this.key = key;
        this.slot = section.isSet("slot") && !section.getString("slot", "").equalsIgnoreCase("auto")
                ? section.getInt("slot", -1) : -1;
        this.material = section.getString("material", "STONE");
        this.amount = section.getInt("amount", 1);
        this.name = section.getString("name", "");
        this.lore = section.getStringList("lore");
        this.glow = section.getBoolean("glow", false);
        this.modelData = section.getInt("custom-model-data", section.getInt("model-data", -1));
        this.headOwner = section.getString("head-owner", null);
        this.action = section.getString("action", null);
        this.leftAction = section.getString("left-click", null);
        this.rightAction = section.getString("right-click", null);
        this.sound = section.getString("sound", null);
        this.permission = section.getString("permission", null);
    }

    public boolean isDynamic() {
        return slot < 0;
    }

    public List<String> loreOrEmpty() {
        return lore == null ? Collections.emptyList() : lore;
    }
}
