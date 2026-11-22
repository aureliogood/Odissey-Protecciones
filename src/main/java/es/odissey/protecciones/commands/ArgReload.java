package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.OdisseyProtecciones;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgReload implements PSCommandArg {

    // /ps reload

    @Override
    public List<String> getNames() {
        return Collections.singletonList("reload");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.admin");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender p, String[] args, HashMap<String, String> flags) {
        if (!p.hasPermission("odisseyprotecciones.admin")) {
            PSL.msg(p, PSL.NO_PERMISSION_ADMIN.msg());
            return true;
        }
        PSL.msg(p, PSL.RELOAD_START.msg());
        OdisseyProtecciones.loadConfig(true);
        PSL.msg(p, PSL.RELOAD_COMPLETE.msg());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
