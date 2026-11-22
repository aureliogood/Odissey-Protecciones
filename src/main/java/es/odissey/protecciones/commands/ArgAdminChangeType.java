package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.FlagHandler;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

class ArgAdminChangeType {

    // /ps admin changeregiontype [world] [fromblocktype] [toblocktype]
    static boolean argumentAdminChangeType(CommandSender p, String[] args) {
        if (args.length < 5) {
            return PSL.msg(p, ArgAdmin.getChangeRegionTypeHelp());
        }

        World w = Bukkit.getWorld(args[2]);
        if (w == null) {
            return PSL.msg(p, PSL.INVALID_WORLD.msg());
        }

        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) {
            return PSL.msg(p, ChatColor.GRAY + "El mundo no tiene WorldGuard configurado!");
        }

        String fromType = args[3], toType = args[4];

        // loop through and update flags manually (do not rely on PSRegion API, since this can include invalid regions)
        for (ProtectedRegion r : rgm.getRegions().values()) {
            // update block material
            if (r.getFlag(FlagHandler.PS_BLOCK_MATERIAL) != null && r.getFlag(FlagHandler.PS_BLOCK_MATERIAL).equals(fromType)) {
                r.setFlag(FlagHandler.PS_BLOCK_MATERIAL, toType);
                p.sendMessage(ChatColor.GRAY + "Region actualizada " + r.getId());
            }
            // update merged regions
            if (r.getFlag(FlagHandler.PS_MERGED_REGIONS_TYPES) != null) {
                Set<String> s = new HashSet<>();
                for (String entry : r.getFlag(FlagHandler.PS_MERGED_REGIONS_TYPES)) {
                    String[] spl = entry.split(" ");

                    if (spl.length == 2) {
                        if (spl[1].equals(fromType)) { // if it is of the type to change
                            p.sendMessage(ChatColor.GRAY + "Region fusionada actualizada " + spl[0]);
                            s.add(spl[0] + " " + toType);
                        } else {
                            s.add(entry);
                        }
                    }

                }

                r.setFlag(FlagHandler.PS_MERGED_REGIONS_TYPES, s);
            }
        }

        p.sendMessage(ChatColor.GREEN + "Terminado!");
        p.sendMessage(ChatColor.GRAY + "Deberias reiniciar el servidor y asegurarte de que el nuevo tipo de bloque este configurado en la config.");
        return true;
    }

}
