package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.OdisseyProtecciones;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgToggle implements PSCommandArg {

    // /ps on
    public static class ArgToggleOn implements PSCommandArg {
        @Override
        public List<String> getNames() {
            return Collections.singletonList("on");
        }
        @Override
        public boolean allowNonPlayersToExecute() {
            return false;
        }
        @Override
        public List<String> getPermissionsToExecute() {
            return Collections.singletonList("odisseyprotecciones.toggle");
        }
        @Override
        public HashMap<String, Boolean> getRegisteredFlags() {
            return null;
        }
        @Override
        public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
            Player p = (Player) s;
            if (p.hasPermission("odisseyprotecciones.toggle")) {
                OdisseyProtecciones.toggleList.remove(p.getUniqueId());
                p.sendMessage(PSL.TOGGLE_ON.msg());
            } else {
                p.sendMessage(PSL.NO_PERMISSION_TOGGLE.msg());
            }
            return true;
        }
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return null;
        }
    }

    // /ps off
    public static class ArgToggleOff implements PSCommandArg {
        @Override
        public List<String> getNames() {
            return Collections.singletonList("off");
        }
        @Override
        public boolean allowNonPlayersToExecute() {
            return false;
        }
        @Override
        public List<String> getPermissionsToExecute() {
            return Collections.singletonList("odisseyprotecciones.toggle");
        }
        @Override
        public HashMap<String, Boolean> getRegisteredFlags() {
            return null;
        }
        @Override
        public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
            Player p = (Player) s;
            if (p.hasPermission("odisseyprotecciones.toggle")) {
                OdisseyProtecciones.toggleList.add(p.getUniqueId());
                p.sendMessage(PSL.TOGGLE_OFF.msg());
            } else {
                p.sendMessage(PSL.NO_PERMISSION_TOGGLE.msg());
            }
            return true;
        }
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return null;
        }
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("toggle");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("odisseyprotecciones.toggle");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        if (p.hasPermission("odisseyprotecciones.toggle")) {
            if (!OdisseyProtecciones.toggleList.contains(p.getUniqueId())) {
                OdisseyProtecciones.toggleList.add(p.getUniqueId());
                p.sendMessage(PSL.TOGGLE_OFF.msg());
            } else {
                OdisseyProtecciones.toggleList.remove(p.getUniqueId());
                p.sendMessage(PSL.TOGGLE_ON.msg());
            }
        } else {
            p.sendMessage(PSL.NO_PERMISSION_TOGGLE.msg());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
