package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.gui.ColorUtil;
import es.odissey.protecciones.gui.MenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// /ps shop [reembolsar]
public class ArgShop implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Arrays.asList("shop", "tienda");
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
        MenuManager manager = MenuManager.getInstance();
        if (!manager.isEnabled()) {
            PSL.msg(s, ColorUtil.color("&cEl sistema de menus esta deshabilitado."));
            return true;
        }
        if (!manager.shopEnabled()) {
            PSL.msg(s, ColorUtil.color("&cLa tienda esta deshabilitada."));
            return true;
        }

        Player p = (Player) s;

        // /ps shop reembolsar
        if (args.length >= 2 && (args[1].equalsIgnoreCase("reembolsar") || args[1].equalsIgnoreCase("refund"))) {
            if (!manager.refundEnabled()) {
                PSL.msg(p, ColorUtil.color("&cLos reembolsos estan deshabilitados."));
                return true;
            }
            if (!p.hasPermission("odisseyprotecciones.refund")) {
                PSL.msg(p, ColorUtil.color("&cNo tienes permiso para reembolsar protecciones."));
                return true;
            }
            manager.open(p, "shop-refund", null, null, null);
            return true;
        }

        if (!p.hasPermission("odisseyprotecciones.shop")) {
            PSL.msg(p, ColorUtil.color("&cNo tienes permiso para abrir la tienda."));
            return true;
        }

        manager.open(p, "shop", null, null, null);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 2) {
            return Collections.singletonList("reembolsar");
        }
        return null;
    }
}
