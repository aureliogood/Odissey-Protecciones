package es.odissey.protecciones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.event.PSBreakProtectBlockEvent;
import es.odissey.protecciones.event.PSCreateEvent;
import es.odissey.protecciones.event.PSRemoveEvent;
import es.odissey.protecciones.utils.RecipeUtil;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListenerClass implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // update UUID cache
        UUIDCache.removeUUID(p.getUniqueId());
        UUIDCache.removeName(p.getName());
        UUIDCache.storeUUIDNamePair(p.getUniqueId(), p.getName());

        // allow worldguard to resolve all UUIDs to names
        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> UUIDCache.storeWGProfile(p.getUniqueId(), p.getName()));

        // add recipes to player's recipe book
        p.discoverRecipes(RecipeUtil.getRecipeKeys());

        PSPlayer psp = PSPlayer.fromPlayer(p);

        // if by default, players should have protection block placement toggled off
        if (OdisseyProtecciones.getInstance().getConfigOptions().defaultProtectionBlockPlacementOff) {
            OdisseyProtecciones.toggleList.add(p.getUniqueId());
        }

        // tax join message
        if (OdisseyProtecciones.getInstance().getConfigOptions().taxEnabled && OdisseyProtecciones.getInstance().getConfigOptions().taxMessageOnJoin) {
            Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
                int amount = 0;
                for (PSRegion psr : psp.getTaxEligibleRegions()) {
                    for (PSRegion.TaxPayment tp : psr.getTaxPaymentsDue()) {
                        amount += tp.getAmount();
                    }
                }

                if (amount != 0) {
                    PSL.msg(psp, PSL.TAX_JOIN_MSG_PENDING_PAYMENTS.msg().replace("%money%", "" + amount));
                }
            });
        }
    }

    // specifically add WG passthrough bypass here, so other plugins can see the result
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlaceLowPriority(PlaceBlockEvent event) {
        var cause = event.getCause().getRootCause();

        if (cause instanceof Player player && event.getBlocks().size() >= 1) {
            var block = event.getBlocks().get(0);
            if (!OdisseyProtecciones.isProtectBlockItem(player.getInventory().getItemInHand())) {
                return;
            }

            var options = OdisseyProtecciones.getBlockOptions(player.getInventory().getItemInHand());

            if (options != null && options.placingBypassesWGPassthrough) {
                // check if any regions here have the passthrough flag
                // we can't query unfortunately, since null flags seem to equate to ALLOW, when we want it to be DENY
                var rm = WGUtils.getRegionManagerWithWorld(event.getWorld());
                if (rm == null) return;
                ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()));

                // loop through regions, if any region does not have a passthrough value set, then don't allow
                for (var region : set.getRegions()) {
                    if (region.getFlag(Flags.PASSTHROUGH) == null) {
                        return;
                    }
                }

                // if every region with an explicit passthrough value, then allow passthrough of protection block
                event.setResult(Event.Result.ALLOW);
            }
        }
    }

    // we only create the region after other plugins' event handlers have run
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        BlockHandler.createPSRegion(e);
    }

    // returns the error message, or "" if the player has permission to break the region
    // TODO: refactor and move this to PSRegion, so that /ps unclaim can use the same checks
    private String checkPermissionToBreakProtection(Player p, PSRegion r) {
        // check for destroy permission
        if (!p.hasPermission("odisseyprotecciones.destroy")) {
            return PSL.NO_PERMISSION_DESTROY.msg();
        }

        // check if player is owner of region
        if (!r.isOwner(p.getUniqueId()) && !p.hasPermission("odisseyprotecciones.superowner")) {
            return PSL.NO_REGION_PERMISSION.msg();
        }

        // cannot break region being rented (prevents splitting merged regions, and breaking as tenant owner)
        if (r.getRentStage() == PSRegion.RentStage.RENTING && !p.hasPermission("odisseyprotecciones.superowner")) {
            return PSL.RENT_CANNOT_BREAK_WHILE_RENTING.msg();
        }

        return "";
    }

    // helper method for breaking protection blocks
    // IMPLEMENTATION NOTES: r may be of a non-configured type
    private boolean playerBreakProtection(Player p, PSRegion r) {
        PSProtectBlock blockOptions = r.getTypeOptions();

        // check if player has permission to break the protection
        String error = checkPermissionToBreakProtection(p, r);
        if (!error.isEmpty()) {
            PSL.msg(p, error);
            return false;
        }

        // Call PSBreakEvent
        PSBreakProtectBlockEvent event = new PSBreakProtectBlockEvent(r , p);
        Bukkit.getPluginManager().callEvent(event);
        // don't give ps block to player if the event is cancelled
        if (event.isCancelled()) return false;

        // prepara la entrega y comprueba el espacio ANTES de borrar la region:
        // si la eliminacion falla no debe quedar item + proteccion duplicados
        List<ItemStack> drops = new ArrayList<>();
        if (blockOptions != null && !blockOptions.noDrop) {
            drops.add(blockOptions.createItem());
        }
        if (!drops.isEmpty() && !hasInventoryRoom(p, drops)
                && !OdisseyProtecciones.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
            PSL.msg(p, PSL.NO_ROOM_IN_INVENTORY.msg());
            return false;
        }

        // ubicacion del bloque antes de borrarlo (para soltar los items si hace falta)
        Location dropLocation = null;
        try {
            dropLocation = r.getProtectBlock().getLocation().add(0.5, 0.5, 0.5);
        } catch (Exception ignored) {
        }

        // check if removing the region and firing region remove event blocked it
        if (!r.deleteRegion(true, p)) {
            if (!OdisseyProtecciones.getInstance().getConfigOptions().allowMergingHoles) { // side case if the removing creates a hole and those are prevented
                PSL.msg(p, PSL.DELETE_REGION_PREVENTED_NO_HOLES.msg());
            }
            return false;
        }

        // el bloque se entrega solo cuando la region ya no existe
        for (ItemStack item : drops) {
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
            for (ItemStack rest : leftover.values()) {
                Location where = dropLocation != null ? dropLocation : p.getLocation();
                where.getWorld().dropItem(where, rest);
            }
        }

        PSL.msg(p, PSL.NO_LONGER_PROTECTED.msg());
        return true;
    }

    // comprueba si el inventario admite todos los items sin modificarlo
    private static boolean hasInventoryRoom(Player p, List<ItemStack> items) {
        org.bukkit.inventory.Inventory probe = Bukkit.createInventory(null, 36);
        probe.setContents(p.getInventory().getStorageContents().clone());
        for (ItemStack item : items) {
            if (!probe.addItem(item.clone()).isEmpty()) return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        // shift-right click block with hand to break
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.isBlockInHand()
                && e.getClickedBlock() != null && OdisseyProtecciones.isProtectBlock(e.getClickedBlock())) {

            PSProtectBlock ppb = OdisseyProtecciones.getBlockOptions(e.getClickedBlock());
            if (ppb.allowShiftRightBreak && e.getPlayer().isSneaking()) {
                PSRegion r = PSRegion.fromLocation(e.getClickedBlock().getLocation());
                if (r != null && playerBreakProtection(e.getPlayer(), r)) { // successful
                    e.getClickedBlock().setType(Material.AIR);
                }
            }
        }
    }

    // click derecho normal sobre el bloque de proteccion: abre el menu de gestion
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProtectBlockInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return; // evita abrirlo dos veces (mano principal + secundaria)

        Block clicked = e.getClickedBlock();
        if (clicked == null || !OdisseyProtecciones.isProtectBlock(clicked)) return;
        if (e.isBlockInHand()) return; // esta colocando un bloque encima: no interferir

        PSProtectBlock ppb = OdisseyProtecciones.getBlockOptions(clicked);
        if (ppb == null) return;

        // shift + click derecho para romper: lo gestiona onPlayerInteract
        if (e.getPlayer().isSneaking() && ppb.allowShiftRightBreak) return;

        es.odissey.protecciones.gui.MenuManager menus = es.odissey.protecciones.gui.MenuManager.getInstance();
        if (!menus.isEnabled()) return;

        // se resuelve por el id de la region del bloque (evita coger otra region superpuesta)
        PSRegion region = PSRegion.fromLocationGroup(clicked.getLocation());
        if (region == null || !menus.canAccess(e.getPlayer(), region)) return;

        e.setCancelled(true);
        menus.open(e.getPlayer(), "manage", null, region, null);
    }

    // this will be the first event handler called in the chain
    // thus we should cancel the event here if possible (so other plugins don't start acting upon it)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakLowPriority(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block pb = e.getBlock();

        if (!OdisseyProtecciones.isProtectBlock(pb)) return;

        // check if player has permission to break the protection
        PSRegion r = PSRegion.fromLocation(pb.getLocation());
        if (r != null) {
            String error = checkPermissionToBreakProtection(p, r);
            if (!error.isEmpty()) {
                PSL.msg(p, error);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block pb = e.getBlock();

        PSProtectBlock blockOptions = OdisseyProtecciones.getBlockOptions(pb);

        // comprueba si el bloque roto es de un tipo de bloque de proteccion
        if (blockOptions == null) return;

        // comprueba si es realmente un bloque de proteccion (tiene una region)
        if (!OdisseyProtecciones.isProtectBlock(pb)) {
            // impide Silk Touch sobre bloques de proteccion (que no tienen una region)
            if (blockOptions.preventSilkTouch) {
                ItemStack left = p.getInventory().getItemInMainHand();
                ItemStack right = p.getInventory().getItemInOffHand();
                if (!left.containsEnchantment(Enchantment.SILK_TOUCH) && !right.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    return;
                }
                e.setDropItems(false);
            }
            return;
        }

        PSRegion r = PSRegion.fromLocation(pb.getLocation());

        // break protection
        if (r != null && playerBreakProtection(p, r)) { // successful
            e.setDropItems(false);
            e.setExpToDrop(0);
        } else { // unsuccessful
            e.setCancelled(true);
        }
    }

    // -=-=-=- prevent smelting protection blocks -=-=-=-

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        // prevent protect block item to be smelt
        PSProtectBlock options = OdisseyProtecciones.getBlockOptions(e.getSource());
        if (options != null && !options.allowSmeltItem) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceBurnItem(FurnaceBurnEvent e) {
        // prevent protect block item to be smelt
        Furnace f = (Furnace) e.getBlock().getState();
        if (f.getInventory().getSmelting() != null) {
            PSProtectBlock options = OdisseyProtecciones.getBlockOptions(f.getInventory().getSmelting());
            PSProtectBlock fuelOptions = OdisseyProtecciones.getBlockOptions(f.getInventory().getFuel());
            if ((options != null && !options.allowSmeltItem) || (fuelOptions != null && !fuelOptions.allowSmeltItem)) {
                e.setCancelled(true);
            }
        }
    }

    // -=-=-=- prevent crafting using protection blocks -=-=-=-

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            PSProtectBlock options = OdisseyProtecciones.getBlockOptions(s);
            if (options != null && !options.allowUseInCrafting) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getType() != Material.CRAFTER) return;
        if (!(e.getBlock().getState() instanceof Container container)) return;
        for (ItemStack item : container.getInventory().getContents()) {
            if (item == null) continue;
            PSProtectBlock options = OdisseyProtecciones.getBlockOptions(item);
            if (options != null && !options.allowUseInCrafting) {
                e.setCancelled(true);
                e.setResult(new ItemStack(Material.AIR));
                return;
            }
        }
    }

    // -=-=-=- disable grindstone inventory to prevent infinite exp exploit with enchanted_effect option  -=-=-=-
    // see https://github.com/espidev/OdisseyProtecciones/issues/324

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClickEvent(InventoryClickEvent e) {
        if (e.getInventory().getType() == InventoryType.GRINDSTONE) {
            if (OdisseyProtecciones.isProtectBlockItem(e.getCurrentItem())) {
                e.setCancelled(true);
            }
        }
    }


    // -=-=-=- block changes to protection block related events -=-=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketEmptyEvent e) {
        Block clicked = e.getBlockClicked();
        BlockFace bf = e.getBlockFace();
        Block check = clicked.getWorld().getBlockAt(clicked.getX() + e.getBlockFace().getModX(), clicked.getY() + bf.getModY(), clicked.getZ() + e.getBlockFace().getModZ());
        if (OdisseyProtecciones.isProtectBlock(check)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (OdisseyProtecciones.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (OdisseyProtecciones.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (OdisseyProtecciones.isProtectBlock(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (OdisseyProtecciones.isProtectBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (OdisseyProtecciones.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent e) {
        if (OdisseyProtecciones.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent e) {
        // unfortunately, the below fix does not really work because Spigot only triggers for the source block, despite
        // what the documentation says: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/block/BlockDropItemEvent.html

        // we want to replace protection blocks that have their protection block broken (ex. signs, banners)
        // the block may not exist anymore, and so we have to recreate the isProtectBlock method here
        BlockState bs = e.getBlockState();
        if (!OdisseyProtecciones.isProtectBlockType(bs.getType().toString())) return;

        RegionManager rgm = WGUtils.getRegionManagerWithWorld(bs.getWorld());
        if (rgm == null) return;

        // check if the block is a source block
        ProtectedRegion br = rgm.getRegion(WGUtils.createPSID(bs.getLocation()));
        if (!OdisseyProtecciones.isPSRegion(br) && PSMergedRegion.getMergedRegion(bs.getLocation()) == null) return;

        PSRegion r = PSRegion.fromLocation(bs.getLocation());
        if (r == null) return;

        // puts the block back
        r.unhide();
        e.setCancelled(true);
    }

    // -=-=- prevent protection block piston effects -=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    private void pistonUtil(List<Block> pushedBlocks, BlockPistonEvent e) {
        for (Block b : pushedBlocks) {
            PSProtectBlock cpb = OdisseyProtecciones.getBlockOptions(b);
            if (cpb != null && OdisseyProtecciones.isProtectBlock(b) && cpb.preventPistonPush) {
                e.setCancelled(true);
            }
        }
    }

    // -=-=- prevent protection blocks from exploding -=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        boolean isWindCharge = e.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK;
        this.explodeUtil(e.blockList(), e.getBlock().getLocation().getWorld(), isWindCharge);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        boolean isWindCharge = e.getEntity() instanceof AbstractWindCharge || e.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK;
        explodeUtil(e.blockList(), e.getLocation().getWorld(), isWindCharge);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!OdisseyProtecciones.isProtectBlock(e.getBlock())) return;

        // events like ender dragon block break, wither running into block break, etc.
        if (!blockExplodeUtil(e.getBlock().getWorld(), e.getBlock(), false)) {
            // if block shouldn't be exploded, cancel the event
            e.setCancelled(true);
        }
    }

    private void explodeUtil(List<Block> blockList, World w, boolean isWindCharge) {
        // loop through exploded blocks
        for (int i = 0; i < blockList.size(); i++) {
            Block b = blockList.get(i);

            if (OdisseyProtecciones.isProtectBlock(b)) {
                // always remove protection block from exploded list
                blockList.remove(i);
                i--;
            }

            blockExplodeUtil(w, b, isWindCharge);
        }
    }

    // returns whether the block is exploded
    private boolean blockExplodeUtil(World w, Block b, boolean isWindCharge) {
        if (OdisseyProtecciones.isProtectBlock(b)) {
            String id = WGUtils.createPSID(b.getLocation());
            PSProtectBlock blockOptions = OdisseyProtecciones.getBlockOptions(b);

            // if prevent explode
            if (blockOptions.preventExplode) {
                return false;
            }

            if (isWindCharge && blockOptions.preventWindChargeExplode) {
                return false;
            }

            // si la region no se destruye al explotar, el bloque tampoco puede desaparecer:
            // dejaria una proteccion fantasma sin bloque y duplicaria el item
            if (!blockOptions.destroyRegionWhenExplode) {
                return false;
            }

            // manually set to air if exploded so there is no natural item drop
            b.setType(Material.AIR);

            // manually add drop
            if (!blockOptions.noDrop) {
                b.getWorld().dropItem(b.getLocation(), blockOptions.createItem());
            }
            // remove region from worldguard
            OdisseyProtecciones.removePSRegion(w, id);
        }
        return true;
    }

    // check player teleporting into region behaviour
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Some plugins can cancel a teleport by clearing its destination.
        if (event.getTo() == null) return;
        // we only want plugin triggered teleports, ignore natural teleportation
        if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT) return;

        if (event.getPlayer().hasPermission("odisseyprotecciones.tp.bypassprevent")) return;

        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(event.getTo().getWorld());
        BlockVector3 v = BlockVector3.at(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

        if (rgm != null) {
            // check if player can teleport into region (no region with preventTeleportIn = true)
            ApplicableRegionSet regions = rgm.getApplicableRegions(v);
            if (regions.getRegions().isEmpty()) return;
            boolean foundNoTeleport = false;
            for (ProtectedRegion r : regions) {
                String f = r.getFlag(FlagHandler.PS_BLOCK_MATERIAL);
                if (f != null && OdisseyProtecciones.getBlockOptions(f) != null && OdisseyProtecciones.getBlockOptions(f).preventTeleportIn)
                    foundNoTeleport = true;
                if (r.getOwners().contains(wg.wrapPlayer(event.getPlayer()))) return;
            }

            if (foundNoTeleport) {
                PSL.msg(event.getPlayer(), PSL.REGION_CANT_TELEPORT.msg());
                event.setCancelled(true);
            }
        }
    }

    // -=-=-=- player defined events -=-=-=-

    private void execEvent(String action, CommandSender s, String player, PSRegion region) {
        if (player == null) player = "";

        // split action_type: action
        String[] sp = action.split(": ");
        if (sp.length < 2) return;

        StringBuilder act = new StringBuilder(sp[1]);
        for (int i = 2; i < sp.length; i++) act.append(": ").append(sp[i]); // add anything extra that has a colon

        act = new StringBuilder(act.toString()
                .replace("%player%", player)
                .replace("%world%", region.getWorld().getName())
                .replace("%region%", region.getName() == null ? region.getId() : region.getName() + " (" + region.getId() + ")")
                .replace("%block_x%", region.getProtectBlock().getX() + "")
                .replace("%block_y%", region.getProtectBlock().getY() + "")
                .replace("%block_z%", region.getProtectBlock().getZ() + "")
                .replace("%radius%", radiusOf(region)));

        switch (sp[0]) {
            case "player_command":
                if (s != null) Bukkit.getServer().dispatchCommand(s, act.toString());
                break;
            case "console_command":
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), act.toString());
                break;
            case "message":
                if (s != null) s.sendMessage(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
            case "global_message":
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', act.toString()));
                }
                OdisseyProtecciones.getPluginLogger().info(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
            case "console_message":
                OdisseyProtecciones.getPluginLogger().info(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
        }
    }

    // radio legible para los comandos de evento (%radius%)
    private static String radiusOf(PSRegion region) {
        PSProtectBlock options = region == null ? null : region.getTypeOptions();
        if (options == null) return "0";
        int x = Math.max(0, options.xRadius);
        int z = Math.max(0, options.zRadius);
        return x == z ? String.valueOf(x) : (x + "x" + z);
    }

    @EventHandler
    public void onPSCreate(PSCreateEvent event) {
        if (event.isCancelled()) return;
        if (!event.getRegion().getTypeOptions().eventsEnabled) return;

        // run on next tick (after the region is created to allow for edits to the region)
        Bukkit.getServer().getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> {
            // run custom commands (in config)
            for (String action : event.getRegion().getTypeOptions().regionCreateCommands) {
                execEvent(action, event.getPlayer(), event.getPlayer().getName(), event.getRegion());
            }
        });
    }

    @EventHandler
    public void onPSRemove(PSRemoveEvent event) {
        if (event.isCancelled()) return;
        if (event.getRegion().getTypeOptions() == null) return;
        if (!event.getRegion().getTypeOptions().eventsEnabled) return;

        // run custom commands (in config)
        for (String action : event.getRegion().getTypeOptions().regionDestroyCommands) {
            if (event.getPlayer() == null) {
                execEvent(action, null, null, event.getRegion());
            } else {
                execEvent(action, event.getPlayer(), event.getPlayer().getName(), event.getRegion());
            }
        }
    }

}
