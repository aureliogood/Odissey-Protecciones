package es.odissey.protecciones.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of a menu YAML file.
 */
public class MenuDefinition {

    public final String id;
    public final int size;
    public final String title;
    public final String openSound;

    public final boolean fillerEnabled;
    public final String fillerMaterial;
    public final String fillerName;
    public final int fillerModelData;

    public final Map<String, MenuButton> buttons = new LinkedHashMap<>();
    public final Map<String, MenuButton> templates = new LinkedHashMap<>();
    public final List<Integer> contentSlots = new ArrayList<>();

    public MenuDefinition(String id, File file) {
        this.id = id;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        int rawSize = Math.max(9, Math.min(54, yml.getInt("size", 27)));
        this.size = ((rawSize + 8) / 9) * 9; // redondear a multiplo de 9
        String configuredTitle = yml.getString("title", "&8" + id);
        this.title = configuredTitle.replace("&8[&e&lODISSEY&8]", "").trim();
        this.openSound = yml.getString("open-sound", null);

        ConfigurationSection filler = yml.getConfigurationSection("filler");
        if (filler != null) {
            this.fillerEnabled = filler.getBoolean("enabled", true);
            this.fillerMaterial = filler.getString("material", "GRAY_STAINED_GLASS_PANE");
            this.fillerName = filler.getString("name", "&r");
            this.fillerModelData = filler.getInt("custom-model-data", filler.getInt("model-data", -1));
        } else {
            this.fillerEnabled = false;
            this.fillerMaterial = "GRAY_STAINED_GLASS_PANE";
            this.fillerName = "&r";
            this.fillerModelData = -1;
        }

        for (int slot : yml.getIntegerList("content-slots")) {
            contentSlots.add(slot);
        }

        ConfigurationSection items = yml.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section == null) continue;
                MenuButton button = new MenuButton(key, section);
                if (button.isDynamic()) {
                    templates.put(key, button);
                } else {
                    buttons.put(key, button);
                }
            }
        }
    }

    public MenuButton getTemplate(String key) {
        return templates.get(key);
    }
}
