package es.odissey.protecciones;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.gui.MenuManager;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles clicks on the clickable part of protection holograms, opening the manage menu.
 */
public class HologramListener implements Listener {

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity clicked = e.getRightClicked();
        if (!(clicked instanceof Interaction)) return;

        PersistentDataContainer pdc = clicked.getPersistentDataContainer();
        String regionId = pdc.get(HologramManager.getRegionKey(), PersistentDataType.STRING);
        if (regionId == null) return;
        String worldName = pdc.get(HologramManager.getWorldKey(), PersistentDataType.STRING);
        World w = worldName == null ? null : Bukkit.getWorld(worldName);
        if (w == null) return;

        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) return;
        ProtectedRegion pr = rgm.getRegion(regionId);
        if (pr == null) return;

        PSRegion r = PSRegion.fromWGRegion(w, pr);
        if (r == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!MenuManager.getInstance().isEnabled()) return;
        if (!MenuManager.getInstance().canAccess(p, r)) {
            es.odissey.protecciones.PSL.msg(p, "&cNo tienes acceso a esta proteccion.");
            return;
        }
        MenuManager.getInstance().open(p, "manage", null, r, null);
    }
}
