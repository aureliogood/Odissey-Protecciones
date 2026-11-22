package es.odissey.protecciones.utils.upgrade;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LegacyUpgrade {

    // upgrade to 1.17, upgrade regions with 0->256 to SHORT_MIN->SHORT_MAX
    public static void upgradeRegionsWithNegativeYValues() {
        OdisseyProtecciones.getInstance().getLogger().info("Upgrading region y-values for 1.17...");
        boolean guardadoOk = true;
        for (RegionManager rgm : WGUtils.getAllRegionManagers().values()) {
            List<ProtectedRegion> newRegions = new ArrayList<>();

            // loop through each region
            for (var region : rgm.getRegions().values()) {
                int minY = region.getMinimumPoint().getBlockY(), maxY = region.getMaximumPoint().getBlockY();
                if (OdisseyProtecciones.isPSRegion(region) && minY == 0 && maxY == 256) {
                    // clone region, and recreate with new min/max points
                    ProtectedRegion toAdd = null;
                    if (region instanceof ProtectedPolygonalRegion) { // convert merged region
                        toAdd = new ProtectedPolygonalRegion(region.getId(), region.getPoints(), WGUtils.MIN_BUILD_HEIGHT, WGUtils.MAX_BUILD_HEIGHT);
                    } else if (region instanceof ProtectedCuboidRegion) { // convert standard region
                        BlockVector3 minVec = BlockVector3.at(region.getMinimumPoint().getX(), WGUtils.MIN_BUILD_HEIGHT, region.getMinimumPoint().getZ()),
                                     maxVec = BlockVector3.at(region.getMaximumPoint().getX(), WGUtils.MAX_BUILD_HEIGHT, region.getMaximumPoint().getZ());
                        toAdd = new ProtectedCuboidRegion(region.getId(), minVec, maxVec);
                    }

                    if (toAdd != null) {
                        OdisseyProtecciones.getInstance().getLogger().info("Region actualizada " + region.getId());
                        toAdd.copyFrom(region); // copy region settings
                        newRegions.add(toAdd);
                    }

                } else {
                    newRegions.add(region);
                }
            }

            rgm.setRegions(newRegions);
            try {
                rgm.save();
            } catch (StorageException e) {
                guardadoOk = false;
                e.printStackTrace();
            }
        }
        // solo marcar la actualizacion como hecha si todos los mundos se guardaron correctamente
        if (guardadoOk) {
            OdisseyProtecciones.config.set("region_negative_min_max_updated", true);
            saveConfig();
            OdisseyProtecciones.getInstance().getLogger().info("Terminado!");
        } else {
            OdisseyProtecciones.getInstance().getLogger().severe("No se pudieron guardar todas las regiones; la actualizacion de altura se reintentara en el proximo arranque.");
        }
    }

    // for one day when we switch to proper base64 generation (no hashcode, use nameuuidfrombytes)
    // problem is, currently I don't know how to convert all items to use this uuid
    public static void fixBase64HeadRegions() {

        HashMap<String, String> oldToNew = new HashMap<>();

        for (PSProtectBlock b : OdisseyProtecciones.getInstance().getConfiguredBlocks()) {
            if (b.type.startsWith("PLAYER_HEAD:") && b.type.split(":").length > 1) {
                String base64 = b.type.split(":")[1];
                oldToNew.put(new UUID(base64.hashCode(), base64.hashCode()).toString(), UUID.nameUUIDFromBytes(base64.getBytes()).toString());
            }
        }

        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WGUtils.getRegionManagerWithWorld(world);
            for (ProtectedRegion r : rm.getRegions().values()) {
                if (OdisseyProtecciones.isPSRegion(r)) {
                    PSRegion psr = PSRegion.fromWGRegion(world, r);

                    if (psr instanceof PSGroupRegion) {
                        PSGroupRegion psgr = (PSGroupRegion) psr;
                        for (PSMergedRegion psmr : psgr.getMergedRegions()) {

                            String type = psmr.getType();
                            if (oldToNew.containsKey(type)) {
                                Set<String> flag = psmr.getGroupRegion().getWGRegion().getFlag(FlagHandler.PS_MERGED_REGIONS_TYPES);
                                String original = null;
                                for (String s : flag) {
                                    String[] spl = s.split(" ");
                                    String id = spl[0];
                                    if (id.equals(psmr.getId())) {
                                        original = s;
                                        break;
                                    }
                                }

                                if (original != null) {
                                    flag.remove(original);
                                    flag.add(psmr.getId() + " " + oldToNew.get(type));
                                }
                            }
                        }
                    }

                    if (oldToNew.containsKey(psr.getType())) {
                        psr.getWGRegion().setFlag(FlagHandler.PS_BLOCK_MATERIAL, oldToNew.get(psr.getType()));
                    }

                }
            }
            try {
                rm.save();
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }
    }

    // check that all of the PS custom flags are in ps regions and upgrade if not
    // originally used for the v1 -> v2 transition
    public static void upgradeRegions() {

        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WGUtils.getRegionManagerWithWorld(world);
            for (String regionName : rm.getRegions().keySet()) {
                if (regionName.startsWith("ps") && !OdisseyProtecciones.isPSRegion(rm.getRegion(regionName))) {
                    try {
                        PSLocation psl = WGUtils.parsePSRegionToLocation(regionName);
                        ProtectedRegion r = rm.getRegion(regionName);

                        // obtener el material del bloque de proteccion
                        String material = world.getBlockAt(psl.x, psl.y, psl.z).getType().toString();

                        if (r.getFlag(FlagHandler.PS_BLOCK_MATERIAL) == null) {
                            r.setFlag(FlagHandler.PS_BLOCK_MATERIAL, material);
                        }

                        if (r.getFlag(FlagHandler.PS_HOME) == null) {
                            if (OdisseyProtecciones.isProtectBlockType(material)) {
                                PSProtectBlock cpb = OdisseyProtecciones.getBlockOptions(material);
                                r.setFlag(FlagHandler.PS_HOME, (psl.x + cpb.homeXOffset) + " " + (psl.y + cpb.homeYOffset) + " " + (psl.z + cpb.homeZOffset));
                            } else {
                                r.setFlag(FlagHandler.PS_HOME, psl.x + " " + psl.y + " " + psl.z);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                rm.save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("[OdisseyProtecciones] WorldGuard Error [" + e + "] during Region File Save");
            }
        }
    }

    // convert regions to use UUIDs instead of player names
    public static void convertToUUID() {
        Bukkit.getLogger().info("Updating PS regions to UUIDs...");
        boolean guardadoOk = true;
        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WGUtils.getRegionManagerWithWorld(world);

            // iterate over regions in world
            for (String regionName : rm.getRegions().keySet()) {
                if (regionName.startsWith("ps")) {
                    ProtectedRegion region = rm.getRegion(regionName);

                    // convert owners with player names to UUIDs
                    List<String> owners, members;
                    owners = new ArrayList<>(region.getOwners().getPlayers());
                    members = new ArrayList<>(region.getMembers().getPlayers());

                    // convert
                    for (String owner : owners) {
                        UUID uuid = Bukkit.getOfflinePlayer(owner).getUniqueId();
                        region.getOwners().removePlayer(owner);
                        region.getOwners().addPlayer(uuid);
                    }
                    for (String member : members) {
                        UUID uuid = Bukkit.getOfflinePlayer(member).getUniqueId();
                        region.getMembers().removePlayer(member);
                        region.getMembers().addPlayer(uuid);
                    }
                }
            }

            try {
                rm.save();
            } catch (Exception e) {
                guardadoOk = false;
                Bukkit.getLogger().severe("[OdisseyProtecciones] WorldGuard Error [" + e + "] during Region File Save");
            }
        }

        // solo marcar la conversion como hecha si TODOS los mundos se guardaron correctamente;
        // en caso contrario se reintentara en el proximo arranque (evita perder la migracion a UUIDs)
        if (guardadoOk) {
            OdisseyProtecciones.config.set("uuidupdated", true);
            saveConfig();
            Bukkit.getLogger().info("Listo!");
        } else {
            Bukkit.getLogger().severe("[OdisseyProtecciones] No se pudieron guardar todas las regiones; la conversion a UUIDs se reintentara en el proximo arranque.");
        }
    }

    // upgrade from config < v2.0.0
    public static void upgradeFromV1V2() {
        Bukkit.getLogger().info(ChatColor.AQUA + "Upgrading configs from v1.x to v2.0+...");

        try {
            OdisseyProtecciones.blockDataFolder.mkdir();

            // leer la config antigua (v1/v2) antes de sobrescribirla
            File oldConfig = new File(OdisseyProtecciones.getInstance().getDataFolder() + "/config.yml");
            YamlConfiguration old = YamlConfiguration.loadConfiguration(oldConfig);

            // crear la nueva config.yml a partir de la plantilla e importar los valores antiguos
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                    PSConfig.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));

            yml.set("uuidupdated", (old.get("UUIDUpdated") != null) && old.getBoolean("UUIDUpdated"));
            yml.set("placing_cooldown", (old.getBoolean("cooldown.enable")) ? old.getInt("cooldown.cooldown") : -1);

            // opciones de ambito global
            List<String> worldsDenied = old.getStringList("Worlds Denied");
            List<String> flags = old.getStringList("Flags");
            String allowedFlagsRaw = old.getString("Allowed Flags");
            List<String> allowedFlags = (allowedFlagsRaw == null) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(allowedFlagsRaw.split(",")));

            // renombrar la config antigua antes de escribir la nueva
            oldConfig.renameTo(new File(OdisseyProtecciones.getInstance().getDataFolder() + "/config.yml.old"));
            yml.save(OdisseyProtecciones.configLocation);

            // upgrade blocks
            var regionSection = old.getConfigurationSection("Region");
            if (regionSection != null)
            for (String type : regionSection.getKeys(false)) {
                File file = new File(OdisseyProtecciones.blockDataFolder.getAbsolutePath() + "/" + type + ".yml");
                YamlConfiguration b = YamlConfiguration.loadConfiguration(new InputStreamReader(
                        PSConfig.class.getResourceAsStream("/block1.yml"), StandardCharsets.UTF_8));

                b.set("type", type);
                b.set("alias", type);
                b.set("description", old.getInt("Region." + type + ".X Radius") + " radius protected area.");
                b.set("restrict_obtaining", false);
                b.set("world_list_type", "blacklist");
                b.set("worlds", worldsDenied);
                b.set("region.x_radius", old.getInt("Region." + type + ".X Radius"));
                b.set("region.y_radius", old.getInt("Region." + type + ".Y Radius"));
                b.set("region.z_radius", old.getInt("Region." + type + ".Z Radius"));
                b.set("region.flags", flags);
                b.set("region.allowed_flags", allowedFlags);
                b.set("region.priority", old.getInt("Region." + type + ".Priority"));
                b.set("block_data.display_name", "");
                b.set("block_data.lore", new ArrayList<>());
                b.set("behaviour.auto_hide", old.getBoolean("Region." + type + ".Auto Hide"));
                b.set("behaviour.no_drop", old.getBoolean("Region." + type + ".No Drop"));
                b.set("behaviour.prevent_piston_push", old.getBoolean("Region." + type + ".Block Piston"));
                // ignore silk touch option
                b.set("player.prevent_teleport_in", old.getBoolean("Teleport To PVP.Block Teleport"));

                b.save(file);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Bukkit.getLogger().info(ChatColor.GREEN + "Listo!");
        Bukkit.getLogger().info(ChatColor.GREEN + "Asegurate de revisar bien tus configuraciones con las nuevas opciones!");

        Bukkit.getLogger().info(ChatColor.AQUA + "Updating PS Regions to new format...");
        LegacyUpgrade.upgradeRegions();
        Bukkit.getLogger().info(ChatColor.GREEN + "Listo!");
    }

    private static void saveConfig() {
        try {
            OdisseyProtecciones.config.save(OdisseyProtecciones.configLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
