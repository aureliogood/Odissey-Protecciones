package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgName implements PSCommandArg {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("name");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.name");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("odisseyprotecciones.name")) {
            PSL.msg(s, PSL.NO_PERMISSION_NAME.msg());
            return true;
        }
        Player p = (Player) s;
        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());
        if (r == null) {
            PSL.msg(s, PSL.NOT_IN_REGION.msg());
            return true;
        }
        if (WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false)) {
            PSL.msg(s, PSL.NO_ACCESS.msg());
            return true;
        }
        if (args.length < 2) {
            PSL.msg(s, PSL.NAME_HELP.msg());
            return true;
        }

        if (args[1].equals("none")) {
            r.setName(null);
            PSL.msg(p, PSL.NAME_REMOVED.msg().replace("%id%", r.getId()));
        } else {
            if (!OdisseyProtecciones.getInstance().getConfigOptions().allowDuplicateRegionNames && OdisseyProtecciones.isPSNameAlreadyUsed(args[1])) {
                PSL.msg(p, PSL.NAME_TAKEN.msg().replace("%name%", args[1]));
                return true;
            }
            r.setName(args[1]);
            PSL.msg(p, PSL.NAME_SET_NAME.msg().replace("%id%", r.getId()).replace("%name%", r.getName()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}

