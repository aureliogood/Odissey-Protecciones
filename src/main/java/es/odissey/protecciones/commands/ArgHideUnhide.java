package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ArgHideUnhide implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Arrays.asList("hide", "unhide");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.hide", "odisseyprotecciones.unhide");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] arg, HashMap<String, String> flags) {
        Player p = (Player) s;
        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());

        // preliminary checks
        if (arg[0].equals("unhide") && !p.hasPermission("odisseyprotecciones.unhide"))
            return PSL.msg(p, PSL.NO_PERMISSION_UNHIDE.msg());

        if (arg[0].equals("hide") && !p.hasPermission("odisseyprotecciones.hide"))
            return PSL.msg(p, PSL.NO_PERMISSION_HIDE.msg());

        if (r == null)
            return PSL.msg(p, PSL.NOT_IN_REGION.msg());

        if (WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false))
            return PSL.msg(p, PSL.NO_ACCESS.msg());

        if (r.isHidden()) {
            if (arg[0].equals("hide")) {
                return PSL.msg(p, PSL.ALREADY_HIDDEN.msg());
            }
            r.unhide();
        } else {
            if (arg[0].equals("unhide")) {
                return PSL.msg(p, PSL.ALREADY_NOT_HIDDEN.msg());
            }
            r.hide();
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
