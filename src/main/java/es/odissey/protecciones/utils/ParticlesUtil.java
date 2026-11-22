package es.odissey.protecciones.utils;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.PSRegion;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ParticlesUtil {

    public static void persistRedstoneParticle(Player p, Location l, Particle.DustOptions d, int occ) {
        if (occ <= 0) return;
        // Una unica tarea repetitiva en vez de "occ" tareas independientes (evita miles de
        // entradas en el planificador cuando se dibujan varios puntos de una region)
        new BukkitRunnable() {
            int run = 0;

            @Override
            public void run() {
                if (run++ >= occ || !p.isOnline()) {
                    cancel();
                    return;
                }

                // Stronger "glow marker" burst
                p.spawnParticle(Particle.DUST, l,
                        2,            // count (was 1)
                        0.10, 0.15, 0.10, // offset/spread (x,y,z)
                        0.0,
                        d
                );

                p.spawnParticle(Particle.GLOW, l,
                        2,
                        0.05, 0.08, 0.05,
                        0.0
                );
            }
        }.runTaskTimer(OdisseyProtecciones.getInstance(), 0L, 20L);
    }

    /** altura (en bloques) que se dibuja hacia arriba y hacia abajo desde el bloque de proteccion */
    private static final int BORDER_HALF_HEIGHT = 30;

    private static final Particle.DustOptions BORDER_DUST = new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.0f);

    /** Dibuja el borde de la region a todos los jugadores cercanos durante {@code ticks} ticks. */
    public static void showRegionBorder(PSRegion region, int ticks) {
        List<Location> points = borderPoints(region);
        if (points.isEmpty()) return;
        World w = region.getWorld();

        new BukkitRunnable() {
            int run = 0;

            @Override
            public void run() {
                if (run++ >= Math.max(1, ticks / 5)) {
                    cancel();
                    return;
                }
                for (Location l : points) {
                    w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, BORDER_DUST);
                }
            }
        }.runTaskTimer(OdisseyProtecciones.getInstance(), 0L, 5L);
    }

    /** Dibuja el borde de la region solo para un jugador concreto durante {@code ticks} ticks. */
    public static void showRegionBorderFor(Player p, PSRegion region, int ticks) {
        List<Location> points = borderPoints(region);
        if (points.isEmpty()) return;

        new BukkitRunnable() {
            int run = 0;

            @Override
            public void run() {
                if (run++ >= Math.max(1, ticks / 5)) {
                    cancel();
                    return;
                }
                if (!p.isOnline()) {
                    cancel();
                    return;
                }
                for (Location l : points) {
                    p.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, BORDER_DUST);
                }
            }
        }.runTaskTimer(OdisseyProtecciones.getInstance(), 0L, 5L);
    }

    private static List<Location> borderPoints(PSRegion region) {
        List<Location> points = new ArrayList<>();
        if (region == null) return points;
        World w = region.getWorld();
        ProtectedRegion wr = region.getWGRegion();
        if (w == null || wr == null) return points;

        BlockVector3 min = wr.getMinimumPoint();
        BlockVector3 max = wr.getMaximumPoint();
        int centerY = region.getProtectBlock().getLocation().getBlockY();

        int minY = Math.max(min.getY(), centerY - BORDER_HALF_HEIGHT);
        int maxY = Math.min(max.getY(), centerY + BORDER_HALF_HEIGHT);

        int minX = min.getX(), maxX = max.getX();
        int minZ = min.getZ(), maxZ = max.getZ();

        // cuadrados inferior y superior
        addEdge(points, w, minX, minY, minZ, maxX, minY, minZ);
        addEdge(points, w, maxX, minY, minZ, maxX, minY, maxZ);
        addEdge(points, w, maxX, minY, maxZ, minX, minY, maxZ);
        addEdge(points, w, minX, minY, maxZ, minX, minY, minZ);

        addEdge(points, w, minX, maxY, minZ, maxX, maxY, minZ);
        addEdge(points, w, maxX, maxY, minZ, maxX, maxY, maxZ);
        addEdge(points, w, maxX, maxY, maxZ, minX, maxY, maxZ);
        addEdge(points, w, minX, maxY, maxZ, minX, maxY, minZ);

        // esquinas verticales
        addEdge(points, w, minX, minY, minZ, minX, maxY, minZ);
        addEdge(points, w, maxX, minY, minZ, maxX, maxY, minZ);
        addEdge(points, w, maxX, minY, maxZ, maxX, maxY, maxZ);
        addEdge(points, w, minX, minY, maxZ, minX, maxY, maxZ);

        return points;
    }

    private static void addEdge(List<Location> out, World w, int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = Integer.signum(x2 - x1);
        int dy = Integer.signum(y2 - y1);
        int dz = Integer.signum(z2 - z1);
        int len = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        int step = Math.max(1, len / 64); // limita la cantidad de particulas por arista
        for (int i = 0; i <= len; i += step) {
            out.add(new Location(w, x1 + dx * i + 0.5, y1 + dy * i, z1 + dz * i + 0.5));
        }
    }
}
