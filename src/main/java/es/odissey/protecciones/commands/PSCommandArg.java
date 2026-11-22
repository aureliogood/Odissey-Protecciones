package es.odissey.protecciones.commands;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;

public interface PSCommandArg {
    List<String> getNames();
    boolean allowNonPlayersToExecute();
    List<String> getPermissionsToExecute();
    HashMap<String, Boolean> getRegisteredFlags(); // <flag, has value after>
    boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags);
    List<String> tabComplete(CommandSender sender, String alias, String[] args);
}
