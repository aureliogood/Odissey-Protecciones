package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgSethome implements PSCommandArg {

    // /ps sethome

    @Override
    public List<String> getNames() {
        return Collections.singletonList("sethome");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.sethome");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());

        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        if (!p.hasPermission("odisseyprotecciones.sethome"))
            return PSL.msg(p, PSL.NO_PERMISSION_SETHOME.msg());

        if (r == null)
            return PSL.msg(p, PSL.NOT_IN_REGION.msg());

        if (WGUtils.hasNoAccess(r.getWGRegion(), p, wg.wrapPlayer(p), false))
            return PSL.msg(p, PSL.NO_ACCESS.msg());

        Location l = p.getLocation();
        r.setHome(l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
        return PSL.msg(p, PSL.SETHOME_SET.msg().replace("%psid%", r.getName() != null ? String.format("%s (%s)", r.getName(), r.getId()) : r.getId()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
