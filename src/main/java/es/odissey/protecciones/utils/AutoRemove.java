package es.odissey.protecciones.utils;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.PSConfig;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically removes protections whose owners (and optionally members) have been inactive
 * for a configurable amount of days. Configure in config.yml ([auto_remove]).
 */
public class AutoRemove {

    private static BukkitTask task;
    private static BukkitTask scanTask;

    /** numero de regiones procesadas por tick durante el escaneo de inactividad */
    private static final int SCAN_PER_TICK = 60;

    /** ultimo umbral de aviso enviado por region (7, 3 o 1 dias) */
    private static final Map<String, Integer> warnLevels = new ConcurrentHashMap<>();

    public static void start() {
        stop();
        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        boolean enabled = c.autoRemoveEnabled != null && c.autoRemoveEnabled;
        if (!enabled) return;
        int minutes = c.autoRemoveIntervalMinutes == null ? 60 : Math.max(1, c.autoRemoveIntervalMinutes);
        long period = minutes * 60L * 20L;
        task = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), AutoRemove::collect, period, period);
        OdisseyProtecciones.getPluginLogger().info("Auto-eliminacion por inactividad activada (cada " + minutes + " min).");
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    private static void collect() {
        if (scanTask != null) return; // ya hay un escaneo en curso

        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        long durationMillis = removalDurationMillis(c);
        boolean deleteWithMembers = c.autoRemoveDeleteWithMembersButNoOwners == null || c.autoRemoveDeleteWithMembersButNoOwners;

        // snapshot de regiones en el hilo principal, repartido en varios ticks para no bloquear uno solo
        List<Map.Entry<World, ProtectedRegion>> entries = new ArrayList<>();
        for (Map.Entry<World, RegionManager> entry : WGUtils.getAllRegionManagers().entrySet()) {
            World w = entry.getKey();
            for (ProtectedRegion pr : entry.getValue().getRegions().values()) {
                if (!OdisseyProtecciones.isPSRegion(pr)) continue;
                entries.add(new java.util.AbstractMap.SimpleEntry<>(w, pr));
            }
        }
        if (entries.isEmpty()) return;

        List<PSRegion> regions = new ArrayList<>();
        Map<UUID, Long> lastPlayed = new HashMap<>();
        final int[] index = {0};

        scanTask = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), () -> {
            int processed = 0;
            while (index[0] < entries.size() && processed < SCAN_PER_TICK) {
                Map.Entry<World, ProtectedRegion> e = entries.get(index[0]++);
                processed++;
                try {
                    PSRegion r = PSRegion.fromWGRegion(e.getKey(), e.getValue());
                    if (r == null) continue;
                    regions.add(r);
                    // getLastPlayed debe consultarse en el hilo principal: se reparte entre ticks
                    for (UUID uuid : r.getOwners()) lastPlayed.putIfAbsent(uuid, Bukkit.getPlayer(uuid) != null ? System.currentTimeMillis() : lastPlayedOf(uuid));
                    for (UUID uuid : r.getMembers()) lastPlayed.putIfAbsent(uuid, Bukkit.getPlayer(uuid) != null ? System.currentTimeMillis() : lastPlayedOf(uuid));
                } catch (Exception ignored) {
                }
            }
            if (index[0] >= entries.size()) {
                scanTask.cancel();
                scanTask = null;
                if (!regions.isEmpty()) analyzeAsync(regions, lastPlayed, durationMillis, deleteWithMembers);
            }
        }, 1L, 1L);
    }

    // debe llamarse en el hilo principal
    private static long lastPlayedOf(UUID uuid) {
        try {
            return Bukkit.getOfflinePlayer(uuid).getLastPlayed();
        } catch (Exception e) {
            return -1L;
        }
    }

    /** Dias hasta la autoeliminacion segun la ultima actividad de los propietarios requeridos. */
    public static String daysUntilRemoval(PSRegion region) {
        if (region == null || region.getOwners().isEmpty()) return "-";
        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        if (c == null || !Boolean.TRUE.equals(c.autoRemoveEnabled)) return "-";

        long durationMillis = removalDurationMillis(c);
        boolean deleteWithMembers = c.autoRemoveDeleteWithMembersButNoOwners == null
                || c.autoRemoveDeleteWithMembersButNoOwners;
        Set<UUID> players = new HashSet<>(region.getOwners());
        if (!deleteWithMembers) players.addAll(region.getMembers());

        long now = System.currentTimeMillis();
        long remainingMillis = 0L;
        for (UUID uuid : players) {
            long lastPlayed;
            try {
                lastPlayed = Bukkit.getPlayer(uuid) != null ? now : Bukkit.getOfflinePlayer(uuid).getLastPlayed();
            } catch (Exception ignored) {
                return "-";
            }
            if (lastPlayed <= 0) return "-";
            long elapsedMillis = Math.max(0L, now - lastPlayed);
            remainingMillis = Math.max(remainingMillis, Math.max(0L, durationMillis - elapsedMillis));
        }
        return formatDuration(remainingMillis);
    }
    private static long removalDurationMillis(PSConfig config) {
        long days = Math.max(1, config.autoRemoveDays == null ? 30 : config.autoRemoveDays);
        return days * 86400000L;
    }

    private static String formatDuration(long millis) {
        long totalMinutes = Math.max(0L, millis) / 60000L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        return days + "d " + hours + "h " + minutes + "m";
    }

    private static String regionKey(PSRegion r) {
        return r.getWorld().getName() + ":" + r.getId();
    }

    private static void analyzeAsync(List<PSRegion> regions, Map<UUID, Long> lastPlayed, long durationMillis, boolean deleteWithMembers) {
        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
            long now = System.currentTimeMillis();
            List<PSRegion> toDelete = new ArrayList<>();
            List<PSRegion> warningRegions = new ArrayList<>();
            Map<String, Long> remainingByRegion = new HashMap<>();

            for (PSRegion region : regions) {
                if (region.getOwners().isEmpty()) continue;

                Set<UUID> requiredPlayers = new HashSet<>(region.getOwners());
                if (!deleteWithMembers) requiredPlayers.addAll(region.getMembers());
                long remaining = 0L;
                boolean unknownActivity = false;
                for (UUID uuid : requiredPlayers) {
                    Long last = lastPlayed.get(uuid);
                    if (last == null || last <= 0) {
                        unknownActivity = true;
                        break;
                    }
                    long elapsed = Math.max(0L, now - last);
                    remaining = Math.max(remaining, Math.max(0L, durationMillis - elapsed));
                }
                if (!unknownActivity) {
                    warningRegions.add(region);
                    remainingByRegion.put(regionKey(region), remaining);
                }

                boolean allOwnersInactive = true;
                for (UUID owner : region.getOwners()) {
                    Long last = lastPlayed.get(owner);
                    if (last == null || last <= 0 || now - last < durationMillis) {
                        allOwnersInactive = false;
                        break;
                    }
                }
                boolean activeMember = false;
                if (!deleteWithMembers) {
                    for (UUID member : region.getMembers()) {
                        Long last = lastPlayed.get(member);
                        if (last == null || last <= 0 || now - last < durationMillis) {
                            activeMember = true;
                            break;
                        }
                    }
                }
                if (allOwnersInactive && !activeMember) toDelete.add(region);
            }

            if (!warningRegions.isEmpty()) {
                Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(),
                        () -> warnInactive(warningRegions, remainingByRegion));
            }
            if (!toDelete.isEmpty()) {
                Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> deleteNext(toDelete, 0));
            }
        });
    }

    /** Sends each configured warning once as a region approaches its expiration deadline. */
    private static void warnInactive(List<PSRegion> regions, Map<String, Long> remainingByRegion) {
        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        boolean warnEnabled = c.autoRemoveWarnEnabled == null || c.autoRemoveWarnEnabled;
        if (!warnEnabled) {
            warnLevels.clear();
            return;
        }

        List<Integer> thresholds = warningThresholds(c.autoRemoveWarnDays);

        Set<String> current = new HashSet<>();
        for (PSRegion region : regions) {
            String key = regionKey(region);
            current.add(key);
            long remaining = remainingByRegion.getOrDefault(key, Long.MAX_VALUE);
            int level = -1;
            for (int threshold : thresholds) {
                if (remaining <= threshold * 86400000L) {
                    level = threshold;
                    break;
                }
            }
            if (level < 0) {
                warnLevels.remove(key);
                continue;
            }
            Integer previous = warnLevels.get(key);
            if (previous != null && previous == level) continue;
            warnLevels.put(key, level);

            String regionName = region.getName() == null ? region.getId() : region.getName();
            for (UUID owner : region.getOwners()) {
                Player online = Bukkit.getPlayer(owner);
                if (online == null) continue;
                PSL.msg(online, PSL.AUTO_REMOVE_WARNING.msg()
                        .replace("%region%", regionName)
                        .replace("%days%", String.valueOf((remaining + 86399999L) / 86400000L))
                        .replace("%time%", formatDuration(remaining)));
            }
        }
        warnLevels.keySet().removeIf(id -> !current.contains(id));
    }
    private static List<Integer> warningThresholds(List<?> configured) {
        List<Integer> thresholds = new ArrayList<>();
        if (configured == null) {
            thresholds.add(1);
            thresholds.add(3);
            thresholds.add(7);
        } else {
            for (Object value : configured) {
                Integer threshold = null;
                if (value instanceof Number) {
                    long numeric = ((Number) value).longValue();
                    if (numeric >= 0 && numeric <= Integer.MAX_VALUE) threshold = (int) numeric;
                } else if (value != null) {
                    try {
                        threshold = Integer.parseInt(value.toString().trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (threshold != null && threshold >= 0) thresholds.add(threshold);
            }
        }
        thresholds.sort(Integer::compareTo);
        return thresholds;
    }
    private static boolean isExpiredNow(PSRegion region, long durationMillis, boolean deleteWithMembers) {
        long now = System.currentTimeMillis();
        for (UUID owner : region.getOwners()) {
            if (Bukkit.getPlayer(owner) != null) return false;
            long last = lastPlayedOf(owner);
            if (last <= 0 || now - last < durationMillis) return false;
        }
        if (!deleteWithMembers) {
            for (UUID member : region.getMembers()) {
                if (Bukkit.getPlayer(member) != null) return false;
                long last = lastPlayedOf(member);
                if (last <= 0 || now - last < durationMillis) return false;
            }
        }
        return !region.getOwners().isEmpty();
    }
    private static void deleteNext(List<PSRegion> list, int index) {
        if (index >= list.size()) return;
        PSRegion region = list.get(index);
        PSConfig config = OdisseyProtecciones.getInstance().getConfigOptions();
        long duration = removalDurationMillis(config);
        boolean deleteWithMembers = config.autoRemoveDeleteWithMembersButNoOwners == null
                || config.autoRemoveDeleteWithMembersButNoOwners;
        try {
            if (isExpiredNow(region, duration, deleteWithMembers)) {
                String id = region.getId();
                if (region.deleteRegion(true)) {
                    OdisseyProtecciones.getPluginLogger().info("Region eliminada automaticamente por inactividad: " + id);
                }
            }
        } catch (Exception e) {
            OdisseyProtecciones.getPluginLogger().warning("No se pudo eliminar la region " + region.getId() + ": " + e.getMessage());
        }
        Bukkit.getScheduler().runTaskLater(OdisseyProtecciones.getInstance(), () -> deleteNext(list, index + 1), 1);
    }
}
