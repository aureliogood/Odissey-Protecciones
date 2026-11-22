package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

class ArgAdminChangeblock {

    // /ps admin changeblock [world] [fromblockalias] [toblockalias]
    static boolean argumentAdminChangeblock(CommandSender p, String[] args) {
        if (args.length < 5) {
            PSL.msg(p, ArgAdmin.getChangeBlockHelp());
            return true;
        }

        String world = args[2], fromBlockAlias = args[3], toBlockAlias = args[4];
        if (OdisseyProtecciones.getProtectBlockFromAlias(fromBlockAlias) == null) {
            PSL.msg(p, ChatColor.GRAY + "El tipo de origen no es un bloque de proteccion registrado!");
            return true;
        }
        if (OdisseyProtecciones.getProtectBlockFromAlias(toBlockAlias) == null) {
            PSL.msg(p, ChatColor.GRAY + "El tipo de destino no es un bloque de proteccion registrado!");
            return true;
        }

        String fromBlock = OdisseyProtecciones.getProtectBlockFromAlias(fromBlockAlias).type,
                toBlock = OdisseyProtecciones.getProtectBlockFromAlias(toBlockAlias).type;

        Consumer<PSRegion> convertFunction = (region) -> {
            if (region.getType().equals(fromBlock)) {
                p.sendMessage(ChatColor.GRAY + "Cambiando " + region.getId() + "...");

                region.setType(OdisseyProtecciones.getBlockOptions(toBlock));
            }
        };

        World w = Bukkit.getWorld(world);
        if (w == null) {
            return PSL.msg(p, PSL.INVALID_WORLD.msg());
        }
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) {
            return PSL.msg(p, ChatColor.GRAY + "El mundo no tiene WorldGuard configurado!");
        }
        for (ProtectedRegion r : rgm.getRegions().values()) {
            if (OdisseyProtecciones.isPSRegion(r)) {
                PSRegion pr = PSRegion.fromWGRegion(w, r);

                convertFunction.accept(pr);

                if (pr instanceof PSGroupRegion) {
                    for (PSMergedRegion psmr : ((PSGroupRegion) pr).getMergedRegions()) {
                        convertFunction.accept(psmr);
                    }
                }
            }
        }

        p.sendMessage(ChatColor.GRAY + "Listo!");

        return true;
    }
}
