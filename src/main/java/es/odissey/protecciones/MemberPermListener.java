package es.odissey.protecciones;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Aplica los permisos por jugador (ver {@link MemberPermission}).
 * <p>
 * Solo restringe a los <b>miembros</b> de una proteccion: los duenos y los administradores
 * (permiso {@code odisseyprotecciones.admin}) siempre pasan. Los jugadores que no son
 * miembros quedan en manos de WorldGuard (aqui no se toca nada).
 * <p>
 * Nota: este listener solo puede <b>restringir</b>, nunca conceder permisos que WorldGuard
 * ya haya denegado (se ejecuta como {@code ignoreCancelled = true}).
 */
public class MemberPermListener implements Listener {

    private static final Set<Material> CONTAINERS = EnumSet.noneOf(Material.class);
    private static final Set<Material> INTERACTS = EnumSet.noneOf(Material.class);
    private static final Set<Material> PRESSURES = EnumSet.noneOf(Material.class);

    static {
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("SHULKER_BOX")
                    || n.equals("CHEST") || n.equals("TRAPPED_CHEST") || n.equals("BARREL")
                    || n.equals("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER")
                    || n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")
                    || n.equals("BREWING_STAND") || n.equals("LECTERN") || n.equals("ENDER_CHEST")
                    || n.equals("BEACON") || n.equals("CHISELED_BOOKSHELF") || n.equals("DECORATED_POT")) {
                CONTAINERS.add(m);
            }
            if (n.endsWith("_BED")
                    || n.endsWith("_DOOR") || n.endsWith("_TRAPDOOR") || n.endsWith("_FENCE_GATE")
                    || n.endsWith("_BUTTON") || n.equals("LEVER") || n.equals("TRIPWIRE")
                    || n.equals("REPEATER") || n.equals("COMPARATOR") || n.equals("NOTE_BLOCK")
                    || n.equals("DAYLIGHT_DETECTOR") || n.equals("ANVIL") || n.equals("CHIPPED_ANVIL")
                    || n.equals("DAMAGED_ANVIL") || n.equals("ENCHANTING_TABLE") || n.equals("GRINDSTONE")
                    || n.equals("STONECUTTER") || n.equals("CARTOGRAPHY_TABLE") || n.equals("LOOM")
                    || n.equals("SMITHING_TABLE") || n.equals("BELL") || n.equals("RESPAWN_ANCHOR")
                    || n.equals("CAKE") || n.equals("COMPOSTER") || n.equals("FLOWER_POT")) {
                INTERACTS.add(m);
            }
            if (n.endsWith("_PRESSURE_PLATE") || n.equals("LIGHT_WEIGHTED_PRESSURE_PLATE") || n.equals("HEAVY_WEIGHTED_PRESSURE_PLATE")) {
                PRESSURES.add(m);
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~ helpers ~~~~~~~~~~~~~~~~~

    private static MemberPermission classify(Material m) {
        if (m == null) return null;
        if (CONTAINERS.contains(m)) return MemberPermission.CONTAINERS;
        if (INTERACTS.contains(m)) return MemberPermission.INTERACT;
        if (PRESSURES.contains(m)) return MemberPermission.PRESSURE;
        return null;
    }

    /**
     * @return true si la accion del jugador debe ser cancelada (no tiene el permiso pedido)
     */
    private static boolean deny(Player p, Location loc, MemberPermission perm) {
        if (p.hasPermission("odisseyprotecciones.admin")) return false;
        PSRegion r = PSRegion.fromLocationGroup(loc);
        if (r == null) return false;
        UUID id = p.getUniqueId();
        if (r.isOwner(id)) return false;
        if (!r.isMember(id)) return false; // los no-miembros los gestiona WorldGuard
        if (r.hasMemberPermission(id, perm)) return false;
        PSL.msg(p, PSL.MEMBER_PERM_DENIED.msg());
        return true;
    }

    // ~~~~~~~~~~~~~~~~~ eventos ~~~~~~~~~~~~~~~~~

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), MemberPermission.BUILD)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), MemberPermission.BUILD)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Action action = e.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            MemberPermission perm = classify(e.getClickedBlock().getType());
            if (perm == null) {
                // no es un bloque interactivo conocido: si esta colocando un bloque lo maneja BlockPlaceEvent
                if (e.getItem() != null) {
                    if (e.getItem().getType().isBlock()) return;
                    // Comida, pociones u otros consumibles de uso personal no deben ser bloqueados
                    if (e.getItem().getType().isEdible() || e.getItem().getType() == Material.POTION
                            || e.getItem().getType() == Material.SPLASH_POTION || e.getItem().getType() == Material.LINGERING_POTION
                            || e.getItem().getType() == Material.MILK_BUCKET || e.getItem().getType() == Material.BOW
                            || e.getItem().getType() == Material.CROSSBOW || e.getItem().getType() == Material.TRIDENT) {
                        return;
                    }
                }
                perm = MemberPermission.ITEMS;
            }
            if (deny(e.getPlayer(), e.getClickedBlock().getLocation(), perm)) {
                e.setCancelled(true);
            }
        } else if (action == Action.PHYSICAL) {
            if (classify(e.getClickedBlock().getType()) == MemberPermission.PRESSURE
                    && deny(e.getPlayer(), e.getClickedBlock().getLocation(), MemberPermission.PRESSURE)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        MemberPermission perm = null;
        if (e.getRightClicked() instanceof Boat || e.getRightClicked() instanceof Minecart) {
            perm = MemberPermission.VEHICLES;
        } else if (e.getRightClicked() instanceof Animals || e.getRightClicked() instanceof Villager) {
            perm = MemberPermission.ANIMALS;
        }
        if (perm != null && deny(e.getPlayer(), e.getRightClicked().getLocation(), perm)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player p = null;
        if (e.getDamager() instanceof Player) {
            p = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            p = shooter;
        }
        if (p == null) return;

        MemberPermission perm = null;
        if (e.getEntity() instanceof Player) perm = MemberPermission.PVP;
        else if (e.getEntity() instanceof Boat || e.getEntity() instanceof Minecart) perm = MemberPermission.VEHICLES;
        else if (e.getEntity() instanceof Animals || e.getEntity() instanceof Villager) perm = MemberPermission.ANIMALS;

        if (perm != null && deny(p, e.getEntity().getLocation(), perm)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (e.getAttacker() instanceof Player) {
            Player p = (Player) e.getAttacker();
            if (deny(p, e.getVehicle().getLocation(), MemberPermission.VEHICLES)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), MemberPermission.ITEMS)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (deny(e.getPlayer(), e.getBlock().getLocation(), MemberPermission.ITEMS)) {
            e.setCancelled(true);
        }
    }
}
