package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class ArgRegion implements PSCommandArg {

    // /ps region

    @Override
    public List<String> getNames() {
        return Collections.singletonList("region");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.region");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        RegionManager rgm = WGUtils.getRegionManagerWithPlayer(p);

        if (!p.hasPermission("odisseyprotecciones.region")) {
            PSL.msg(p, PSL.NO_PERMISSION_REGION.msg());
            return true;
        }

        if (rgm == null) {
            PSL.msg(p, PSL.INVALID_WORLD.msg());
            return true;
        }

        if (args.length < 3) {
            PSL.msg(p, PSL.REGION_HELP.msg());
            return true;
        }

        if (!UUIDCache.containsName(args[2])) {
            PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
            return true;
        }

        UUID playerUuid = UUIDCache.getUUIDFromName(args[2]);

        if (args[1].equalsIgnoreCase("list")) { // list player's regions
            StringBuilder regionMessage = new StringBuilder();
            boolean found = false;
            for (ProtectedRegion r : rgm.getRegions().values()) {
                if (OdisseyProtecciones.isPSRegion(r) && r.getOwners().contains(playerUuid)) {
                    found = true;
                    regionMessage.append(r.getId()).append(", ");
                }
            }

            if (!found) {
                PSL.msg(p, PSL.REGION_NOT_FOUND_FOR_PLAYER.msg()
                        .replace("%player%", args[2]));
            } else {
                regionMessage = new StringBuilder(regionMessage.substring(0, regionMessage.length() - 2) + ".");
                PSL.msg(p, PSL.REGION_LIST.msg()
                        .replace("%player%", args[2])
                        .replace("%regions%", regionMessage));
            }

        } else if ((args[1].equalsIgnoreCase("remove")) || (args[1].equalsIgnoreCase("disown"))) {

            // recopila las regiones primero para no modificar la coleccion mientras se itera
            List<PSRegion> ownedRegions = new ArrayList<>();
            for (ProtectedRegion r : rgm.getRegions().values()) {
                if (OdisseyProtecciones.isPSRegion(r)) {
                    PSRegion psr = PSRegion.fromWGRegion(p.getWorld(), r);
                    if (psr != null && psr.isOwner(playerUuid)) {
                        ownedRegions.add(psr);
                    }
                }
            }

            boolean found = !ownedRegions.isEmpty();

            for (PSRegion psr : ownedRegions) {
                // remove as owner
                psr.removeOwner(playerUuid);

                // remove region if empty and is "remove" mode
                if (psr.getOwners().size() == 0 && args[1].equalsIgnoreCase("remove")) {
                    psr.deleteRegion(true);
                }
            }

            if (!found) {
                PSL.msg(p, PSL.REGION_NOT_FOUND_FOR_PLAYER.msg().replace("%player%", args[2]));
                return true;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                PSL.msg(p, PSL.REGION_REMOVE.msg().replace("%player%", args[2]));
            } else if (args[1].equalsIgnoreCase("disown")) {
                PSL.msg(p, PSL.REGION_DISOWN.msg().replace("%player%", args[2]));
            }
        } else {
            PSL.msg(p, PSL.REGION_HELP.msg());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return args.length == 2 ? StringUtil.copyPartialMatches(args[1], Arrays.asList("disown", "remove", "list"), new ArrayList<>()) : null;
    }
}
