package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class ArgAdminHide {

    // /ps admin hide
    static boolean argumentAdminHide(CommandSender p, String[] args) {
        RegionManager mgr;
        World w;
        if (p instanceof Player) {
            mgr = WGUtils.getRegionManagerWithPlayer((Player) p);
            w = ((Player) p).getWorld();
        } else {
            if (args.length != 3) {
                PSL.msg(p, PSL.ADMIN_CONSOLE_WORLD.msg());
                return true;
            }
            if (Bukkit.getWorld(args[2]) == null) {
                PSL.msg(p, PSL.INVALID_WORLD.msg());
                return true;
            }
            w = Bukkit.getWorld(args[2]);
            mgr = WGUtils.getRegionManagerWithWorld(w);
        }

        final World world = w;
        final RegionManager manager = mgr;
        if (manager == null) {
            PSL.msg(p, PSL.INVALID_WORLD.msg());
            return true;
        }
        final String action = args[1].toLowerCase();
        // run on the main thread (WorldGuard + Bukkit block changes are not thread-safe)
        Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
            // recorre las regiones de proteccion y oculta o muestra el bloque
            for (ProtectedRegion r : new java.util.ArrayList<>(manager.getRegions().values())) {
                if (OdisseyProtecciones.isPSRegion(r)) {
                    PSRegion region = PSRegion.fromWGRegion(world, r);
                    if (region == null) continue;
                    if (action.equals("hide")) {
                        region.hide();
                    } else if (action.equals("unhide")) {
                        region.unhide();
                    }
                }
            }

            String hMessage = action.equals("unhide") ? "unhidden" : "hidden";
            PSL.msg(p, PSL.ADMIN_HIDE_TOGGLED.msg()
                    .replace("%message%", hMessage));
        });

        return true;
    }
}
