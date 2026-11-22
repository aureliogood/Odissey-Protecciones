package es.odissey.protecciones.compat;

import es.odissey.protecciones.PSProtectBlock;
import es.odissey.protecciones.utils.config.YamlConfigBinder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee los bloques de proteccion del plugin AxoProtectionBlock y los convierte al
 * formato interno de OdisseyProtecciones.
 */
public final class AxoBlockAdapter {

    private static final String[] AXO_FLAG_ORDER = {
            "EXPLOSIONS", "PVP", "LIQUID_FLOW", "PISTONS", "ENTRY", "INVINCIBLE",
            "MOB_DAMAGE", "FIRE_SPREAD", "LIGHTER", "TRAMPLE_BLOCKS", "CREEPER_EXPLOSION",
            "WITHER_DAMAGE", "GHAST_FIREBALL", "MOB_SPAWNING", "ENTITY_DAMAGE"
    };

    private static final Map<String, Boolean> AXO_DEFAULTS = new LinkedHashMap<>();

    static {
        AXO_DEFAULTS.put("PVP", false);
        AXO_DEFAULTS.put("EXPLOSIONS", false);
        AXO_DEFAULTS.put("LIQUID_FLOW", true);
        AXO_DEFAULTS.put("PISTONS", true);
        AXO_DEFAULTS.put("ENTRY", true);
        AXO_DEFAULTS.put("INVINCIBLE", false);
        AXO_DEFAULTS.put("MOB_DAMAGE", false);
        AXO_DEFAULTS.put("FIRE_SPREAD", false);
        AXO_DEFAULTS.put("LIGHTER", false);
        AXO_DEFAULTS.put("TRAMPLE_BLOCKS", false);
        AXO_DEFAULTS.put("CREEPER_EXPLOSION", false);
        AXO_DEFAULTS.put("WITHER_DAMAGE", false);
        AXO_DEFAULTS.put("GHAST_FIREBALL", false);
        AXO_DEFAULTS.put("MOB_SPAWNING", false);
        AXO_DEFAULTS.put("ENTITY_DAMAGE", false);
    }

    private AxoBlockAdapter() {
    }

    public static boolean isAxoBlock(ConfigurationSection c) {
        return c.isConfigurationSection("block-data")
                || c.isConfigurationSection("region.home-offset")
                || (c.isConfigurationSection("region.flags") && !c.isList("region.flags"));
    }

    public static PSProtectBlock convert(YamlConfiguration axo, YamlConfiguration template, String fileName) {
        YamlConfiguration c = new YamlConfiguration();
        copyTemplate(template, c);

        c.set("type", resolveType(axo));
        String alias = axo.getString("alias", strip(fileName.replaceFirst("\\.yml$", "")));
        c.set("alias", alias == null ? "" : alias.replaceAll("\\s+", "_"));
        c.set("description", strip(axo.getString("description", "")));
        c.set("restrict_obtaining", axo.getBoolean("restrict-obtaining", true));

        ConfigurationSection worlds = axo.getConfigurationSection("worlds");
        if (worlds != null) {
            String listType = worlds.getString("list-type", "blacklist");
            c.set("world_list_type", listType == null ? "blacklist" : listType.toLowerCase());
            c.set("worlds", new ArrayList<>(worlds.getStringList("list")));
        }
        c.set("prevent_block_place_in_restricted_world",
                axo.getBoolean("prevent-block-place-in-restricted-world", true));
        c.set("allow_placing_in_wild", axo.getBoolean("allow-placing-in-wild", true));

        c.set("region.x_radius", axo.getInt("region.x_radius", 10));
        c.set("region.y_radius", axo.getInt("region.y_radius", -1));
        c.set("region.z_radius", axo.getInt("region.z_radius", 10));

        ConfigurationSection home = axo.getConfigurationSection("region.home-offset");
        if (home != null) {
            c.set("region.home_x_offset", home.getDouble("x", 0.5));
            c.set("region.home_y_offset", home.getDouble("y", 1.0));
            c.set("region.home_z_offset", home.getDouble("z", 0.5));
        }

        c.set("region.flags", buildFlags(axo, c.getStringList("region.flags")));

        ConfigurationSection blockData = axo.getConfigurationSection("block-data");
        if (blockData != null) {
            if (blockData.isString("display-name")) {
                c.set("block_data.display_name", strip(blockData.getString("display-name")));
            }
            List<String> lore = new ArrayList<>();
            for (String line : blockData.getStringList("lore")) lore.add(strip(line));
            c.set("block_data.lore", lore);
            c.set("block_data.enchanted_effect", blockData.getBoolean("enchanted-effect", false));
            c.set("block_data.custom_model_data", blockData.getInt("custom-model-data", -1));

            ConfigurationSection recipe = blockData.getConfigurationSection("recipe");
            if (recipe != null) {
                c.set("block_data.allow_craft_with_custom_recipe", recipe.getBoolean("enabled", false));
                c.set("block_data.recipe_amount", recipe.getInt("amount", 1));
                List<List<String>> shape = readRecipe(recipe);
                if (!shape.isEmpty()) c.set("block_data.custom_recipe", shape);
            }
        }

        double price = axo.getDouble("shop.price", c.getDouble("block_data.price", 0.0));
        c.set("block_data.price", price);

        ConfigurationSection shop = axo.getConfigurationSection("shop");
        if (shop != null) {
            c.set("shop.enabled", shop.getBoolean("enabled", true));
            ConfigurationSection refund = shop.getConfigurationSection("refund");
            if (refund != null) {
                c.set("shop.refund_enabled", refund.getBoolean("enabled", true));
            }
        }

        ConfigurationSection behaviour = axo.getConfigurationSection("behaviour");
        if (behaviour != null) {
            c.set("behaviour.auto_hide", behaviour.getBoolean("auto-hide", false));
            c.set("behaviour.auto_merge", behaviour.getBoolean("auto-merge", false));
            c.set("behaviour.no_drop", behaviour.getBoolean("no-drop", false));
            c.set("behaviour.prevent_piston_push", behaviour.getBoolean("prevent-piston-push", true));
            c.set("behaviour.prevent_explode", behaviour.getBoolean("prevent-explode", true));
            c.set("behaviour.destroy_region_when_explode", behaviour.getBoolean("destroy-region-when-explode", false));
            c.set("behaviour.prevent_silk_touch", behaviour.getBoolean("prevent-silk-touch", false));
            c.set("behaviour.cost_to_place", behaviour.getDouble("cost-to-place", 0.0));
            c.set("behaviour.allow_smelt_item", behaviour.getBoolean("allow-smelt-item", false));
            c.set("behaviour.allow_use_in_crafting", behaviour.getBoolean("allow-use-in-crafting", false));
        }

        ConfigurationSection player = axo.getConfigurationSection("player");
        if (player != null) {
            c.set("player.allow_shift_right_break", player.getBoolean("allow-shift-right-break", false));
            c.set("player.prevent_teleport_in", player.getBoolean("prevent-teleport-in", false));
            c.set("player.no_moving_when_tp_waiting", player.getBoolean("no-moving-when-tp-waiting", true));
            c.set("player.tp_waiting_seconds", player.getInt("tp-waiting-seconds", 0));
            c.set("player.prevent_ps_get", player.getBoolean("prevent-protection-get", false));
            c.set("player.prevent_ps_home", player.getBoolean("prevent-protection-home", false));
            c.set("player.permission", player.getString("permission", ""));
        }

        ConfigurationSection event = axo.getConfigurationSection("event");
        if (event != null) {
            c.set("event.enable", event.getBoolean("enable", false));
            c.set("event.on_region_create", new ArrayList<>(event.getStringList("on-region-create")));
            c.set("event.on_region_destroy", new ArrayList<>(event.getStringList("on-region-destroy")));
        }

        return YamlConfigBinder.bind(c, PSProtectBlock::new);
    }

    private static String resolveType(ConfigurationSection axo) {
        String raw = axo.getString("type", "PLAYER_HEAD");
        String material = raw == null || raw.trim().isEmpty() ? "PLAYER_HEAD" : raw.trim().toUpperCase();
        if (!material.equals("PLAYER_HEAD")) return material;

        String texture = axo.getString("block-data.head.texture", "");
        if (texture != null && !texture.isEmpty()) {
            return "PLAYER_HEAD:" + texture;
        }

        String owner = axo.getString("block-data.head.owner", "");
        if (owner != null && !owner.isEmpty() && !owner.contains("%")) {
            return "PLAYER_HEAD:" + owner;
        }
        return material;
    }

    private static List<String> buildFlags(ConfigurationSection axo, List<String> fallback) {
        ConfigurationSection flags = axo.getConfigurationSection("region.flags");
        LinkedHashMap<String, String> out = new LinkedHashMap<>();

        // fly-flag -> allow-flight (aunque el bloque no tenga seccion flags)
        ConfigurationSection fly = axo.getConfigurationSection("region.fly-flag");
        if (fly != null && fly.getBoolean("enabled", false)) {
            out.put("allow-flight", fly.getBoolean("fly", false) ? "allow" : "deny");
        }

        if (flags == null) {
            if (out.isEmpty()) return fallback;
            List<String> only = new ArrayList<>();
            for (Map.Entry<String, String> e : out.entrySet()) only.add(e.getKey() + " " + e.getValue());
            return only;
        }

        for (String axoName : AXO_FLAG_ORDER) {
            Boolean state = resolveState(flags.getString(axoName), AXO_DEFAULTS.get(axoName));
            if (state == null) continue;
            String value = state ? "allow" : "deny";
            switch (axoName) {
                case "EXPLOSIONS":
                    out.put("tnt", value);
                    out.put("creeper-explosion", value);
                    out.put("other-explosion", value);
                    break;
                case "PVP":
                    out.put("pvp", value);
                    break;
                case "LIQUID_FLOW":
                    out.put("water-flow", value);
                    out.put("lava-flow", value);
                    break;
                case "PISTONS":
                    out.put("pistons", value);
                    break;
                case "ENTRY":
                    out.put("entry", value);
                    break;
                case "INVINCIBLE":
                    out.put("invincible", value);
                    break;
                case "MOB_DAMAGE":
                    out.put("mob-damage", value);
                    break;
                case "FIRE_SPREAD":
                    out.put("fire-spread", value);
                    break;
                case "LIGHTER":
                    out.put("lighter", value);
                    break;
                case "TRAMPLE_BLOCKS":
                    out.put("block-trampling", value);
                    break;
                case "CREEPER_EXPLOSION":
                    out.put("creeper-explosion", value);
                    break;
                case "WITHER_DAMAGE":
                    out.put("wither-damage", value);
                    break;
                case "GHAST_FIREBALL":
                    out.put("ghast-fireball", value);
                    break;
                case "MOB_SPAWNING":
                    out.put("mob-spawning", value);
                    break;
                case "ENTITY_DAMAGE":
                    out.put("damage-animals", value);
                    break;
                default:
                    break;
            }
        }

        ConfigurationSection messages = axo.getConfigurationSection("region.messages");
        if (messages != null) {
            addMessage(out, "greeting", messages.getString("enter"));
            addMessage(out, "farewell", messages.getString("leave"));
        }

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> e : out.entrySet()) {
            list.add(e.getKey() + " " + e.getValue());
        }
        return list;
    }

    private static void addMessage(LinkedHashMap<String, String> out, String flag, String message) {
        if (message == null || message.trim().isEmpty()) return;
        String value = strip(message).replace("%owner%", "%player%").replace("%prefix%", "").trim();
        if (!value.isEmpty()) out.put(flag, value);
    }

    private static Boolean resolveState(String raw, Boolean defaultValue) {
        if (raw == null) return defaultValue;
        String v = raw.trim().toUpperCase();
        if (v.equals("ALLOW") || v.equals("TRUE") || v.equals("ENABLED")) return true;
        if (v.equals("DENY") || v.equals("FALSE") || v.equals("DISABLED")) return false;
        return defaultValue;
    }

    private static List<List<String>> readRecipe(ConfigurationSection recipe) {
        List<List<String>> out = new ArrayList<>();
        List<?> rows = recipe.getList("shape");
        if (rows == null) return out;
        for (Object row : rows) {
            List<String> line = new ArrayList<>();
            if (row instanceof List) {
                for (Object cell : (List<?>) row) {
                    line.add(cell == null ? "" : String.valueOf(cell));
                }
            }
            out.add(line);
        }
        return out;
    }

    private static void copyTemplate(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(true)) {
            if (from.isConfigurationSection(key)) continue;
            to.set(key, from.get(key));
        }
    }

    private static String strip(String value) {
        return value == null ? "" : value.replaceAll("<[^>]+>", "");
    }
}
