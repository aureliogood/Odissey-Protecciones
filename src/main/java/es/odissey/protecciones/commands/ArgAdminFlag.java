package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.FlagHandler;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

class ArgAdminFlag {
    static boolean argumentAdminFlag(CommandSender p, String[] args) {

        if (args.length < 5) {
            PSL.msg(p, ArgAdmin.getFlagHelp());
            return true;
        }

        String flag, value = "", gee = "";
        World w = Bukkit.getWorld(args[2]);
        if (w == null)
            return PSL.msg(p, PSL.INVALID_WORLD.msg());

        if (args[3].equalsIgnoreCase("-g")) {
            if (args.length < 6) {
                PSL.msg(p, ArgAdmin.getFlagHelp());
                return true;
            }
            flag = args[5];
            for (int i = 6; i < args.length; i++) value += args[i] + " ";
            gee = args[4];
        } else {
            flag = args[3];
            for (int i = 4; i < args.length; i++) value += args[i] + " ";
        }

        if (WGUtils.getFlagRegistry().get(flag) == null)
            return PSL.msg(p, PSL.FLAG_NOT_SET.msg());

        final String fValue = value, fGee = gee;
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null)
            return PSL.msg(p, PSL.INVALID_WORLD.msg());
        for (ProtectedRegion r : rgm.getRegions().values()) {
            if (OdisseyProtecciones.isPSRegion(r) && PSRegion.fromWGRegion(w, r) != null) {
                String flagValue = fValue;

                // apply %player% placeholder
                if (FlagHandler.getPlayerPlaceholderFlags().contains(flag) && (!r.getOwners().getUniqueIds().isEmpty())) {
                    String name = UUIDCache.getNameFromUUID(r.getOwners().getUniqueIds().stream().findFirst().get());
                    if (name != null) {
                        flagValue = flagValue.replace("%player%", name);
                    }
                }

                ArgFlag.setFlag(PSRegion.fromWGRegion(w, r), p, flag, flagValue.trim(), fGee);
            }
        }
        return true;
    }
}
