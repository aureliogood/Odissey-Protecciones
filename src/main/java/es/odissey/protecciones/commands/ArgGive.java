package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSProtectBlock;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.OdisseyProtecciones;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.*;

public class ArgGive implements PSCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("give");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("odisseyprotecciones.give");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender p, String[] args, HashMap<String, String> flags) {
        if (!p.hasPermission("odisseyprotecciones.give"))
            return PSL.msg(p, PSL.NO_PERMISSION_GIVE.msg());

        if (args.length < 3)
            return PSL.msg(p, PSL.GIVE_HELP.msg());

        // check if player online
        if (Bukkit.getPlayer(args[2]) == null)
            return PSL.msg(p, PSL.PLAYER_NOT_FOUND.msg() + " (" + args[2] + ")");

        // check if argument is valid block
        PSProtectBlock cp = OdisseyProtecciones.getProtectBlockFromAlias(args[1]);
        if (cp == null)
            return PSL.msg(p, PSL.INVALID_BLOCK.msg());

        // check if item was able to be added (inventory not full)
        Player ps = Bukkit.getPlayer(args[2]);

        ItemStack item = cp.createItem();
        if (args.length >= 4 && NumberUtils.isNumber(args[3]))
            item.setAmount(Integer.parseInt(args[3]));

        if (!ps.getInventory().addItem(item).isEmpty()) {
            if (OdisseyProtecciones.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                PSL.msg(ps, PSL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                ps.getWorld().dropItem(ps.getLocation(), cp.createItem());
            } else {
                return PSL.msg(p, PSL.GIVE_NO_INVENTORY_ROOM.msg());
            }
        }

        return PSL.msg(p, PSL.GIVE_GIVEN.msg().replace("%block%", args[1]).replace("%player%", Bukkit.getPlayer(args[2]).getDisplayName()));
    }

    // tab completion
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> l = new ArrayList<>();
        if (args.length == 2) {
            for (PSProtectBlock b : OdisseyProtecciones.getInstance().getConfiguredBlocks()) l.add(b.alias);
            return StringUtil.copyPartialMatches(args[1], l, new ArrayList<>());
        } else if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) l.add(p.getName());
            return StringUtil.copyPartialMatches(args[2], l, new ArrayList<>());
        }
        return null;
    }

}
