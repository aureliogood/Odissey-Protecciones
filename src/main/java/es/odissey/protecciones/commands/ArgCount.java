package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSGroupRegion;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSPlayer;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ArgCount implements PSCommandArg {

    // Only PS regions, not other regions
    static int[] countRegionsOfPlayer(UUID uuid, World w) {
        int[] count = {0, 0}; // total, including merged

        PSPlayer psp = PSPlayer.fromUUID(uuid);
        psp.getPSRegions(w, false).forEach(r -> {
            count[0]++;
            if (r instanceof PSGroupRegion) {
                count[1] += ((PSGroupRegion) r).getMergedRegions().size();
            }
        });

        return count;
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("count");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.count", "odisseyprotecciones.count.others");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    // /ps count
    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        final World world = p.getWorld();
        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
            int[] count;

            if (args.length == 1) {
                if (!p.hasPermission("odisseyprotecciones.count")) {
                    PSL.msg(p, PSL.NO_PERMISSION_COUNT.msg());
                    return;
                }

                count = countRegionsOfPlayer(p.getUniqueId(), world);
                PSL.msg(p, PSL.PERSONAL_REGION_COUNT.msg().replace("%num%", "" + count[0]));
                if (count[1] != 0) {
                    PSL.msg(p, PSL.PERSONAL_REGION_COUNT_MERGED.msg().replace("%num%", ""+count[1]));
                }

            } else if (args.length == 2) {

                if (!p.hasPermission("odisseyprotecciones.count.others")) {
                    PSL.msg(p, PSL.NO_PERMISSION_COUNT_OTHERS.msg());
                    return;
                }
                if (!UUIDCache.containsName(args[1])) {
                    PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
                    return;
                }

                UUID countUuid = UUIDCache.getUUIDFromName(args[1]);
                count = countRegionsOfPlayer(countUuid, world);

                PSL.msg(p, PSL.OTHER_REGION_COUNT.msg()
                        .replace("%player%", UUIDCache.getNameFromUUID(countUuid))
                        .replace("%num%", "" + count[0]));
                if (count[1] != 0) {
                    PSL.msg(p, PSL.OTHER_REGION_COUNT_MERGED.msg()
                            .replace("%player%", UUIDCache.getNameFromUUID(countUuid))
                            .replace("%num%", "" + count[1]));
                }
            } else {
                PSL.msg(p, PSL.COUNT_HELP.msg());
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
