package es.odissey.protecciones.utils;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.util.profile.Profile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UUIDCache {
    private static Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
    private static Map<String, UUID> nameToUUID = new ConcurrentHashMap<>();

    // toLowerCase for case insensitive search

    public static UUID getUUIDFromName(String name) {
        if (name == null) return null;
        return nameToUUID.get(name.toLowerCase());
    }

    public static String getNameFromUUID(UUID uuid) {
        if (uuid == null) return null;
        return uuidToName.get(uuid);
    }

    public static boolean containsName(String name) {
        if (name == null) return false;
        return nameToUUID.containsKey(name.toLowerCase());
    }

    public static boolean containsUUID(UUID uuid) {
        if (uuid == null) return false;
        return uuidToName.containsKey(uuid);
    }

    public static void storeUUIDNamePair(UUID uuid, String name) {
        if (uuid == null || name == null) return;
        uuidToName.put(uuid, name);
        nameToUUID.put(name.toLowerCase(), uuid);
    }

    public static void removeUUID(UUID uuid) {
        if (uuid == null) return;
        String name = uuidToName.remove(uuid);
        if (name != null) nameToUUID.remove(name.toLowerCase());
    }

    public static void removeName(String name) {
        if (name == null) return;
        UUID uuid = nameToUUID.remove(name.toLowerCase());
        if (uuid != null) uuidToName.remove(uuid);
    }

    public static void storeWGProfile(UUID uuid, String name) {
        WorldGuard.getInstance().getProfileCache().put(new Profile(uuid, name));
    }
}
