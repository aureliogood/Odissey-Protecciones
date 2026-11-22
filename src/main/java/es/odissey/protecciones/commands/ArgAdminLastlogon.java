package es.odissey.protecciones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.UUIDCache;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.*;

class ArgAdminLastlogon {
    private static final int LINES_PER_TICK = 100;

    private static final class LastLogonEntry {
        private final String playerName;
        private final long daysSinceLastPlayed;

        private LastLogonEntry(String playerName, long daysSinceLastPlayed) {
            this.playerName = playerName;
            this.daysSinceLastPlayed = daysSinceLastPlayed;
        }
    }

    static boolean argumentAdminLastLogon(CommandSender p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(PSL.COMMAND_REQUIRES_PLAYER_NAME.msg());
            return true;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(args[2]);

        String playerName = args[2];
        long lastPlayed = (System.currentTimeMillis() - op.getLastPlayed()) / 86400000L;

        PSL.msg(p, PSL.ADMIN_LAST_LOGON.msg()
                .replace("%player%", playerName)
                .replace("%days%", "" + lastPlayed));

        if (op.isBanned()) {
            PSL.msg(p, PSL.ADMIN_IS_BANNED.msg()
                    .replace("%player%", playerName));
        }

        return true;
    }

    static boolean argumentAdminLastLogons(CommandSender p, String[] args) {
        int days = 0;
        if (args.length > 2) {
            try {
                days = Integer.parseInt(args[2]);
            } catch (Exception e) {
                PSL.msg(p, PSL.ADMIN_ERROR_PARSING.msg());
                return true;
            }
        }

        final int minDays = days;
        PSL.msg(p, PSL.ADMIN_LASTLOGONS_HEADER.msg()
                .replace("%days%", "" + minDays));

        Bukkit.getScheduler().runTaskAsynchronously(OdisseyProtecciones.getInstance(), () -> {
            Set<UUID> ownerUuids = collectRegionOwnerUuids();
            List<LastLogonEntry> matchingEntries = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (UUID uuid : ownerUuids) {
                try {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    long lastPlayed = (now - offlinePlayer.getLastPlayed()) / 86400000L;
                    if (lastPlayed >= minDays) {
                        matchingEntries.add(new LastLogonEntry(resolvePlayerName(uuid), lastPlayed));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            matchingEntries.sort(Comparator.comparing(entry -> entry.playerName, String.CASE_INSENSITIVE_ORDER));

            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () ->
                    sendLastLogonEntries(p, matchingEntries, ownerUuids.size(), 0));
        });

        return true;
    }

    private static void sendLastLogonEntries(CommandSender sender, List<LastLogonEntry> entries, int checkedCount, int startIndex) {
        int endIndex = Math.min(entries.size(), startIndex + LINES_PER_TICK);
        for (int i = startIndex; i < endIndex; i++) {
            LastLogonEntry entry = entries.get(i);
            PSL.msg(sender, PSL.ADMIN_LASTLOGONS_LINE.msg()
                    .replace("%player%", entry.playerName)
                    .replace("%time%", "" + entry.daysSinceLastPlayed));
        }

        if (endIndex < entries.size()) {
            int nextIndex = endIndex;
            Bukkit.getScheduler().runTaskLater(OdisseyProtecciones.getInstance(),
                    () -> sendLastLogonEntries(sender, entries, checkedCount, nextIndex), 1L);
            return;
        }

        PSL.msg(sender, PSL.ADMIN_LASTLOGONS_FOOTER.msg()
                .replace("%count%", "" + entries.size())
                .replace("%checked%", "" + checkedCount));
    }

    private static Set<UUID> collectRegionOwnerUuids() {
        Set<UUID> owners = new HashSet<>();
        for (Map.Entry<World, RegionManager> entry : WGUtils.getAllRegionManagers().entrySet()) {
            World world = entry.getKey();
            RegionManager regionManager = entry.getValue();
            if (world == null || regionManager == null) continue;

            for (ProtectedRegion protectedRegion : regionManager.getRegions().values()) {
                PSRegion region = PSRegion.fromWGRegion(world, protectedRegion);
                if (region == null) continue;
                owners.addAll(region.getOwners());
            }
        }
        return owners;
    }

    private static String resolvePlayerName(UUID uuid) {
        String name = UUIDCache.getNameFromUUID(uuid);
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("null")) {
            name = uuid.toString().substring(0, 8);
        }
        return name;
    }
}
