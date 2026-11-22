package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.gui.ColorUtil;
import es.odissey.protecciones.gui.MenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// /ps menu
public class ArgMenu implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Arrays.asList("menu", "gui");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return null;
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!MenuManager.getInstance().isEnabled()) {
            PSL.msg(s, ColorUtil.color("&cEl sistema de menus esta deshabilitado."));
            return true;
        }
        MenuManager.getInstance().openMain((Player) s);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
