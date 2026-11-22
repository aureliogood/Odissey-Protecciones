package es.odissey.protecciones;

import es.odissey.protecciones.utils.ParticlesUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Muestra el borde de una proteccion con particulas cuando el jugador entra en ella.
 * Solo se envia al jugador que entra y esta limitado por tiempo para no ser molesto.
 */
public class RegionEnterListener implements Listener {

    private static final long COOLDOWN_MS = 15000;   // no repetir mas de 1 vez cada 15s por jugador
    private static final long LOOKUP_THROTTLE_MS = 500; // no consultar la region mas de 2 veces/s por jugador

    private final Map<UUID, String> lastRegion = new HashMap<>();
    private final Map<UUID, Long> lastShown = new HashMap<>();
    private final Map<UUID, Long> lastLookup = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        // solo cuando cambia de bloque
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        UUID id = e.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        Long lookup = lastLookup.get(id);
        if (lookup != null && now - lookup < LOOKUP_THROTTLE_MS) return;
        lastLookup.put(id, now);

        PSRegion r = PSRegion.fromLocationGroup(to);
        if (r == null) {
            lastRegion.remove(id);
            return;
        }

        String key = r.getWorld().getName() + " " + r.getId();
        if (key.equals(lastRegion.get(id))) return; // sigue en la misma region
        lastRegion.put(id, key);

        Long shown = lastShown.get(id);
        if (shown != null && now - shown < COOLDOWN_MS) return;
        lastShown.put(id, now);

        showBorder(e.getPlayer(), r);
    }

    private void showBorder(Player p, PSRegion r) {
        try {
            ParticlesUtil.showRegionBorderFor(p, r, 60); // 3 segundos
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastRegion.remove(id);
        lastShown.remove(id);
        lastLookup.remove(id);
    }
}
