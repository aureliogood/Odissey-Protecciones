package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSPlayer;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ArgList implements PSCommandArg {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("list");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.list");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("odisseyprotecciones.list"))
            return PSL.msg(s, PSL.NO_PERMISSION_LIST.msg());

        if (args.length == 2 && !s.hasPermission("odisseyprotecciones.list.others"))
            return PSL.msg(s, PSL.NO_PERMISSION_LIST_OTHERS.msg());

        if (args.length == 2 && !UUIDCache.containsName(args[1]))
            return PSL.msg(s, PSL.PLAYER_NOT_FOUND.msg());

        PSPlayer psp = PSPlayer.fromPlayer((Player) s);
        final org.bukkit.World world = psp.getPlayer().getWorld();

        // run query async to reduce load
        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
            if (args.length == 1) {
                List<PSRegion> regions = psp.getPSRegionsCrossWorld(world, true);
                display(s, regions, psp.getUuid(), true);
            } else if (args.length == 2) {
                UUID uuid = UUIDCache.getUUIDFromName(args[1]);
                List<PSRegion> regions = PSPlayer.fromUUID(uuid).getPSRegionsCrossWorld(world, true);
                display(s, regions, uuid, false);
            } else {
                PSL.msg(s, PSL.LIST_HELP.msg());
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission("odisseyprotecciones.list") || !sender.hasPermission("odisseyprotecciones.list.others")) {
            return null;
        }
        if (args.length == 2) {
            // autocomplete with online player list
            return StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList()), new ArrayList<>());
        }

        return null;
    }

    private void display(CommandSender s, List<PSRegion> regions, UUID pUUID, boolean isCurrentPlayer) {
        List<String> ownerOf = new ArrayList<>(), memberOf = new ArrayList<>();
        for (PSRegion r : regions) {
            if (r.isOwner(pUUID)) {
                if (r.getName() == null) {
                    ownerOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getId());
                } else {
                    ownerOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getName() + " (" + r.getId() + ")");
                }
            }
            if (r.isMember(pUUID)) {
                if (r.getName() == null) {
                    memberOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getId());
                } else {
                    memberOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getName() + " (" + r.getId() + ")");
                }
            }
        }

        if (ownerOf.isEmpty() && memberOf.isEmpty()) {
            if (isCurrentPlayer) {
                PSL.msg(s, PSL.LIST_NO_REGIONS.msg());
            } else {
                PSL.msg(s, PSL.LIST_NO_REGIONS_PLAYER.msg().replace("%player%", UUIDCache.getNameFromUUID(pUUID)));
            }
            return;
        }

        PSL.msg(s, PSL.LIST_HEADER.msg().replace("%player%", UUIDCache.getNameFromUUID(pUUID)));

        if (!ownerOf.isEmpty()) {
            PSL.msg(s, PSL.LIST_OWNER.msg());
            for (String str : ownerOf) s.sendMessage(str);
        }
        if (!memberOf.isEmpty()) {
            PSL.msg(s, PSL.LIST_MEMBER.msg());
            for (String str : memberOf) s.sendMessage(str);
        }
    }

}
