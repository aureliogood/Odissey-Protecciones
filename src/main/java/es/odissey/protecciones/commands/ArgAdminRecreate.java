package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ArgAdminRecreate {
    static boolean argumentAdminRecreate(CommandSender s, String[] args) {
        s.sendMessage(ChatColor.YELLOW + "Recreando...");

        HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();
        for (World w : m.keySet()) {
            RegionManager rgm = m.get(w);

            List<ProtectedRegion> toAdd = new ArrayList<>();

            for (ProtectedRegion r : rgm.getRegions().values()) {
                if (OdisseyProtecciones.isPSRegion(r)) {
                    PSRegion wr = PSRegion.fromWGRegion(w, r);
                    if (wr instanceof PSGroupRegion) continue; // skip group regions for now TODO
                    PSProtectBlock blockOptions = wr.getTypeOptions();

                    if (blockOptions == null) {
                        Bukkit.getLogger().info("Region " + r.getId() + " in world " + w.getName() + " no esta configurado en la config de bloques! Omitiendo...");
                        continue;
                    }

                    ProtectedRegion nr = WGUtils.getDefaultProtectedRegion(blockOptions, WGUtils.parsePSRegionToLocation(wr.getId()));
                    nr.copyFrom(r); // copy region data over
                    toAdd.add(nr);
                }
            }

            for (ProtectedRegion r : toAdd) {
                rgm.removeRegion(r.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
                rgm.addRegion(r);
            }
        }

        s.sendMessage(ChatColor.YELLOW + "Listo.");
        return true;
    }
}
