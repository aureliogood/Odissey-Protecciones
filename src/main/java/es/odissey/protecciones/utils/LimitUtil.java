package es.odissey.protecciones.utils;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class LimitUtil {

    // warning: group regions should be split into merged regions first
    public static String checkAddOwner(PSPlayer psp, List<PSProtectBlock> blocksAdded) {
        HashMap<PSProtectBlock, Integer> regionLimits = psp.getRegionLimits();
        int maxPS = psp.getGlobalRegionLimits();

        OdisseyProtecciones.getInstance().debug(String.format("Player's global limit is %d.", maxPS));
        OdisseyProtecciones.getInstance().debug(String.format("El jugador tiene limites en %d tipos de region.", regionLimits.size()));

        if (maxPS != -1 || !regionLimits.isEmpty()) { // only check if limit was found

            // count player's protection blocks
            int total = 0;
            HashMap<PSProtectBlock, Integer> playerRegionCounts = getOwnedRegionTypeCounts(psp);

            // add the blocks
            for (PSProtectBlock b : blocksAdded) {
                OdisseyProtecciones.getInstance().debug(String.format("Adding region type %s.", b.alias));
                if (playerRegionCounts.containsKey(b)) {
                    playerRegionCounts.put(b, playerRegionCounts.get(b)+1);
                } else {
                    playerRegionCounts.put(b, 1);
                }
            }

            // check each limit
            for (PSProtectBlock type : playerRegionCounts.keySet()) {
                if (regionLimits.containsKey(type)) {
                    OdisseyProtecciones.getInstance().debug(String.format("Of type %s: player will have %d regions - Player's limit is %d regions.", type.alias, playerRegionCounts.get(type), regionLimits.get(type)));
                    if (playerRegionCounts.get(type) > regionLimits.get(type)) {
                        return PSL.ADDREMOVE_PLAYER_REACHED_LIMIT.msg();
                    }
                }
                total += playerRegionCounts.get(type);
            }

            // check if player has passed region limit
            OdisseyProtecciones.getInstance().debug(String.format("El jugador tendra %d regiones en total. Su limite es %d.", total, maxPS));
            if (total > maxPS && maxPS != -1) {
                return PSL.ADDREMOVE_PLAYER_REACHED_LIMIT.msg();
            }
        }
        return "";
    }

    public static boolean check(Player p, PSProtectBlock b) {
        if (!p.hasPermission("odisseyprotecciones.admin")) {
            // comprueba si el jugador ha superado el limite de bloques de proteccion
            String msg = LimitUtil.hasPlayerPassedRegionLimit(PSPlayer.fromPlayer(p), b);
            if (!msg.isEmpty()) {
                PSL.msg(p, msg);
                return false;
            }
        }

        return true;
    }

    public static boolean hasPassedOrEqualsRentLimit(Player p) {
        int lim = MiscUtil.getPermissionNumber(p, "odisseyprotecciones.rent.limit.", -1);
        if (lim != -1) {
            int total = 0;

            // find total number of rented regions
            HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();
            for (World w : m.keySet()) {
                RegionManager rgm = m.get(w);
                for (ProtectedRegion r : rgm.getRegions().values()) {
                    if (OdisseyProtecciones.isPSRegion(r) && r.getOwners().contains(WorldGuardPlugin.inst().wrapPlayer(p))) {
                        PSRegion psr = PSRegion.fromWGRegion(w, r);

                        if (psr != null && psr.getTenant() != null && psr.getTenant().equals(p.getUniqueId())) total++;
                    }
                }
            }

            return total >= lim;
        }
        return false;
    }

    /**
     * Returns the region counts of a player (for all worlds).
     * @param psp player
     * @return map of region types to the counts
     */
    private static HashMap<PSProtectBlock, Integer> getOwnedRegionTypeCounts(PSPlayer psp) {
        if (OdisseyProtecciones.getInstance().isDebug()) { // psp.getName may incur a performance penalty
            OdisseyProtecciones.getInstance().debug(String.format("Limites de depuracion para: %s", psp.getName()));
        }

        HashMap<PSProtectBlock, Integer> counts = new HashMap<>();
        HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();

        for (World w : m.keySet()) {
            psp.getPSRegions(w, false).forEach(r -> {
                if (r instanceof PSGroupRegion) {
                    OdisseyProtecciones.getInstance().debug(String.format("Checking group region %s's (world %s) (type %s) regions:", r.getId(), w.getName(), r.getTypeOptions().alias));
                    for (PSMergedRegion psmr : ((PSGroupRegion) r).getMergedRegions()) {
                        if (psmr.getTypeOptions() == null) continue;
                        if (!counts.containsKey(psmr.getTypeOptions())) {
                            counts.put(psmr.getTypeOptions(), 1);
                        } else {
                            counts.put(psmr.getTypeOptions(), counts.get(psmr.getTypeOptions())+1);
                        }

                        OdisseyProtecciones.getInstance().debug(String.format("Region fusionada %s (mundo %s) (tipo %s)", psmr.getId(), w.getName(), psmr.getTypeOptions().alias));
                    }
                } else {
                    if (r.getTypeOptions() == null) return;
                    if (!counts.containsKey(r.getTypeOptions())) {
                        counts.put(r.getTypeOptions(), 1);
                    } else {
                        counts.put(r.getTypeOptions(), counts.get(r.getTypeOptions())+1);
                    }
                    OdisseyProtecciones.getInstance().debug(String.format("Region %s (mundo %s) (tipo %s)", r.getId(), w.getName(), r.getTypeOptions().alias));
                }
            });
        }
        return counts;
    }

    private static String hasPlayerPassedRegionLimit(PSPlayer psp, PSProtectBlock b) {
        HashMap<PSProtectBlock, Integer> regionLimits = psp.getRegionLimits();
        int maxPS = psp.getGlobalRegionLimits();

        if (maxPS != -1 || !regionLimits.isEmpty()) { // only check if limit was found

            // cuenta los bloques de proteccion del jugador
            int total = 0, bFound = 0;
            HashMap<PSProtectBlock, Integer> playerRegionCounts = getOwnedRegionTypeCounts(psp);
            for (PSProtectBlock type : playerRegionCounts.keySet()) {
                OdisseyProtecciones.getInstance().debug(String.format("Adding region type %s.", b.alias));
                if (type.equals(b)) {
                    bFound = playerRegionCounts.get(type);
                }
                total += playerRegionCounts.get(type);
            }

            // check if player has passed region limit
            OdisseyProtecciones.getInstance().debug(String.format("El jugador tendra %d regiones en total. Su limite es %d.", total, maxPS));
            if (total >= maxPS && maxPS != -1) {
                return PSL.REACHED_REGION_LIMIT.msg().replace("%limit%", ""+maxPS);
            }

            // check if player has passed per block limit
            OdisseyProtecciones.getInstance().debug(String.format("Of type %s: player will have %d regions - Player's limit is %d regions.", b.alias, bFound, regionLimits.get(b) == null ? -1 : regionLimits.get(b)));
            if (regionLimits.get(b) != null && bFound >= regionLimits.get(b)) {
                return PSL.REACHED_PER_BLOCK_REGION_LIMIT.msg().replace("%limit%", ""+regionLimits.get(b));
            }
        }
        return "";
    }

}
