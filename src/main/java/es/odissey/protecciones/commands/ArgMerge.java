package es.odissey.protecciones.commands;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.WGMerge;
import es.odissey.protecciones.utils.WGUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ArgMerge implements PSCommandArg {
    @Override
    public List<String> getNames() {
        return Arrays.asList("merge");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.merge");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    public static List<TextComponent> getGUI(Player p, PSRegion r) {
        return r.getMergeableRegions(p).stream()
                .map(psr -> {
                    TextComponent tc = new TextComponent(ChatColor.AQUA + "> " + ChatColor.WHITE + psr.getId());
                    if (psr.getName() != null) tc.addExtra(" (" + psr.getName() + ")"); // name
                    tc.addExtra(" (" + psr.getTypeOptions().alias + ")"); // region type

                    tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + OdisseyProtecciones.getInstance().getConfigOptions().base_command + " merge " + r.getId() + " " + psr.getId()));
                    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(PSL.MERGE_CLICK_TO_MERGE.msg().replace("%region%", psr.getId())).create()));
                    return tc;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("odisseyprotecciones.merge"))
            return PSL.msg(s, PSL.NO_PERMISSION_MERGE.msg());

        if (!OdisseyProtecciones.getInstance().getConfigOptions().allowMergingRegions)
            return PSL.msg(s, PSL.MERGE_DISABLED.msg());

        Player p = (Player) s;
        if (args.length == 1) { // GUI

            PSRegion r = PSRegion.fromLocationGroup(p.getLocation());
            if (r == null)
                return PSL.msg(s, PSL.NOT_IN_REGION.msg());

            if (r.getTypeOptions() == null) {
                PSL.msg(p, ChatColor.RED + "Esta region tiene un problema y el tipo de bloque (" + r.getType() + ") no esta configurado. Contacta a un administrador.");
                Bukkit.getLogger().info(ChatColor.RED + "Esta region tiene un problema y el tipo de bloque (" + r.getType() + ") no esta configurado.");
                return true;
            }

            if (!r.getTypeOptions().allowMerging)
                return PSL.msg(s, PSL.MERGE_NOT_ALLOWED.msg());

            List<TextComponent> components = getGUI(p, r);
            if (components.isEmpty()) {
                PSL.msg(p, PSL.MERGE_NO_REGIONS.msg());
            } else {
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
                PSL.msg(p, PSL.MERGE_HEADER.msg().replace("%region%", r.getId()));
                PSL.msg(p, PSL.MERGE_WARNING.msg());
                for (TextComponent tc : components) p.spigot().sendMessage(tc);
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
            }

        } else if (args.length == 3) { // /ps merge [region] [root]
            RegionManager rm = WGUtils.getRegionManagerWithPlayer(p);
            if (rm == null)
                return PSL.msg(p, PSL.INVALID_WORLD.msg());
            ProtectedRegion region = rm.getRegion(args[1]), root = rm.getRegion(args[2]);
            LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

            if (!OdisseyProtecciones.isPSRegion(region) || !OdisseyProtecciones.isPSRegion(root))
                return PSL.msg(p, PSL.MULTI_REGION_DOES_NOT_EXIST.msg());

            if (!p.hasPermission("odisseyprotecciones.admin") && (!region.isOwner(lp) || !root.isOwner(lp)))
                return PSL.msg(p, PSL.NO_ACCESS.msg());

            // check if region is actually overlapping the region
            var overlappingRegionIds = WGUtils.findOverlapOrAdjacentRegions(root, rm, p.getWorld()).stream().map(ProtectedRegion::getId).collect(Collectors.toList());
            if (!overlappingRegionIds.contains(region.getId()))
                return PSL.msg(p, PSL.REGION_NOT_OVERLAPPING.msg());

            // check if merging is allowed in config
            PSRegion aRegion = PSRegion.fromWGRegion(p.getWorld(), region), aRoot = PSRegion.fromWGRegion(p.getWorld(), root);
            if (!aRegion.getTypeOptions().allowMerging || !aRoot.getTypeOptions().allowMerging)
                return PSL.msg(p, PSL.MERGE_NOT_ALLOWED.msg());

            // check if the region types allow for it
            if (!WGUtils.canMergeRegionTypes(aRegion.getTypeOptions(), aRoot))
                return PSL.msg(p, PSL.MERGE_NOT_ALLOWED.msg());

            // merge on the main thread (WorldGuard is not thread-safe)
            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
                try {
                    WGMerge.mergeRealRegions(p.getWorld(), rm, aRoot, Arrays.asList(aRegion, aRoot));
                } catch (WGMerge.RegionHoleException e) {
                    PSL.msg(p, PSL.NO_REGION_HOLES.msg());
                    return;
                } catch (WGMerge.RegionCannotMergeWhileRentedException e) {
                    PSL.msg(p, PSL.CANNOT_MERGE_RENTED_REGION.msg().replace("%region%", e.getRentedRegion().getName() == null ? e.getRentedRegion().getId() : e.getRentedRegion().getName()));
                    return;
                }
                PSL.msg(p, PSL.MERGE_MERGED.msg());

                // show menu again if the new region still has overlapping regions
                Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
                    if (!getGUI(p, PSRegion.fromWGRegion(p.getWorld(), rm.getRegion(aRoot.getId()))).isEmpty()) {
                        Bukkit.dispatchCommand(p, OdisseyProtecciones.getInstance().getConfigOptions().base_command + " merge");
                    }
                });
            });

        } else {
            PSL.msg(s, PSL.MERGE_HELP.msg());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
