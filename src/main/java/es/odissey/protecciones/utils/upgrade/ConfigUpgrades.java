package es.odissey.protecciones.utils.upgrade;

import es.odissey.protecciones.OdisseyProtecciones;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigUpgrades {
    // Upgrade the config one version up (ex. 3 -> 4)
    public static boolean doConfigUpgrades() {
        boolean leaveLoop = false;
        switch (OdisseyProtecciones.getInstance().getConfigOptions().configVersion) {
            case 3:
                OdisseyProtecciones.config.set("config_version", 4);
                OdisseyProtecciones.config.set("base_command", "ps");
                OdisseyProtecciones.config.set("aliases", Arrays.asList("oproyecto", "oproteccion"));
                break;
            case 4:
                OdisseyProtecciones.config.set("config_version", 5);
                OdisseyProtecciones.config.set("async_load_uuid_cache", false);
                OdisseyProtecciones.config.set("ps_view_cooldown", 20);
                break;
            case 5:
                OdisseyProtecciones.config.set("config_version", 6);
                OdisseyProtecciones.config.set("allow_duplicate_region_names", false);
                break;
            case 6:
                OdisseyProtecciones.config.set("config_version", 7);
                OdisseyProtecciones.config.set("drop_item_when_inventory_full", true);
                break;
            case 7:
                OdisseyProtecciones.config.set("config_version", 8);
                OdisseyProtecciones.config.set("regions_must_be_adjacent", false);
                break;
            case 8:
                OdisseyProtecciones.config.set("config_version", 9);
                OdisseyProtecciones.config.set("allow_merging_regions", true);
                break;
            case 9:
                OdisseyProtecciones.config.set("config_version", 10);
                OdisseyProtecciones.config.set("allow_merging_holes", true);
                break;
            case 10:
                OdisseyProtecciones.config.set("config_version", 11);
                for (File file : blockFiles()) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
                    setComment(c, "type", " Define your protection block below\n" +
                            " Use block type from here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html\n" +
                            " --------------------------------------------------------------------------------------------------\n" +
                            " If you want to use player heads, you can use \"PLAYER_HEAD:player_name\" (ex. \"PLAYER_HEAD:Notch\")\n" +
                            " To use custom player heads, you need the base64 value of the head. On minecraft-heads.com, you will find this value in the Other section under \"Value:\".\n" +
                            " To use UUIDs for player heads, go to https://sessionserver.mojang.com/session/minecraft/profile/PUT-UUID-HERE and copy the value from the \"value\" field not including quotes.\n" +
                            " When you have the value, you can set the type to \"PLAYER_HEAD:value\"");

                    try {
                        c.set("region.home_x_offset", ((Number) c.get("region.home_x_offset")).doubleValue());
                        c.set("region.home_y_offset", ((Number) c.get("region.home_y_offset")).doubleValue());
                        c.set("region.home_z_offset", ((Number) c.get("region.home_z_offset")).doubleValue());
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("No se pudo convertir home offsets en " + file.getName() + ": " + e.getMessage());
                    }
                    save(c, file);
                }
                break;
            case 11:
                OdisseyProtecciones.config.set("config_version", 12);
                OdisseyProtecciones.config.set("economy.max_rent_price", -1.0);
                OdisseyProtecciones.config.set("economy.min_rent_price", 1.0);
                setComment(OdisseyProtecciones.config, "economy.max_rent_price", " Set limits on the price for renting. Set to -1.0 to disable.");
                OdisseyProtecciones.config.set("economy.max_rent_period", -1);
                OdisseyProtecciones.config.set("economy.min_rent_period", 1);
                setComment(OdisseyProtecciones.config, "economy.max_rent_period", " Set limits on the period between rent payments, in seconds (86400 seconds = 1 day). Set to -1 to disable.");
                OdisseyProtecciones.config.set("economy.tax_enabled", false);
                OdisseyProtecciones.config.set("economy.tax_message_on_join", true);
                setComment(OdisseyProtecciones.config, "economy.tax_enabled", " Set taxes on regions.\n" +
                        " Taxes are configured in each individual block config.\n" +
                        " Whether or not to enable the tax command.");

                for (File file : blockFiles()) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
                    try {
                        List<String> l = new ArrayList<>(c.getStringList("region.hidden_flags_from_info"));
                        l.addAll(Arrays.asList("ps-rent-settings", "ps-tax-payments-due", "ps-tax-last-payment-added", "ps-tax-autopayer", "ps-member-perms"));
                        c.set("region.hidden_flags_from_info", l);
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("No se pudo actualizar hidden_flags_from_info en " + file.getName() + ": " + e.getMessage());
                    }
                    save(c, file);
                }
                break;
            case 12:
                OdisseyProtecciones.config.set("config_version", 13);
                OdisseyProtecciones.config.set("default_protection_block_placement_off", false);
                setComment(OdisseyProtecciones.config, "default_protection_block_placement_off", " Whether when players join, by default they have protection block placement toggled off (equivalent to running /ps toggle)");
                OdisseyProtecciones.config.set("default_allow_addowner_for_offline_players_without_lp", false);
                setComment(OdisseyProtecciones.config, "default_allow_addowner_for_offline_players_without_lp", " If you do not have LuckPerms, OdisseyProtecciones is unable to determine the limits of offline players (since it depends\n" +
                        " on permissions), and so it requires players to be online. Set this to true if your server does not need limits (and so\n" +
                        " the check is unnecessary).");
                break;
            case 13:
                OdisseyProtecciones.config.set("config_version", 14);
                OdisseyProtecciones.config.set("admin.cleanup_delete_regions_with_members_but_no_owners", true);
                setComment(OdisseyProtecciones.config, "admin.cleanup_delete_regions_with_members_but_no_owners", "     Whether /ps admin cleanup remove should delete regions that have members, but don't have owners (after inactive\n" +
                        "     owners are removed).\n" +
                        "     Regions that have no owners or members will be deleted regardless.");
                break;
            case 14:
                OdisseyProtecciones.config.set("config_version", 15);

                // fix incorrect value set
                if (OdisseyProtecciones.config.get("allow_addowner_for_offline_players_without_lp") == null) {
                    Object value = OdisseyProtecciones.config.get("default_allow_addowner_for_offline_players_without_lp");
                    OdisseyProtecciones.config.setComments("default_allow_addowner_for_offline_players_without_lp", null);
                    OdisseyProtecciones.config.set("default_allow_addowner_for_offline_players_without_lp", null);
                    OdisseyProtecciones.config.set("allow_addowner_for_offline_players_without_lp", value == null ? false : (boolean) value);
                    setComment(OdisseyProtecciones.config, "allow_addowner_for_offline_players_without_lp", " If you do not have LuckPerms, OdisseyProtecciones is unable to determine the limits of offline players (since it depends\n" +
                            " on permissions), and so it requires players to be online. Set this to true if your server does not need limits (and so\n" +
                            " the check is unnecessary).");
                }
                break;
            case 15:
                OdisseyProtecciones.config.set("config_version", 16);
                OdisseyProtecciones.config.set("allow_home_teleport_for_members", true);
                setComment(OdisseyProtecciones.config, "allow_home_teleport_for_members", " Whether or not members of a region can /ps home to the region.");
                break;
            case 16:
                OdisseyProtecciones.config.set("config_version", 17);
                for (File file : blockFiles()) {
                    YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
                    try {
                        List<String> l = new ArrayList<>(c.getStringList("region.hidden_flags_from_info"));
                        if (!l.contains("ps-member-perms")) {
                            l.add("ps-member-perms");
                            c.set("region.hidden_flags_from_info", l);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("No se pudo actualizar hidden_flags_from_info en " + file.getName() + ": " + e.getMessage());
                    }
                    save(c, file);
                }
                break;
            case 17:
                OdisseyProtecciones.config.set("config_version", 18);
                OdisseyProtecciones.config.set("menu.sounds", true);
                setComment(OdisseyProtecciones.config, "menu.sounds", " Activa o desactiva globalmente los sonidos de los menus.");
                OdisseyProtecciones.config.set("menu.open_animation", true);
                setComment(OdisseyProtecciones.config, "menu.open_animation", " Muestra una animacion de titulo al abrir el menu principal (estilo ODISSEY).");
                OdisseyProtecciones.config.set("messages.prefix", "&8[&eODISSEY&8]&r ");
                setComment(OdisseyProtecciones.config, "messages.prefix", " Prefijo usado en los mensajes de chat de feedback del plugin (identidad ODISSEY).");
                break;
            case 18:
                OdisseyProtecciones.config.set("config_version", 19);
                // cargar la cache de UUID en segundo plano por defecto (evita bloquear el arranque)
                OdisseyProtecciones.config.set("async_load_uuid_cache", true);
                setComment(OdisseyProtecciones.config, "async_load_uuid_cache", " Ponlo en true para no bloquear el arranque del servidor al cargar la cache de UUID.\n" +
                        " /ps add y /ps remove no funcionaran con jugadores desconectados hasta que la cache termine de cargar.");
                break;
            case OdisseyProtecciones.CONFIG_VERSION:
                leaveLoop = true;
                break;
            default:
                Bukkit.getLogger().info("Version de config invalida! Puede que el plugin no cargue correctamente!");
                leaveLoop = true;
                break;
        }
        return leaveLoop;
    }

    private static File[] blockFiles() {
        File[] files = OdisseyProtecciones.blockDataFolder.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".yml"));
        return files == null ? new File[0] : files;
    }

    private static void setComment(ConfigurationSection section, String path, String comment) {
        section.setComments(path, Arrays.asList(comment.split("\n", -1)));
    }

    private static void save(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("No se pudo guardar " + file.getName() + ": " + e.getMessage());
        }
    }
}
