package es.odissey.protecciones;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.commands.ArgMerge;
import es.odissey.protecciones.event.PSCreateEvent;
import es.odissey.protecciones.utils.LimitUtil;
import es.odissey.protecciones.utils.MiscUtil;
import es.odissey.protecciones.utils.WGMerge;
import es.odissey.protecciones.utils.WGUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BlockHandler {
    private static HashMap<UUID, Double> lastProtectStonePlaced = new HashMap<>();

    private static String checkCooldown(Player p) {
        double currentTime = System.currentTimeMillis();
        UUID uuid = p.getUniqueId();
        if (lastProtectStonePlaced.containsKey(uuid)) {
            double cooldown = OdisseyProtecciones.getInstance().getConfigOptions().placingCooldown; // seconds
            double lastPlace = lastProtectStonePlaced.get(uuid); // milliseconds

            if (lastPlace + cooldown * 1000 > currentTime) { // if cooldown has not been finished
                return String.format("%.1f", cooldown - ((currentTime - lastPlace) / 1000));
            }
            lastProtectStonePlaced.remove(uuid);
        }
        lastProtectStonePlaced.put(uuid, currentTime);
        return null;
    }

    private static boolean isFarEnoughFromOtherClaims(PSProtectBlock blockOptions, World w, LocalPlayer lp, double bx, double by, double bz) {
        BlockVector3 min = WGUtils.getMinVector(bx, by, bz, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims);
        BlockVector3 max = WGUtils.getMaxVector(bx, by, bz, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims);

        ProtectedRegion td = new ProtectedCuboidRegion("regionRadiusTest" + (long) (bx + by + bz), true, min, max);
        td.setPriority(blockOptions.priority);
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);

        // if the radius test region overlaps an unowned region
        if (rgm.overlapsUnownedRegion(td, lp)) {
            for (ProtectedRegion rg : rgm.getApplicableRegions(td)) {
                // skip if the user is already an owner
                if (rg.isOwner(lp)) continue;

                if (OdisseyProtecciones.isPSRegion(rg) && rg.getFlag(Flags.PASSTHROUGH) != StateFlag.State.ALLOW) {
                    // if it is a PS region, and "passthrough allow" is not set, then it is not far enough
                    return false;
                } else if (rg.getPriority() >= td.getPriority()) {
                    // if the priorities are the same for plain WorldGuard regions, it is not far enough
                    return false;
                }
            }
        }

        return true;
    }

    // create PS region from a block place event
    public static void createPSRegion(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        // comprueba si el bloque es un bloque de proteccion
        if (!OdisseyProtecciones.isProtectBlockType(b)) return;
        PSProtectBlock blockOptions = OdisseyProtecciones.getBlockOptions(b);

        // comprueba si el objeto fue creado por OdisseyProtecciones (guardado en un tag personalizado)
        // el bloque debe tener restrictObtaining activado para bloquear la colocacion
        if (blockOptions.restrictObtaining && !OdisseyProtecciones.isProtectBlockItem(e.getItemInHand(), true)) return;

        // comprueba si el jugador ha desactivado la colocacion de bloques de proteccion
        if (OdisseyProtecciones.toggleList.contains(p.getUniqueId())) return;

        // check if player can place block in that area
        if (!WorldGuardPlugin.inst().createProtectionQuery().testBlockPlace(p, b.getLocation(), b.getType())) {
            PSL.msg(p, PSL.CANT_PROTECT_THAT.msg());
            e.setCancelled(true);
            return;
        }

        // check if it is in a WorldGuard region
        RegionManager rgm = WGUtils.getRegionManagerWithPlayer(p);
        if (!blockOptions.allowPlacingInWild && rgm.getApplicableRegions(BlockVector3.at(b.getLocation().getX(), b.getLocation().getY(), b.getLocation().getZ())).size() == 0) {
            PSL.msg(p, PSL.MUST_BE_PLACED_IN_EXISTING_REGION.msg());
            e.setCancelled(true);
            return;
        }

        // create region, and cancel if it fails
        if (!createPSRegion(p, b.getLocation(), blockOptions)) {
            e.setCancelled(true);
        }
    }

    // create a PS region (no checks for items)
    public static boolean createPSRegion(Player p, Location l, PSProtectBlock blockOptions) {
        // check permission
        if (!p.hasPermission("odisseyprotecciones.create")) {
            PSL.msg(p, PSL.NO_PERMISSION_CREATE.msg());
            return false;
        }
        if (!blockOptions.permission.equals("") && !p.hasPermission(blockOptions.permission)) {
            PSL.msg(p, PSL.NO_PERMISSION_CREATE_SPECIFIC.msg());
            return false;
        }

        // check cooldown
        if (OdisseyProtecciones.getInstance().getConfigOptions().placingCooldown != -1) {
            String time = checkCooldown(p);
            if (time != null) {
                PSL.msg(p, PSL.COOLDOWN.msg().replace("%time%", time));
                return false;
            }
        }

        // check if player reached region limit
        if (!LimitUtil.check(p, blockOptions)) {
            return false;
        }

        // non-admin checks
        if (!p.hasPermission("odisseyprotecciones.admin")) {
            // check if in world blacklist or not in world whitelist
            boolean containsWorld = blockOptions.worlds.contains(p.getLocation().getWorld().getName());

            if ((containsWorld && blockOptions.worldListType.equalsIgnoreCase("blacklist")) || (!containsWorld && blockOptions.worldListType.equalsIgnoreCase("whitelist"))) {
                if (blockOptions.preventBlockPlaceInRestrictedWorld) {
                    PSL.msg(p, PSL.WORLD_DENIED_CREATE.msg());
                    return false;
                } else {
                    return true;
                }
            }

        } // end of non-admin checks

        // check if player has enough money
        if (OdisseyProtecciones.getInstance().isVaultSupportEnabled() && blockOptions.costToPlace != 0 && !OdisseyProtecciones.getInstance().getVaultEconomy().has(p, blockOptions.costToPlace)) {
            PSL.msg(p, PSL.NOT_ENOUGH_MONEY.msg().replace("%price%", String.format("%.2f", blockOptions.costToPlace)));
            return false;
        }

        // debug message
        if (!OdisseyProtecciones.getInstance().isVaultSupportEnabled() && blockOptions.costToPlace != 0) {
            OdisseyProtecciones.getPluginLogger().info("Vault no esta habilitado pero hay un precio establecido en la colocacion del bloque de proteccion! No funcionara!");
        }

        // comprueba el saldo ANTES de crear la region: asi no se dispara PSCreateEvent
        // ni hay que revertir region/holograma/respaldo si el cobro falla
        if (OdisseyProtecciones.getInstance().isVaultSupportEnabled() && blockOptions.costToPlace != 0) {
            if (!OdisseyProtecciones.getInstance().getVaultEconomy().has(p, blockOptions.costToPlace)) {
                PSL.msg(p, PSL.NOT_ENOUGH_MONEY.msg()
                        .replace("%price%", String.format("%.2f", blockOptions.costToPlace)));
                return false;
            }
        }

        if (createActualRegion(p, l, blockOptions)) { // region creation successful

            // take money
            if (OdisseyProtecciones.getInstance().isVaultSupportEnabled() && blockOptions.costToPlace != 0) {
                EconomyResponse er = OdisseyProtecciones.getInstance().getVaultEconomy().withdrawPlayer(p, blockOptions.costToPlace);
                if (!er.transactionSuccess()) {
                    PSL.msg(p, er.errorMessage);
                    // createActualRegion ya creo la region, su holograma y su
                    // respaldo. Si el cobro falla, revertirlos y cancelar la
                    // colocacion para que no quede una proteccion gratis.
                    RegionManager manager = WGUtils.getRegionManagerWithPlayer(p);
                    String regionId = WGUtils.createPSID(l.getX(), l.getY(), l.getZ());
                    ProtectedRegion failedRegion = manager.getRegion(regionId);
                    PSRegion psFailedRegion = failedRegion == null ? null
                            : PSRegion.fromWGRegion(p.getWorld(), failedRegion);
                    manager.removeRegion(regionId);
                    if (psFailedRegion != null) HologramManager.getInstance().remove(psFailedRegion);
                    es.odissey.protecciones.storage.Database db =
                            OdisseyProtecciones.getInstance().getDatabase();
                    if (db != null && db.isEnabled()) {
                        db.deleteRegionAsync(p.getWorld().getName(), regionId);
                    }
                    return false;
                }
                PSL.msg(p, PSL.PAID_MONEY.msg().replace("%price%", String.format("%.2f", blockOptions.costToPlace)));
            }

            return true;
        } else { // region creation failed
            return false;
        }
    }

    // create the actual WG region for PS region
    public static boolean createActualRegion(Player p, Location l, PSProtectBlock blockOptions) {
        // create region
        double bx = l.getX(), by = l.getY(), bz = l.getZ();

        RegionManager rm = WGUtils.getRegionManagerWithPlayer(p);
        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

        String id = WGUtils.createPSID(bx, by, bz);

        // if the region's id already exists, possibly placing block where a region is hidden
        if (rm.hasRegion(id)) {
            PSL.msg(p, PSL.REGION_ALREADY_IN_LOCATION_IS_HIDDEN.msg());
            return false;
        }

        // check for minimum distance between claims by using fake region
        if (blockOptions.distanceBetweenClaims != -1 && !p.hasPermission("odisseyprotecciones.superowner")) {
            if (!isFarEnoughFromOtherClaims(blockOptions, p.getWorld(), lp, bx, by, bz)) {
                PSL.msg(p, PSL.REGION_TOO_CLOSE.msg().replace("%num%", "" + blockOptions.distanceBetweenClaims));
                return false;
            }
        }

        // create actual region
        ProtectedRegion region = WGUtils.getDefaultProtectedRegion(blockOptions, WGUtils.parsePSRegionToLocation(id));
        region.getOwners().addPlayer(p.getUniqueId());
        region.setPriority(blockOptions.priority);
        rm.addRegion(region); // added to the region manager, be careful in implementing checks

        // check if new region overlaps more powerful region
        if (!blockOptions.allowOverlapUnownedRegions && !p.hasPermission("odisseyprotecciones.superowner") && WGUtils.overlapsStrongerRegion(p.getWorld(), region, lp)) {
            rm.removeRegion(id);
            PSL.msg(p, PSL.REGION_OVERLAP.msg());
            return false;
        }

        // add corresponding flags to new region by cloning blockOptions default flags
        HashMap<Flag<?>, Object> flags = new HashMap<>(blockOptions.regionFlags);

        // replace greeting and farewell messages with player name
        FlagHandler.initDefaultFlagPlaceholders(flags, p);

        // set flags
        try {
            region.setFlags(flags);
        } catch (Exception e) {
            OdisseyProtecciones.getPluginLogger().severe(String.format("No se pudieron inicializar las flags de la region para: %s (%s)", blockOptions.alias, blockOptions.type));
            throw e;
        }
        FlagHandler.initCustomFlagsForPS(region, l, blockOptions);

        // check for player's number of adjacent region groups
        if (OdisseyProtecciones.getInstance().getConfigOptions().regionsMustBeAdjacent) {
            if (MiscUtil.getPermissionNumber(p, "odisseyprotecciones.adjacent.", 1) >= 0 && !p.hasPermission("odisseyprotecciones.admin")) {
                HashMap<String, ArrayList<String>> adjGroups = WGUtils.getPlayerAdjacentRegionGroups(p, rm);

                int permNum = MiscUtil.getPermissionNumber(p, "odisseyprotecciones.adjacent.", 1);
                if (adjGroups.size() > permNum && permNum != -1) {
                    PSL.msg(p, PSL.REGION_NOT_ADJACENT.msg());
                    rm.removeRegion(id);
                    return false;
                }
            }
        }

        // fire event and check if cancelled
        PSCreateEvent event = new PSCreateEvent(PSRegion.fromWGRegion(p.getWorld(), region), p);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            rm.removeRegion(id);
            return false;
        }

        PSL.msg(p, PSL.PROTECTED.msg());

        // create the protection hologram
        try {
            HologramManager.getInstance().spawn(PSRegion.fromWGRegion(p.getWorld(), region));
        } catch (Exception ignored) {
        }

        // guardar respaldo en la base de datos
        try {
            es.odissey.protecciones.storage.Database db = OdisseyProtecciones.getInstance().getDatabase();
            if (db != null && db.isEnabled()) {
                db.saveRegionAsync(es.odissey.protecciones.storage.RegionSnapshot.from(p.getWorld(), region));
                db.audit(p.getUniqueId().toString(), p.getName(), "create",
                        p.getWorld().getName(), id, blockOptions.alias);
            }
        } catch (Exception ignored) {
        }

        // dibuja el borde de la proteccion con particulas durante 5 segundos
        try {
            es.odissey.protecciones.utils.ParticlesUtil.showRegionBorder(PSRegion.fromWGRegion(p.getWorld(), region), 100);
        } catch (Exception ignored) {
        }

        // hide block if auto hide is enabled
        if (blockOptions.autoHide) {
            PSL.msg(p, PSL.REGION_HIDDEN.msg());
            // run on next tick so placing tile entities don't complain
            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> l.getBlock().setType(Material.AIR));
        }

        if (blockOptions.startWithTaxAutopay) {
            // set tax auto-pay (even if taxing is not enabled)
            region.setFlag(FlagHandler.PS_TAX_AUTOPAYER, p.getUniqueId().toString());
        }

        // show merge menu
        if (OdisseyProtecciones.getInstance().getConfigOptions().allowMergingRegions && blockOptions.allowMerging && p.hasPermission("odisseyprotecciones.merge")) {
            PSRegion r = PSRegion.fromWGRegion(p.getWorld(), region);
            if (r != null) playerMergeTask(p, r);
        }

        return true;
    }

    // merge behaviour after a region is created
    private static void playerMergeTask(Player p, PSRegion r) {
        boolean showGUI = true;

        // auto merge to nearest region if only one exists
        if (r.getTypeOptions().autoMerge) {
            PSRegion mergeTo = null;
            for (PSRegion psr : r.getMergeableRegions(p)) {
                if (mergeTo == null) {
                    mergeTo = psr;
                    showGUI = false;
                } else {
                    showGUI = true;
                    break;
                }
            }

            // actually do auto merge
            if (!showGUI) {
                PSRegion finalMergeTo = mergeTo;
                Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
                    try {
                        WGMerge.mergeRealRegions(p.getWorld(), r.getWGRegionManager(), finalMergeTo, Arrays.asList(finalMergeTo, r));
                        PSL.msg(p, PSL.MERGE_AUTO_MERGED.msg().replace("%region%", finalMergeTo.getId()));
                    } catch (WGMerge.RegionHoleException e) {
                        PSL.msg(p, PSL.NO_REGION_HOLES.msg()); // TODO github issue #120, prevent holes even if showGUI is true
                    } catch (WGMerge.RegionCannotMergeWhileRentedException e) {
                        // don't need to tell player that you can't merge
                    }
                });
            }
        }

        // show merge gui
        if (showGUI) {
            List<TextComponent> tc = ArgMerge.getGUI(p, r);
            if (!tc.isEmpty()) { // if there are regions you can merge into
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
                PSL.msg(p, PSL.MERGE_INTO.msg());
                PSL.msg(p, PSL.MERGE_HEADER.msg().replace("%region%", r.getId()));
                for (TextComponent t : tc) p.spigot().sendMessage(t);
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
            }
        }
    }
}
