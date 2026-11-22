package es.odissey.protecciones;

import es.odissey.protecciones.compat.AxoBlockAdapter;
import es.odissey.protecciones.utils.BlockUtil;
import es.odissey.protecciones.utils.RecipeUtil;
import es.odissey.protecciones.utils.config.Path;
import es.odissey.protecciones.utils.config.YamlConfigBinder;
import es.odissey.protecciones.utils.upgrade.ConfigUpgrades;
import es.odissey.protecciones.utils.upgrade.LegacyUpgrade;
import es.odissey.protecciones.utils.upgrade.TomlMigration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the global config (config.yml) settings.
 */

public class PSConfig {

    // config options
    // config.yml will be loaded into these fields
    // uses autoboxing for primitives to allow for null (because of the way config upgrades work)
    @Path("config_version")
    public int configVersion;
    @Path("uuidupdated")
    public Boolean uuidupdated;
    @Path("region_negative_min_max_updated")
    public Boolean regionNegativeMinMaxUpdated;
    @Path("placing_cooldown")
    public Integer placingCooldown;
    @Path("allow_duplicate_region_names")
    public Boolean allowDuplicateRegionNames;
    @Path("async_load_uuid_cache")
    public Boolean asyncLoadUUIDCache;
    @Path("ps_view_cooldown")
    public Integer psViewCooldown;
    @Path("base_command")
    public String base_command;
    @Path("aliases")
    public List<String> aliases;
    @Path("drop_item_when_inventory_full")
    public Boolean dropItemWhenInventoryFull;
    @Path("regions_must_be_adjacent")
    public Boolean regionsMustBeAdjacent;
    @Path("allow_merging_regions")
    public Boolean allowMergingRegions;
    @Path("allow_merging_holes")
    public Boolean allowMergingHoles;
    @Path("default_protection_block_placement_off")
    public Boolean defaultProtectionBlockPlacementOff;
    @Path("allow_addowner_for_offline_players_without_lp")
    public Boolean allowAddownerForOfflinePlayersWithoutLp;
    @Path("allow_home_teleport_for_members")
    public Boolean allowHomeTeleportForMembers;

    @Path("menu.enabled")
    public Boolean menuEnabled;
    @Path("menu.open_on_command")
    public Boolean menuOpenOnCommand;
    @Path("menu.sounds")
    public Boolean menuSounds;
    @Path("menu.open_animation")
    public Boolean menuOpenAnimation;

    @Path("messages.prefix")
    public String messagesPrefix;

    @Path("shop.enabled")
    public Boolean shopEnabled;
    @Path("shop.refund_enabled")
    public Boolean refundEnabled;
    @Path("shop.refund_percentage")
    public Double shopRefundPercentage;

    @Path("hologram.enabled")
    public Boolean hologramEnabled;
    @Path("hologram.offset_y")
    public Double hologramOffsetY;
    @Path("hologram.update_interval_seconds")
    public Integer hologramUpdateInterval;
    @Path("hologram.billboard")
    public String hologramBillboard;
    @Path("hologram.see_through")
    public Boolean hologramSeeThrough;
    @Path("hologram.background")
    public Boolean hologramBackground;
    @Path("hologram.background_color")
    public String hologramBackgroundColor;
    @Path("hologram.shadowed")
    public Boolean hologramShadowed;
    @Path("hologram.click_to_manage")
    public Boolean hologramClickToManage;
    @Path("hologram.click_width")
    public Double hologramClickWidth;
    @Path("hologram.click_height")
    public Double hologramClickHeight;
    @Path("hologram.lines")
    public List<String> hologramLines;

    @Path("auto_remove.enabled")
    public Boolean autoRemoveEnabled;
    @Path("auto_remove.days")
    public Integer autoRemoveDays;
    @Path("auto_remove.check_interval_minutes")
    public Integer autoRemoveIntervalMinutes;
    @Path("auto_remove.delete_regions_with_members_but_no_owners")
    public Boolean autoRemoveDeleteWithMembersButNoOwners;
    @Path("auto_remove.warn_enabled")
    public Boolean autoRemoveWarnEnabled;
    @Path("auto_remove.warn_days")
    public List<Integer> autoRemoveWarnDays;

    @Path("admin.cleanup_delete_regions_with_members_but_no_owners")
    public Boolean cleanupDeleteRegionsWithMembersButNoOwners;

    @Path("database.enabled")
    public Boolean databaseEnabled;
    @Path("database.type")
    public String databaseType;
    @Path("database.sqlite_file")
    public String databaseSqliteFile;
    @Path("database.mysql.host")
    public String databaseMysqlHost;
    @Path("database.mysql.port")
    public Integer databaseMysqlPort;
    @Path("database.mysql.database")
    public String databaseMysqlDatabase;
    @Path("database.mysql.username")
    public String databaseMysqlUsername;
    @Path("database.mysql.password")
    public String databaseMysqlPassword;
    @Path("database.table_prefix")
    public String databaseTablePrefix;
    @Path("database.snapshot_interval_minutes")
    public Integer databaseSnapshotInterval;
    @Path("database.restore_missing_on_startup")
    public Boolean databaseRestoreMissingOnStartup;
    @Path("database.audit_enabled")
    public Boolean databaseAuditEnabled;

    @Path("economy.max_rent_price")
    public Double maxRentPrice;
    @Path("economy.min_rent_price")
    public Double minRentPrice;
    @Path("economy.max_rent_period")
    public Integer maxRentPeriod;
    @Path("economy.min_rent_period")
    public Integer minRentPeriod;
    @Path("economy.tax_enabled")
    public Boolean taxEnabled;
    @Path("economy.tax_message_on_join")
    public Boolean taxMessageOnJoin;
    @Path("economy.rent_warning_seconds")
    public List<Integer> economyRentWarningSeconds;

    static void initConfig() {

        File dataFolder = OdisseyProtecciones.getInstance().getDataFolder();
        File configFile = OdisseyProtecciones.configLocation; // config.yml
        File legacyToml = new File(dataFolder, "config.toml");

        // check if config files exist
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            if (!OdisseyProtecciones.blockDataFolder.exists()) {
                OdisseyProtecciones.blockDataFolder.mkdir();
                Files.copy(PSConfig.class.getResourceAsStream("/block1.yml"),
                        Paths.get(OdisseyProtecciones.blockDataFolder.getAbsolutePath() + "/block1.yml"),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            // determinar si config.yml ya esta en el formato nuevo (contiene config_version)
            boolean modernYaml = false;
            if (configFile.exists()) {
                try {
                    YamlConfiguration probe = new YamlConfiguration();
                    probe.load(configFile);
                    modernYaml = probe.contains("config_version");
                } catch (Exception ignored) {
                }
            }

            if (!modernYaml) {
                if (legacyToml.exists()) {
                    // migrar config.toml -> config.yml (una sola vez)
                    if (configFile.exists()) {
                        configFile.renameTo(new File(dataFolder, "config.yml.old"));
                    }
                    OdisseyProtecciones.getPluginLogger().info("Migrando config.toml a config.yml...");
                    TomlMigration.migrateConfig(legacyToml, configFile);
                    legacyToml.renameTo(new File(dataFolder, "config.toml.migrated"));
                } else if (configFile.exists()) {
                    // config antigua v1/v2 (config.yml sin config_version)
                    LegacyUpgrade.upgradeFromV1V2();
                }
            }

            if (!configFile.exists()) {
                Files.copy(PSConfig.class.getResourceAsStream("/config.yml"),
                        Paths.get(configFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
            }

            // migrar blocks/*.toml -> blocks/*.yml (una sola vez)
            File[] tomlBlocks = OdisseyProtecciones.blockDataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".toml"));
            if (tomlBlocks != null) {
                for (File toml : tomlBlocks) {
                    String base = toml.getName().substring(0, toml.getName().length() - ".toml".length());
                    File yml = new File(OdisseyProtecciones.blockDataFolder, base + ".yml");
                    if (!yml.exists()) {
                        OdisseyProtecciones.getPluginLogger().info("Migrando " + toml.getName() + " a " + yml.getName() + "...");
                        TomlMigration.migrateBlock(toml, yml);
                    }
                    toml.renameTo(new File(OdisseyProtecciones.blockDataFolder, toml.getName() + ".migrated"));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(OdisseyProtecciones.class.getName()).log(Level.SEVERE, null, ex);
        }

        // keep in mind that there is /ps reload, so clear arrays before adding config options!
        // clear data (for /ps reload)
        OdisseyProtecciones.protectBlocks.clear();

        // loop upgrades until the config has been updated to the latest version
        do {
            OdisseyProtecciones.config = YamlConfiguration.loadConfiguration(configFile); // load latest settings

            // load config into configOptions object
            OdisseyProtecciones.getInstance().setConfigOptions(
                    YamlConfigBinder.bind(OdisseyProtecciones.config, PSConfig::new));

            // upgrade config if need be (v3+)
            boolean leaveLoop = ConfigUpgrades.doConfigUpgrades();
            if (leaveLoop) break; // leave loop if config version is correct

            // save config if upgrading
            try {
                OdisseyProtecciones.config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (true);

        // anadir claves nuevas que falten en configs ya existentes (database, avisos, etc.)
        try {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(
                    PSConfig.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
            if (mergeDefaults(OdisseyProtecciones.config, defaults)) {
                OdisseyProtecciones.config.save(configFile);
                OdisseyProtecciones.getInstance().setConfigOptions(
                        YamlConfigBinder.bind(OdisseyProtecciones.config, PSConfig::new));
            }
        } catch (IOException e) {
            OdisseyProtecciones.getPluginLogger().warning("No se pudieron anadir las opciones nuevas al config: " + e.getMessage());
        }

        // carga los bloques de proteccion al mapa de opciones
        File[] blockFiles = OdisseyProtecciones.blockDataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (blockFiles == null || blockFiles.length == 0) {
            OdisseyProtecciones.getPluginLogger().warning("La carpeta de bloques esta vacia! No tienes ningun bloque de proteccion configurado!");
        } else {

            // plantilla por defecto para rellenar claves que falten en cada bloque
            YamlConfiguration template = YamlConfiguration.loadConfiguration(new InputStreamReader(
                    PSConfig.class.getResourceAsStream("/block1.yml"), StandardCharsets.UTF_8));

            // iterate over block files and load into map
            OdisseyProtecciones.getPluginLogger().info("Bloques de proteccion:");
            for (File file : blockFiles) {

                YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

                PSProtectBlock b;
                if (AxoBlockAdapter.isAxoBlock(c)) {
                    // formato AxoProtectionBlock: se adapta sin modificar el archivo original
                    try {
                        b = AxoBlockAdapter.convert(c, template, file.getName());
                    } catch (Exception e) {
                        OdisseyProtecciones.getPluginLogger().warning("No se pudo importar el bloque AxoProtectionBlock "
                                + file.getName() + ": " + e.getMessage());
                        continue;
                    }
                } else {
                    // check to make sure all options are not null
                    boolean updated = mergeDefaults(c, template);
                    if (updated) {
                        try {
                            c.save(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // convert yaml data into object
                    b = YamlConfigBinder.bind(c, PSProtectBlock::new);
                }

                // check if material is valid, and is not a player head (since player heads also have the player name after)
                if (Material.getMaterial(b.type) == null && !(b.type.startsWith(Material.PLAYER_HEAD.toString()))) {
                    OdisseyProtecciones.getPluginLogger().warning("Material no reconocido: " + b.type);
                    OdisseyProtecciones.getPluginLogger().warning("El bloque no se agregara. Corrige esto en tu config.");
                    continue;
                }

                // check for duplicates
                if (OdisseyProtecciones.isProtectBlockType(b.type)) {
                    OdisseyProtecciones.getPluginLogger().warning("Tipo de bloque duplicado! Omitiendo el bloque extra " + b.type);
                    continue;
                }
                if (OdisseyProtecciones.getProtectBlockFromAlias(b.alias) != null) {
                    OdisseyProtecciones.getPluginLogger().warning("Alias de bloque duplicado! Omitiendo el bloque extra " + b.alias);
                    continue;
                }

                OdisseyProtecciones.getPluginLogger().info("- " + b.type + " (" + b.alias + ")");
                FlagHandler.initDefaultFlagsForBlock(b); // process flags for block and set regionFlags field

                // for PLAYER_HEAD:base64, we need to change the entry to link to a UUID hash instead of storing the giant base64
                if (BlockUtil.isBase64PSHead(b.type)) {
                    String nuuid = BlockUtil.getUUIDFromBase64PS(b);

                    BlockUtil.uuidToBase64Head.put(nuuid, b.type.split(":")[1]);
                    b.type = "PLAYER_HEAD:" + nuuid;
                }

                OdisseyProtecciones.protectBlocks.put(b.type, b); // add block
            }

            // setup crafting recipes for all blocks
            RecipeUtil.setupPSRecipes();
        }
    }

    /**
     * Copia recursivamente las claves (y comentarios) que falten en target desde template.
     *
     * @return true si se anadio alguna clave
     */
    private static boolean mergeDefaults(ConfigurationSection target, ConfigurationSection template) {
        boolean updated = false;
        for (String key : template.getKeys(false)) {
            Object templateValue = template.get(key);
            if (templateValue instanceof ConfigurationSection) {
                if (!target.isConfigurationSection(key)) {
                    target.createSection(key);
                }
                updated |= mergeDefaults(target.getConfigurationSection(key), (ConfigurationSection) templateValue);
            } else if (!target.isSet(key)) {
                target.set(key, templateValue);
                List<String> comments = template.getComments(key);
                if (comments != null && !comments.isEmpty()) {
                    target.setComments(key, comments);
                }
                updated = true;
            }
        }
        return updated;
    }
}
