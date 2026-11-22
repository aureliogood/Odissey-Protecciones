package es.odissey.protecciones;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.commands.ArgHelp;
import es.odissey.protecciones.commands.PSCommandArg;
import es.odissey.protecciones.placeholders.PSPlaceholderExpansion;
import es.odissey.protecciones.storage.Database;
import es.odissey.protecciones.storage.RegionSnapshot;
import es.odissey.protecciones.utils.BlockUtil;
import es.odissey.protecciones.utils.RecipeUtil;
import es.odissey.protecciones.utils.upgrade.LegacyUpgrade;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandMap;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * The base class for the plugin. Some utilities are static, and others are instance methods, so they need to
 * be accessed through getInstance().
 */

public class OdisseyProtecciones extends JavaPlugin {
    // change this when the config version goes up
    public static final int CONFIG_VERSION = 19;

    private boolean debug = false;

    public static File configLocation, blockDataFolder;
    public static YamlConfiguration config;

    private static List<PSCommandArg> commandArgs = new ArrayList<>();
    private static OdisseyProtecciones plugin;

    private PSEconomy economy;
    private Database database;

    // all configuration file options are stored in here
    private PSConfig configOptions;
    static HashMap<String, PSProtectBlock> protectBlocks = new HashMap<>();


    // ps alias to id cache
    // <world-name, <alias, [ids]>>
    static HashMap<UUID, HashMap<String, ArrayList<String>>> regionNameToID = new HashMap<>();

    // vault economy integration
    private boolean vaultSupportEnabled = false;
    private Economy vaultEconomy;

    // luckperms integration
    private boolean luckPermsSupportEnabled = false;
    private LuckPerms luckPerms;

    private boolean placeholderAPISupportEnabled = false;

    // ps toggle/on/off list
    public static Set<UUID> toggleList = new HashSet<>();

    /* ~~~~~~~~~~ Instance methods ~~~~~~~~~~~~ */

    /**
     * Add a command argument to /ps.
     *
     * @param psca PSCommandArg object to be added
     */
    public void addCommandArgument(PSCommandArg psca) {
        commandArgs.add(psca);
    }

    /**
     * @return the list of command arguments for /ps
     */
    public List<PSCommandArg> getCommandArguments() {
        return commandArgs;
    }

    /**
     * @return whether PlaceholderAPI support is enabled
     */
    public boolean isPlaceholderAPISupportEnabled() {
        return placeholderAPISupportEnabled;
    }

    /**
     * @return whether vault support is enabled
     */
    public boolean isVaultSupportEnabled() {
        return vaultSupportEnabled;
    }

    /**
     * @return returns this instance's vault economy hook
     */
    public Economy getVaultEconomy() {
        return vaultEconomy;
    }

    /**
     * @return the instance's {@link PSEconomy} instance
     */
    public PSEconomy getPSEconomy() {
        return economy;
    }

    public boolean isLuckPermsSupportEnabled() {
        return luckPermsSupportEnabled;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    /**
     * @param debug whether the plugin should be in debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return whether the plugin is in debug mode
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Print a debug message (only prints if the plugin is in debug mode).
     * @param msg the message to print
     */
    public void debug(String msg) {
        if (debug) {
            getLogger().info("[DEBUG] " + msg);
        }
    }

    /**
     * @return returns the config options of this instance of OdisseyProtecciones
     */
    public PSConfig getConfigOptions() {
        return configOptions;
    }

    /**
     * @return la base de datos de respaldo de protecciones, o null si no esta habilitada
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * @param conf config object to replace current config
     */
    public void setConfigOptions(PSConfig conf) {
        this.configOptions = conf;
    }

    /**
     * Returns the list of PSProtectBlocks configured through the config.
     *
     * @return the list of PSProtectBlocks configured
     */
    public List<PSProtectBlock> getConfiguredBlocks() {
        List<PSProtectBlock> l = new ArrayList<>();
        for (PSProtectBlock b : protectBlocks.values()) {
            if (!l.contains(b)) l.add(b);
        }
        return l;
    }


    /* ~~~~~~~~~~ Static methods ~~~~~~~~~~~~~~ */

    /**
     * @return the plugin instance that is currently being used
     */
    public static OdisseyProtecciones getInstance() {
        return plugin;
    }

    /**
     * @return the plugin's logger
     */
    public static Logger getPluginLogger() {
        return plugin.getLogger();
    }

    /**
     * @return the PSEconomy object adapter
     */
    public static PSEconomy getEconomy() {
        return getInstance().getPSEconomy();
    }

    /**
     * Get the protection block config options for the block specified.
     *
     * @param block the block to get the block options of
     * @return the config options for the protect block specified (null if not found)
     */

    public static PSProtectBlock getBlockOptions(Block block) {
        if (block == null) return null;
        return getBlockOptions(BlockUtil.getProtectBlockType(block));
    }

    /**
     * Get the protection block config options for the item specified.
     *
     * If the options has restrict-obtaining enabled, and the item does not contain the required NBT tag, null will
     * be returned.
     *
     * @param item the item to get the block options of
     * @return the config options for the protect item specified (null if not found)
     */

    public static PSProtectBlock getBlockOptions(ItemStack item) {
        if (!isProtectBlockItem(item)) return null;
        return getBlockOptions(BlockUtil.getProtectBlockType(item));
    }

    /**
     * Gets the config options for the protection block type specified. It is recommended to use the block parameter overloaded
     * method instead if possible, since it deals better with heads.
     *
     * @param blockType the material type name (Bukkit) of the protect block to get the options for, or "PLAYER_HEAD name" for heads
     * @return the config options for the protect block specified (null if not found)
     */
    public static PSProtectBlock getBlockOptions(String blockType) {
        return protectBlocks.get(blockType);
    }

    public static boolean isProtectBlockType(Block b) {
        return getBlockOptions(b) != null;
    }

    /**
     * Get whether or not a material is used as a protection block. It is recommended to use the block
     * parameter overloaded method if possible since player heads have a different format.
     *
     * @param material material type to check (Bukkit material name), or "PLAYER_HEAD name" for heads
     * @return whether or not that material is being used for a protection block
     */
    public static boolean isProtectBlockType(String material) {
        return protectBlocks.containsKey(material);
    }

    /**
     * Check whether or not a given block is a protection block, and actually protects a region.
     * @param b the block to look at
     * @return whether or not the block is a protection block responsible for a region.
     */

    public static boolean isProtectBlock(Block b) {
        if (!isProtectBlockType(b)) return false;
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(b.getWorld());
        if (rgm == null) return false;
        return rgm.getRegion(WGUtils.createPSID(b.getLocation())) != null || PSMergedRegion.getMergedRegion(b.getLocation()) != null;
    }

    /**
     * Check if a WorldGuard {@link ProtectedRegion} is a OdisseyProtecciones region, and is configured in the config.
     *
     * @param r the region to check
     * @return true if the WorldGuard region is a OdisseyProtecciones region, and false if it isn't
     */
    public static boolean isPSRegion(ProtectedRegion r) {
        return isPSRegionFormat(r) && getBlockOptions(r.getFlag(FlagHandler.PS_BLOCK_MATERIAL)) != null;
    }

    /**
     * Check if a WorldGuard {@link ProtectedRegion} has the format of a OdisseyProtecciones region, but is not necessarily configured
     * in the config.
     *
     * @param r the region to check
     * @return true if the WorldGuard region is a OdisseyProtecciones region, and false if it isn't
     */
    public static boolean isPSRegionFormat(ProtectedRegion r) {
        return r != null && r.getId().startsWith("ps") && r.getFlag(FlagHandler.PS_BLOCK_MATERIAL) != null;
    }

    /**
     * Check if a OdisseyProtecciones name is already used by a region globally (from /ps name)
     *
     * @param name the name to search for
     * @return whether or not there is a region with this name
     */

    public static boolean isPSNameAlreadyUsed(String name) {
        for (UUID worldUid : regionNameToID.keySet()) {
            RegionManager rgm = WGUtils.getRegionManagerWithWorld(Bukkit.getWorld(worldUid));

            List<String> l = regionNameToID.get(worldUid).get(name);
            if (l == null) continue;
            for (int i = 0; i < l.size(); i++) { // remove outdated cache
                if (rgm.getRegion(l.get(i)) == null) {
                    l.remove(i);
                    i--;
                }
            }
            if (!l.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Obtiene regiones de proteccion por ID o alias.
     *
     * @param w          the world to search in (only if it is an id; aliases/names are global)
     * @param identifier id or alias of the region
     * @return a list of psregions that match the id or alias; will be empty if no regions were found
     */

    public static List<PSRegion> getPSRegions(World w, String identifier) {
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) return new ArrayList<>();

        PSRegion r = PSRegion.fromWGRegion(w, rgm.getRegion(identifier));
        if (r != null) { // return id based query
            return new ArrayList<>(Collections.singletonList(r));
        } else { // return alias based query
            List<PSRegion> regions = new ArrayList<>();
            PSRegion.fromName(identifier).values().forEach(regions::addAll);
            return regions;
        }
    }

    /**
     * Elimina una region de proteccion dado su ID y el gestor de regiones en el que se guarda
     * Note: Does not remove the PS block.
     *
     * @param w    the world that the region is in
     * @param psID the worldguard region ID of the region
     * @return whether or not the event was cancelled
     */

    public static boolean removePSRegion(World w, String psID) {
        PSRegion r = PSRegion.fromWGRegion(checkNotNull(w), checkNotNull(WGUtils.getRegionManagerWithWorld(w).getRegion(psID)));
        return r != null && r.deleteRegion(false);
    }

    /**
     * Elimina una region de proteccion dado su ID y el gestor de regiones en el que se guarda, con un jugador como causa
     * Note: Does not remove the PS block, and does not check if the player (cause) has permission to do this.
     *
     * @param w     the world that the region is in
     * @param psID  the worldguard region ID of the region
     * @param cause the player that caused the removal
     * @return whether or not the event was cancelled
     */

    public static boolean removePSRegion(World w, String psID, Player cause) {
        PSRegion r = PSRegion.fromWGRegion(checkNotNull(w), checkNotNull(WGUtils.getRegionManagerWithWorld(w).getRegion(psID)));
        return r != null && r.deleteRegion(false, cause);
    }

    /**
     * Get the config options for a protect block based on its alias
     *
     * @param name the alias of the protection block
     * @return the protect block options, or null if it wasn't found
     */

    public static PSProtectBlock getProtectBlockFromAlias(String name) {
        if (name == null) return null;
        for (PSProtectBlock cpb : OdisseyProtecciones.protectBlocks.values()) {
            if (cpb.alias.equalsIgnoreCase(name) || cpb.type.equalsIgnoreCase(name)) return cpb;
        }
        return null;
    }

    /**
     * Check if an item is a valid protection block, and if checkNBT is true, check if it was created by
     * OdisseyProtecciones. Be aware that blocks may have restrict-obtaining off, meaning that it is ignored whether or not
     * the item is created by OdisseyProtecciones (in this case have checkNBT false).
     *
     * @param item     the item to check
     * @param checkNBT whether or not to check if the plugin signed off on the item (restrict-obtaining)
     * @return si el objeto es un bloque de proteccion valido y fue creado por OdisseyProtecciones
     */

    public static boolean isProtectBlockItem(ItemStack item, boolean checkNBT) {
        if (item == null) return false;
        // check basic item
        if (!OdisseyProtecciones.isProtectBlockType(BlockUtil.getProtectBlockType(item))) return false;
        // check for player heads
        if (!checkNBT) return true; // if not checking nbt, you only need to check type

        boolean tag = false;

        // comprueba si el objeto fue creado por OdisseyProtecciones (guardado en un tag persistente)
        if (item.getItemMeta() != null) {
            NamespacedKey key = new NamespacedKey(OdisseyProtecciones.getInstance(), "isPSBlock");
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(key, PersistentDataType.BYTE)) {
                Byte isPSBlock = pdc.get(key, PersistentDataType.BYTE);
                tag = isPSBlock != null && isPSBlock == 1;
            } else if (pdc.has(key, PersistentDataType.STRING)) {
                String isPSBlock = pdc.get(key, PersistentDataType.STRING);
                tag = isPSBlock != null && isPSBlock.equals("true");
            } else {
                try { // compatibilidad con el tag legado (API antigua CustomItemTagContainer)
                    CustomItemTagContainer tagContainer = item.getItemMeta().getCustomTagContainer();
                    Byte isPSBlock = tagContainer.getCustomTag(key, ItemTagType.BYTE);
                    tag = isPSBlock != null && isPSBlock == 1;
                } catch (Throwable ignored) {
                }
            }
        }

        return tag; // whether or not the nbt tag was found
    }

    /**
     * Check if an item is a valid protection block, and if the block type has restrict-obtaining on, check if it was
     * created by OdisseyProtecciones (custom NBT tag). Be aware that blocks may have restrict-obtaining
     * off, meaning that it ignores whether or not the item is created by OdisseyProtecciones.
     *
     * @param item     the item to check
     * @return si el objeto es un bloque de proteccion valido y fue creado por OdisseyProtecciones
     */

    public static boolean isProtectBlockItem(ItemStack item) {
        if (item == null) return false;
        PSProtectBlock b = OdisseyProtecciones.getBlockOptions(BlockUtil.getProtectBlockType(item));
        if (b == null) return false;
        return isProtectBlockItem(item, b.restrictObtaining);
    }

    /**
     * Get a protection block item from a protect block config object.
     *
     * @param b the config options for the protection block
     * @return el objeto con NBT y otros metadatos que indican que fue creado por OdisseyProtecciones
     */

    public static ItemStack createProtectBlockItem(PSProtectBlock b) {
        ItemStack is = BlockUtil.getProtectBlockItemFromType(b.type);

        // add enchant effect if enabled
        if (b.enchantedEffect) {
            is.addUnsafeEnchantment(Enchantment.LURE, 1);
        }

        ItemMeta im = is.getItemMeta();

        // add skull metadata, must be before others since it resets item metadata
        if (im instanceof SkullMeta && is.getType().equals(Material.PLAYER_HEAD) && b.type.split(":").length > 1) {
            is = BlockUtil.setHeadType(b.type, is);
            im = is.getItemMeta();
        }

        // set custom model data
        if (b.customModelData != -1) {
            im.setCustomModelData(b.customModelData);
        }

        // add display name and lore
        if (!b.displayName.equals("")) {
            im.setDisplayName(ChatColor.translateAlternateColorCodes('&', b.displayName));
        }
        List<String> lore = new ArrayList<>();
        for (String s : b.lore) lore.add(ChatColor.translateAlternateColorCodes('&', s));
        im.setLore(lore);

        // hide enchant name (cannot call addUnsafeEnchantment here)
        if (b.enchantedEffect) {
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // anade un identificador a los objetos creados por OdisseyProtecciones
        im.getPersistentDataContainer().set(new NamespacedKey(plugin, "isPSBlock"), PersistentDataType.BYTE, (byte) 1);

        is.setItemMeta(im);

        return is;
    }

    // called on first start, and /ps reload
    public static void loadConfig(boolean isReload) {
        // remove old ps crafting recipes
        RecipeUtil.removePSRecipes();

        // init config
        PSConfig.initConfig();

        // init messages
        PSL.loadConfig();

        // init help menu
        ArgHelp.initHelpMenu();

        // load inventory menus
        es.odissey.protecciones.gui.MenuManager.getInstance().load();

        // start hologram refresh task and auto-removal task
        HologramManager.getInstance().start();
        es.odissey.protecciones.utils.AutoRemove.start();

        // load economy
        if (OdisseyProtecciones.getInstance().economy != null) OdisseyProtecciones.getInstance().economy.stop();
        OdisseyProtecciones.getInstance().economy = new PSEconomy();

        // add command to Bukkit (using reflection)
        if (!isReload) {
            try {
                final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                bukkitCommandMap.setAccessible(true);
                CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

                PSCommand psc = new PSCommand(getInstance().configOptions.base_command);
                for (String command : getInstance().configOptions.aliases) { // add aliases
                    psc.getAliases().add(command);
                }
                commandMap.register(getInstance().configOptions.base_command, psc); // register command

            } catch (Exception | NoSuchMethodError e) {
                OdisseyProtecciones.getPluginLogger().severe("No se pudieron cargar los comandos del plugin!");
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onLoad() {
        // register WG flags
        FlagHandler.registerFlags();
    }

    @Override
    public void onEnable() {
        FlagHandler.registerHandlers(); // register custom WG flag handlers

        plugin = this;
        configLocation = new File(this.getDataFolder() + "/config.yml");
        blockDataFolder = new File(this.getDataFolder() + "/blocks");

        // load command arguments
        PSCommand.addDefaultArguments();

        // register event listeners
        getServer().getPluginManager().registerEvents(new ListenerClass(), this);
        getServer().getPluginManager().registerEvents(new es.odissey.protecciones.gui.MenuListener(), this);
        getServer().getPluginManager().registerEvents(new MemberPermListener(), this);
        getServer().getPluginManager().registerEvents(new RegionEnterListener(), this);

        // check that WorldGuard and WorldEdit are enabled (WorldGuard will only be enabled if there's WorldEdit)
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null || !getServer().getPluginManager().getPlugin("WorldGuard").isEnabled()) {
            getLogger().severe("WorldGuard o WorldEdit no estan habilitados! Deshabilitando OdisseyProtecciones...");
            getServer().getPluginManager().disablePlugin(this);
        }

        // check if Vault is enabled (for economy support)
        if (getServer().getPluginManager().getPlugin("Vault") != null && getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
            RegisteredServiceProvider<Economy> econ = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (econ == null) {
                getLogger().warning("Vault no encontro ningun plugin de economia! No habra soporte de economia!");
                vaultSupportEnabled = false;
            } else {
                vaultEconomy = econ.getProvider();
                vaultSupportEnabled = true;
            }
        } else {
            getLogger().warning("Vault no esta habilitado! No habra soporte de economia!");
        }

        // check for PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null && getServer().getPluginManager().getPlugin("PlaceholderAPI").isEnabled()) {
            getLogger().info("Soporte de PlaceholderAPI habilitado!");
            placeholderAPISupportEnabled = true;
            new PSPlaceholderExpansion().register();
        } else {
            getLogger().info("PlaceholderAPI no encontrado! No habra soporte de PlaceholderAPI.");
        }

        // check for LuckPerms
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null && getServer().getPluginManager().getPlugin("LuckPerms").isEnabled()) {
            try {
                luckPermsSupportEnabled = true;
                luckPerms = getServer().getServicesManager().load(LuckPerms.class);
                getLogger().info("Soporte de LuckPerms habilitado!");
            } catch (NoClassDefFoundError err) { // incompatible luckperms api
                getLogger().warning("Version de LuckPerms incompatible! Actualiza tu LuckPerms!");
            }
        }

        // load configuration
        loadConfig(false);

        // register odisseyprotecciones.flags.edit.[flag] permission
        FlagHandler.initializePermissions();

        // iniciar base de datos de respaldo y restaurar protecciones perdidas
        database = new Database(this);
        if (database.init()) {
            boolean restoreMissing = configOptions.databaseRestoreMissingOnStartup == null
                    || configOptions.databaseRestoreMissingOnStartup;
            if (restoreMissing) {
                int restored = database.getRepository().restoreMissing();
                if (restored > 0) {
                    getLogger().info("Se restauraron " + restored + " protecciones desde la base de datos.");
                    try {
                        HologramManager.getInstance().refreshLoaded();
                    } catch (Exception ignored) {
                    }
                }
            }

            int interval = configOptions.databaseSnapshotInterval == null
                    ? 10 : configOptions.databaseSnapshotInterval;
            if (interval > 0) {
                long ticks = interval * 60L * 20L;
                Bukkit.getScheduler().runTaskTimer(this, () -> {
                    if (database == null || !database.isEnabled()) return;
                    database.saveAllAsync(RegionSnapshot.collectAll());
                }, ticks, ticks);
            }
        }

        // build up region cache
        getLogger().info("Construyendo cache de regiones...");

        HashMap<World, RegionManager> regionManagers = WGUtils.getAllRegionManagers();
        for (World w : regionManagers.keySet()) {
            RegionManager rgm = regionManagers.get(w);
            HashMap<String, ArrayList<String>> m = new HashMap<>();
            for (ProtectedRegion r : rgm.getRegions().values()) {
                String name = r.getFlag(FlagHandler.PS_NAME);
                if (isPSRegion(r) && name != null) {
                    if (m.containsKey(name)) {
                        m.get(name).add(r.getId());
                    } else {
                        m.put(name, new ArrayList<>(Collections.singletonList(r.getId())));
                    }
                }
            }
            regionNameToID.put(w.getUID(), m);
        }

        // uuid cache
        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers(); // snapshot en el hilo principal
        boolean asyncLoadUUIDCache = configOptions.asyncLoadUUIDCache == null || configOptions.asyncLoadUUIDCache;
        getLogger().info("Construyendo cache de UUID (" + offlinePlayers.length + " jugadores, "
                + (asyncLoadUUIDCache ? "asincrono" : "sincrono") + ")...");
        if (asyncLoadUUIDCache) { // async load
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (OfflinePlayer op : offlinePlayers) {
                    try {
                        UUIDCache.storeUUIDNamePair(op.getUniqueId(), op.getName());
                    } catch (Exception ignored) {
                    }
                }
            });
        } else { // sync load
            for (OfflinePlayer op : offlinePlayers) {
                try {
                    UUIDCache.storeUUIDNamePair(op.getUniqueId(), op.getName());
                } catch (Exception ignored) {
                }
            }
        }

        // check if UUIDs have been upgraded already
        getLogger().info("Comprobando si las regiones ya se actualizaron a UUIDs...");

        // update to UUIDs
        if (configOptions.uuidupdated == null || !configOptions.uuidupdated)
            LegacyUpgrade.convertToUUID();

        if (configOptions.regionNegativeMinMaxUpdated == null || !configOptions.regionNegativeMinMaxUpdated)
            LegacyUpgrade.upgradeRegionsWithNegativeYValues();

        getLogger().info(ChatColor.WHITE + "OdisseyProtecciones se inicio correctamente!");
    }

    @Override
    public void onDisable() {
        // cancelar tareas programadas del plugin
        HologramManager.getInstance().stop();
        es.odissey.protecciones.utils.AutoRemove.stop();
        if (economy != null) economy.stop();

        // guardar los cambios pendientes de WorldGuard para no perder datos en apagados/reinicios
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
                if (rgm != null) rgm.saveChanges();
            }
        } catch (Exception e) {
            getLogger().warning("No se pudieron guardar las regiones al apagar: " + e.getMessage());
        }

        // guardar copia final en la base de datos y cerrar la conexion
        if (database != null) {
            try {
                database.saveAllSync(RegionSnapshot.collectAll());
            } catch (Exception e) {
                getLogger().warning("No se pudo guardar el respaldo final de protecciones: " + e.getMessage());
            }
            database.close();
        }
    }

}