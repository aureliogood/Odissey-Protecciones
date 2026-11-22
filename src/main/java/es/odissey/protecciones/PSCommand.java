package es.odissey.protecciones;

import es.odissey.protecciones.commands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PSCommand extends Command {

    PSCommand(String name) {
        super(name);
    }

    static void addDefaultArguments() {
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgAddRemove());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgAdmin());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgBuySell());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgCount());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgFlag());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgGet());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgGive());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgHideUnhide());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgHome());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgInfo());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgList());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgMerge());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgMenu());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgShop());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgName());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgPriority());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgRegion());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgReload());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgRent());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgSethome());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgSetparent());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgTax());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgTransfer());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgToggle());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgToggle.ArgToggleOn());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgToggle.ArgToggleOff());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgTp());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgUnclaim());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgView());
        OdisseyProtecciones.getInstance().addCommandArgument(new ArgHelp());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            List<String> l = new ArrayList<>();
            for (PSCommandArg ps : OdisseyProtecciones.getInstance().getCommandArguments()) {
                boolean hasPerm = false;
                if (ps.getPermissionsToExecute() == null) {
                    hasPerm = true;
                } else {
                    for (String perm : ps.getPermissionsToExecute()) {
                        if (sender.hasPermission(perm)) {
                            hasPerm = true;
                            break;
                        }
                    }
                }
                if (hasPerm) l.addAll(ps.getNames());
            }
            return StringUtil.copyPartialMatches(args[0], l, new ArrayList<>());
        } else if (args.length >= 2) {
            for (PSCommandArg ps : OdisseyProtecciones.getInstance().getCommandArguments()) {
                for (String arg : ps.getNames()) {
                    if (arg.equalsIgnoreCase(args[0])) {
                        return ps.tabComplete(sender, alias, args);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (args.length == 0) { // no arguments
            if (s instanceof ConsoleCommandSender || !(s instanceof Player)) {
                s.sendMessage(ChatColor.RED + "Solo puedes usar /ps reload, /ps admin y /ps give desde la consola.");
            } else if (es.odissey.protecciones.gui.MenuManager.getInstance().isEnabled()
                    && es.odissey.protecciones.gui.MenuManager.getInstance().openOnCommand()) {
                es.odissey.protecciones.gui.MenuManager.getInstance().openMain((Player) s);
            } else {
                new ArgHelp().executeArgument(s, args, null);
            }
            return true;
        }
        for (PSCommandArg command : OdisseyProtecciones.getInstance().getCommandArguments()) {
            if (command.getNames().contains(args[0])) {
                // defensa en profundidad: aunque cada comando valida por su cuenta, no se
                // despacha nada cuyo permiso declarado no tenga el emisor
                if (!hasAnyPermission(s, command)) {
                    return PSL.msg(s, PSL.NO_PERMISSION_ADMIN.msg());
                }
                if (command.allowNonPlayersToExecute() || s instanceof Player) {

                    // extract flags
                    List<String> nArgs = new ArrayList<>();
                    HashMap<String, String> flags = new HashMap<>();
                    for (int i = 0; i < args.length; i++) {

                        if (command.getRegisteredFlags() != null && command.getRegisteredFlags().containsKey(args[i])) {
                            if (command.getRegisteredFlags().get(args[i])) { // has value after
                                if (i != args.length-1) {
                                    flags.put(args[i], args[++i]);
                                }
                            } else {
                                flags.put(args[i], null);
                            }
                        } else {
                            nArgs.add(args[i]);
                        }
                    }

                    return command.executeArgument(s, nArgs.toArray(new String[0]), flags);
                } else if (!command.allowNonPlayersToExecute()) {
                    s.sendMessage(ChatColor.RED + "Solo puedes usar /ps reload, /ps admin y /ps give desde la consola.");
                    return true;
                }
            }
        }

        PSL.msg(s, PSL.NO_SUCH_COMMAND.msg());
        return true;
    }

    private static boolean hasAnyPermission(CommandSender sender, PSCommandArg command) {
        List<String> permissions = command.getPermissionsToExecute();
        if (permissions == null || permissions.isEmpty()) return true;
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) return true;
        }
        return false;
    }
}
