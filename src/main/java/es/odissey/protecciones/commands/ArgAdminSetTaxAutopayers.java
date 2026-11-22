package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

class ArgAdminSetTaxAutopayers {
    static boolean argumentAdminSetTaxAutopayers(CommandSender s, String[] args) {
        if (!OdisseyProtecciones.getInstance().getConfigOptions().taxEnabled) {
            return PSL.msg(s, ChatColor.RED + "Los impuestos estan deshabilitados! Activalos en la config.");
        }

        PSL.msg(s, ChatColor.GRAY + "Buscando regiones y estableciendo el pago automatico de impuestos donde no lo tengan...");

        // run on the main thread (WorldGuard is not thread-safe)
        Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
            WGUtils.getAllRegionManagers().forEach((w, rgm) -> {
                for (ProtectedRegion r : rgm.getRegions().values()) {
                    PSRegion psr = PSRegion.fromWGRegion(w, r);

                    if (psr != null && psr.getTypeOptions() != null && psr.getTypeOptions().taxPeriod != -1 && psr.getTaxAutopayer() == null) {
                        if (psr.getOwners().size() >= 1) {
                            PSL.msg(s, ChatColor.GRAY + "Se configuro el pago automatico de impuestos para " + psr.getOwners().get(0).toString() + " en la region " + psr.getId());
                            psr.setTaxAutopayer(psr.getOwners().get(0));
                        }
                    }

                }
            });
            PSL.msg(s, ChatColor.GREEN + "Completado!");
        });

        return true;
    }
}
