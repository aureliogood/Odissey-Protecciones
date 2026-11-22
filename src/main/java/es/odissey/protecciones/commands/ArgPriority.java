package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgPriority implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("priority");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.priority");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());

        if (!p.hasPermission("odisseyprotecciones.priority")) {
            PSL.msg(p, PSL.NO_PERMISSION_PRIORITY.msg());
            return true;
        }
        if (r == null) {
            PSL.msg(p, PSL.NOT_IN_REGION.msg());
            return true;
        }
        if (WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false)) {
            PSL.msg(p, PSL.NO_ACCESS.msg());
            return true;
        }
        if (args.length < 2) {
            int priority = r.getWGRegion().getPriority();
            PSL.msg(p, PSL.PRIORITY_INFO.msg().replace("%priority%", "" + priority));
            return true;
        }

        try {
            int priority = Integer.parseInt(args[1]);
            r.getWGRegion().setPriority(priority);
            PSL.msg(p, PSL.PRIORITY_SET.msg());
        } catch (NumberFormatException e) {
            PSL.msg(p, PSL.PRIORITY_ERROR.msg());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
