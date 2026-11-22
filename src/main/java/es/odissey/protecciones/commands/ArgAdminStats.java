package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

class ArgAdminStats {

    // /ps admin stats
    static boolean argumentAdminStats(CommandSender p, String[] args) {
        WorldGuardPlugin wg = WorldGuardPlugin.inst();

        int size = 0;
        for (RegionManager rgm : WGUtils.getAllRegionManagers().values()) {
            size += rgm.getRegions().values().stream().filter(OdisseyProtecciones::isPSRegion).count();
        }

        if (args.length > 2) {
            String playerName = args[2];
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            long count = 0;
            HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();
            java.util.UUID opUuid = wg.wrapOfflinePlayer(op).getUniqueId();
            for (RegionManager rgm : m.values()) {
                count += rgm.getRegions().values().stream()
                        .filter(OdisseyProtecciones::isPSRegion)
                        .filter(r -> r.getOwners().getUniqueIds().contains(opUuid))
                        .count();
            }
            p.sendMessage(ChatColor.YELLOW + playerName + ":");
            p.sendMessage(ChatColor.YELLOW + "================");
            long firstPlayed = (System.currentTimeMillis() - op.getFirstPlayed()) / 86400000L;
            p.sendMessage(ChatColor.YELLOW + "Primera conexion hace " + firstPlayed + " dias.");
            long lastPlayed = (System.currentTimeMillis() - op.getLastPlayed()) / 86400000L;
            p.sendMessage(ChatColor.YELLOW + "Ultima conexion hace " + lastPlayed + " dias.");

            String banMessage = (op.isBanned()) ? "Baneado" : "No baneado";
            p.sendMessage(ChatColor.YELLOW + banMessage);
            p.sendMessage(ChatColor.YELLOW + "Regiones: " + count);
            p.sendMessage(ChatColor.YELLOW + "================");
            return true;
        }

        p.sendMessage(ChatColor.YELLOW + "================");
        p.sendMessage(ChatColor.YELLOW + "Regiones: " + size);
        p.sendMessage(ChatColor.YELLOW + "================");

        return true;
    }
}
