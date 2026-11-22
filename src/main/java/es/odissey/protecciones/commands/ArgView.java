package es.odissey.protecciones.commands;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.*;
import es.odissey.protecciones.utils.ParticlesUtil;
import es.odissey.protecciones.utils.RegionTraverse;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ArgView implements PSCommandArg {

    private static List<UUID> cooldown = new ArrayList<>();

    @Override
    public List<String> getNames() {
        return Collections.singletonList("view");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.view");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;

        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());

        if (!p.hasPermission("odisseyprotecciones.view")) {
            PSL.msg(p, PSL.NO_PERMISSION_VIEW.msg());
            return true;
        }
        if (r == null) {
            PSL.msg(p, PSL.NOT_IN_REGION.msg());
            return true;
        }
        if (!p.hasPermission("odisseyprotecciones.view.others") && WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), true)) {
            PSL.msg(p, PSL.NO_ACCESS.msg());
            return true;
        }
        if (cooldown.contains(p.getUniqueId())) {
            PSL.msg(p, PSL.VIEW_COOLDOWN.msg());
            return true;
        }

        PSL.msg(p, PSL.VIEW_GENERATING.msg());

        // capture player references on the main thread before going async
        final World world = p.getWorld();
        final Location playerLocation = p.getLocation();

        // add player to cooldown
        cooldown.add(p.getUniqueId());
        Bukkit.getScheduler().runTaskLater(OdisseyProtecciones.getInstance(), () -> cooldown.remove(p.getUniqueId()), 20 * OdisseyProtecciones.getInstance().getConfigOptions().psViewCooldown);

        // captura en el hilo principal todos los datos de WorldGuard que necesitamos
        final int playerY = playerLocation.getBlockY();
        final int minY = r.getWGRegion().getMinimumPoint().getBlockY();
        final int maxY = r.getWGRegion().getMaximumPoint().getBlockY();
        final java.util.HashSet<com.sk89q.worldedit.math.BlockVector2> edgePoints =
                new java.util.HashSet<>(r.getWGRegion().getPoints());
        final List<ProtectedRegion> wgRegions = new ArrayList<>();
        wgRegions.add(r.getWGRegion());

        final List<int[]> blocks = new ArrayList<>();
        if (r instanceof PSGroupRegion) {
            for (PSMergedRegion psmr : ((PSGroupRegion) r).getMergedRegions()) {
                PSLocation psl = WGUtils.parsePSRegionToLocation(psmr.getId());
                if (psl != null) blocks.add(new int[]{psl.x, psl.y, psl.z});
            }
        } else {
            PSLocation psl = WGUtils.parsePSRegionToLocation(r.getId());
            if (psl != null) blocks.add(new int[]{psl.x, psl.y, psl.z});
        }

        // calcula posiciones en async (solo matematicas) y envia las particulas en el hilo principal
        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
            List<Location> purple = new ArrayList<>();
            List<Location> blue = new ArrayList<>();
            List<Location> pink = new ArrayList<>();
            AtomicInteger modU = new AtomicInteger(0);

            for (int[] b : blocks) {
                purple.add(new Location(world, 0.5 + b[0], 1.5 + b[1], 0.5 + b[2]));
                for (int y = minY; y <= maxY; y += 10) {
                    purple.add(new Location(world, 0.5 + b[0], 0.5 + y, 0.5 + b[2]));
                }
            }

            RegionTraverse.traverseRegionEdge(edgePoints, wgRegions, tr -> {
                if (tr.isVertex) {
                    blue.add(new Location(world, 0.5 + tr.point.getX(), 0.5 + playerY, 0.5 + tr.point.getZ()));
                    for (int y = minY; y <= maxY; y += 5) {
                        blue.add(new Location(world, 0.5 + tr.point.getX(), 0.5 + y, 0.5 + tr.point.getZ()));
                    }
                } else {
                    if (modU.get() % 2 == 0) {
                        pink.add(new Location(world, 0.5 + tr.point.getX(), 0.5 + playerY, 0.5 + tr.point.getZ()));
                        pink.add(new Location(world, 0.5 + tr.point.getX(), 0.5 + minY, 0.5 + tr.point.getZ()));
                        pink.add(new Location(world, 0.5 + tr.point.getX(), 0.5 + maxY, 0.5 + tr.point.getZ()));
                    }
                    modU.set((modU.get() + 1) % 2);
                }
            });

            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(),
                    () -> sendParticles(p, playerLocation, purple, blue, pink));
        });
        return true;
    }

    private static void sendParticles(Player p, Location playerLocation,
                                      List<Location> purple, List<Location> blue, List<Location> pink) {
        if (!p.isOnline()) return;
        for (Location l : purple) handlePurpleParticle(p, l, playerLocation);
        for (Location l : blue) handleBlueParticle(p, l, playerLocation);
        for (Location l : pink) handlePinkParticle(p, l, playerLocation);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

    private static int PARTICLE_VIEW_DISTANCE_LIMIT = 150;

    private static boolean handlePinkParticle(Player p, Location l, Location playerLocation) {
        if (playerLocation.distance(l) > PARTICLE_VIEW_DISTANCE_LIMIT || Math.abs(l.getY()-playerLocation.getY()) > 30) return false;
        ParticlesUtil.persistRedstoneParticle(p, l, new Particle.DustOptions(Color.fromRGB(233, 30, 99), 2.0f), 30);
        return true;
    }

    private static boolean handleBlueParticle(Player p, Location l, Location playerLocation) {
        if (playerLocation.distance(l) > PARTICLE_VIEW_DISTANCE_LIMIT || Math.abs(l.getY()-playerLocation.getY()) > 30) return false;
        ParticlesUtil.persistRedstoneParticle(p, l, new Particle.DustOptions(Color.fromRGB(0, 255, 255), 2.0f), 30);
        return true;
    }

    private static boolean handlePurpleParticle(Player p, Location l, Location playerLocation) {
        if (playerLocation.distance(l) > PARTICLE_VIEW_DISTANCE_LIMIT || Math.abs(l.getY()-playerLocation.getY()) > 30) return false;
        ParticlesUtil.persistRedstoneParticle(p, l, new Particle.DustOptions(Color.fromRGB(255, 0, 255), 4.0f), 30);
        return true;
    }
}
