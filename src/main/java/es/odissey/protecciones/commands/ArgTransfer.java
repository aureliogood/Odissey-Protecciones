package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSGroupRegion;
import es.odissey.protecciones.PSPlayer;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * /ps transfer [jugador] - transfiere la proteccion actual a otro jugador.
 */
public class ArgTransfer implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("transfer");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("odisseyprotecciones.transfer");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        if (!p.hasPermission("odisseyprotecciones.transfer")) {
            return PSL.msg(p, PSL.NO_PERMISSION_TRANSFER.msg());
        }
        if (args.length < 2) {
            return PSL.msg(p, PSL.TRANSFER_HELP.msg());
        }
        if (!UUIDCache.containsName(args[1])) {
            return PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
        }

        UUID targetUuid = UUIDCache.getUUIDFromName(args[1]);
        if (targetUuid == null) {
            return PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg());
        }
        if (targetUuid.equals(p.getUniqueId())) {
            return PSL.msg(p, PSL.TRANSFER_SELF.msg());
        }

        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());
        if (r == null) {
            return PSL.msg(p, PSL.NOT_IN_REGION.msg());
        }
        if (WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false)) {
            return PSL.msg(p, PSL.NO_ACCESS.msg());
        }
        if (!r.isOwner(p.getUniqueId())) {
            return PSL.msg(p, PSL.TRANSFER_NOT_OWNER.msg());
        }
        if (r.getTenant() != null) {
            return PSL.msg(p, PSL.TRANSFER_RENTED.msg());
        }

        List<PSRegion> targets = new ArrayList<>();
        if (r instanceof PSGroupRegion group) {
            targets.addAll(group.getMergedRegions());
        } else {
            targets.add(r);
        }

        if (new ArgAddRemove().determinePlayerSurpassedLimit(p, targets, PSPlayer.fromUUID(targetUuid))) {
            return true;
        }

        String targetName = UUIDCache.getNameFromUUID(targetUuid);
        for (PSRegion region : targets) {
            if (!region.isOwner(p.getUniqueId())) continue;
            region.removeOwner(p.getUniqueId());
            region.addOwner(targetUuid);
            if (region.isMember(targetUuid)) {
                region.removeMember(targetUuid);
            }
        }

        PSL.msg(p, PSL.TRANSFER_SUCCESS.msg().replace("%player%", targetName));

        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            PSL.msg(online, PSL.TRANSFER_RECEIVED.msg().replace("%player%", p.getName()));
        }

        try {
            es.odissey.protecciones.storage.Database db = OdisseyProtecciones.getInstance().getDatabase();
            if (db != null && db.isEnabled()) {
                for (PSRegion region : targets) {
                    // las subregiones fusionadas no existen en WorldGuard: getWGRegion() devuelve
                    // una region sintetica y guardarla haria que restoreMissing creara cuboides
                    // gigantes al reiniciar. Solo se respalda la region raiz.
                    if (region instanceof es.odissey.protecciones.PSMergedRegion) continue;
                    db.saveRegionAsync(es.odissey.protecciones.storage.RegionSnapshot.from(
                            region.getWorld(), region.getWGRegion()));
                    db.audit(p.getUniqueId().toString(), p.getName(), "transfer",
                            region.getWorld().getName(), region.getId(), targetUuid.toString());
                }
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 2) return null;
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(((Player) sender).getUniqueId())) continue;
            if (((Player) sender).canSee(online)) {
                names.add(online.getName());
            }
        }
        try {
            return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
