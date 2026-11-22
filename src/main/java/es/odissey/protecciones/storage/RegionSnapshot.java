package es.odissey.protecciones.storage;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.math.BlockVector2;
import es.odissey.protecciones.FlagHandler;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Copia serializable de una region de proteccion para guardarla en la base de datos.
 */
public class RegionSnapshot {

    public String world;
    public String regionId;
    public String name;
    public String blockMaterial;
    public int blockX;
    public int blockY;
    public int blockZ;
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    public int priority;
    public String owners = "";
    public String members = "";
    public String flagsJson = "{}";
    public long createdAt;
    public long updatedAt;

    public static List<RegionSnapshot> collectAll() {
        List<RegionSnapshot> list = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            try {
                list.addAll(collect(world));
            } catch (Throwable t) {
                OdisseyProtecciones.getPluginLogger().warning(
                        "No se pudieron leer las protecciones de " + world.getName() + ": " + t.getMessage());
            }
        }
        return list;
    }

    public static List<RegionSnapshot> collect(World world) {
        List<RegionSnapshot> list = new ArrayList<>();
        if (world == null) return list;
        com.sk89q.worldguard.protection.managers.RegionManager rm = WGUtils.getRegionManagerWithWorld(world);
        if (rm == null) return list;

        for (ProtectedRegion region : rm.getRegions().values()) {
            if (!OdisseyProtecciones.isPSRegionFormat(region)) continue;
            try {
                list.add(from(world, region));
            } catch (Throwable t) {
                OdisseyProtecciones.getPluginLogger().warning(
                        "No se pudo respaldar la region " + region.getId() + ": " + t.getMessage());
            }
        }
        return list;
    }

    public static RegionSnapshot from(World world, ProtectedRegion region) {
        RegionSnapshot s = new RegionSnapshot();
        long now = System.currentTimeMillis();

        s.world = world.getName();
        s.regionId = region.getId();
        s.name = region.getFlag(FlagHandler.PS_NAME);
        s.blockMaterial = region.getFlag(FlagHandler.PS_BLOCK_MATERIAL);

        String home = region.getFlag(FlagHandler.PS_HOME);
        s.blockX = minX(region);
        s.blockY = minY(region);
        s.blockZ = minZ(region);
        if (home != null) {
            String[] parts = home.trim().split("\\s+");
            try {
                if (parts.length >= 3) {
                    s.blockX = (int) Double.parseDouble(parts[0]);
                    s.blockY = (int) Double.parseDouble(parts[1]);
                    s.blockZ = (int) Double.parseDouble(parts[2]);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        s.minX = minX(region);
        s.minY = minY(region);
        s.minZ = minZ(region);
        s.maxX = region.getMaximumPoint().getX();
        s.maxY = region.getMaximumPoint().getY();
        s.maxZ = region.getMaximumPoint().getZ();
        s.priority = region.getPriority();

        s.owners = joinUuids(region.getOwners().getUniqueIds());
        s.members = joinUuids(region.getMembers().getUniqueIds());
        s.flagsJson = flagsToJson(region);

        s.createdAt = now;
        s.updatedAt = now;
        return s;
    }

    private static int minX(ProtectedRegion region) {
        return region.getMinimumPoint().getX();
    }

    private static int minY(ProtectedRegion region) {
        return region.getMinimumPoint().getY();
    }

    private static int minZ(ProtectedRegion region) {
        return region.getMinimumPoint().getZ();
    }

    private static String joinUuids(Set<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : uuids) {
            if (uuid == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(uuid);
        }
        return sb.toString();
    }

    private static String flagsToJson(ProtectedRegion region) {
        JSONObject flags = new JSONObject();
        for (Map.Entry<Flag<?>, Object> entry : region.getFlags().entrySet()) {
            try {
                Object marshalled = ((Flag) entry.getKey()).marshal(entry.getValue());
                if (marshalled == null) continue;
                flags.put(entry.getKey().getName(), toJsonValue(marshalled));
            } catch (Throwable ignored) {
            }
        }
        JSONObject root = new JSONObject();
        root.put("flags", flags);
        if (region instanceof ProtectedPolygonalRegion polygon) {
            root.put("shape", "polygon");
            JSONArray points = new JSONArray();
            for (BlockVector2 point : polygon.getPoints()) {
                JSONArray pair = new JSONArray();
                pair.add(point.getX());
                pair.add(point.getZ());
                points.add(pair);
            }
            root.put("points", points);
        } else {
            root.put("shape", "cuboid");
        }
        return root.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static Object toJsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Collection) {
            JSONArray array = new JSONArray();
            for (Object element : (Collection<?>) value) {
                array.add(toJsonValue(element));
            }
            return array;
        }
        if (value instanceof Map) {
            JSONObject object = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                object.put(String.valueOf(entry.getKey()), toJsonValue(entry.getValue()));
            }
            return object;
        }
        return String.valueOf(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void applyFlags(ProtectedRegion region) {
        if (flagsJson == null || flagsJson.isEmpty()) return;
        try {
            Object parsed = new JSONParser().parse(flagsJson);
            if (!(parsed instanceof JSONObject)) return;
            Object flagsObj = ((JSONObject) parsed).get("flags");
            if (!(flagsObj instanceof JSONObject)) return;

            JSONObject flags = (JSONObject) flagsObj;
            for (Object keyObj : flags.keySet()) {
                String flagName = String.valueOf(keyObj);
                Flag flag = WGUtils.getFlagRegistry().get(flagName);
                if (flag == null) continue;
                try {
                    Object value = flag.unmarshal(toJavaValue(flags.get(keyObj)));
                    if (value != null) region.setFlag(flag, value);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            OdisseyProtecciones.getPluginLogger().warning(
                    "No se pudieron restaurar las flags de la region " + regionId + ": " + t.getMessage());
        }
    }

    private static Object toJavaValue(Object value) {
        if (value instanceof JSONArray) {
            List<Object> list = new ArrayList<>();
            for (Object element : (JSONArray) value) {
                list.add(toJavaValue(element));
            }
            return list;
        }
        if (value instanceof JSONObject) {
            return value;
        }
        return value;
    }

    public ProtectedRegion toProtectedRegion() {
        String shape = null;
        List<BlockVector2> points = new ArrayList<>();
        try {
            Object parsed = new JSONParser().parse(flagsJson == null ? "{}" : flagsJson);
            if (parsed instanceof JSONObject root) {
                Object rawShape = root.get("shape");
                shape = rawShape == null ? null : String.valueOf(rawShape);
                Object rawPoints = root.get("points");
                if (rawPoints instanceof JSONArray array) {
                    for (Object rawPoint : array) {
                        if (!(rawPoint instanceof JSONArray pair) || pair.size() < 2) return null;
                        points.add(BlockVector2.at(
                                ((Number) pair.get(0)).intValue(), ((Number) pair.get(1)).intValue()));
                    }
                }
            }
        } catch (Exception invalid) {
            return null;
        }

        // Snapshots antiguos no guardaban la forma. No convertirlos a cubo:
        // una region poligonal podria terminar protegiendo todo su rectangulo.
        if ("polygon".equals(shape)) {
            if (points.size() < 3) return null;
            ProtectedPolygonalRegion region = new ProtectedPolygonalRegion(regionId, points, minY, maxY);
            region.setPriority(priority);
            applyOwnersAndMembers(region);
            applyFlags(region);
            return region;
        }
        if (!"cuboid".equals(shape)) return null;
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId,
                BlockVector3.at(minX, minY, minZ), BlockVector3.at(maxX, maxY, maxZ));
        region.setPriority(priority);
        applyOwnersAndMembers(region);
        applyFlags(region);
        return region;
    }

    private void applyOwnersAndMembers(ProtectedRegion region) {
        Set<UUID> ownerIds = parseUuids(owners);
        Set<UUID> memberIds = parseUuids(members);
        for (UUID uuid : ownerIds) {
            try {
                region.getOwners().addPlayer(uuid);
            } catch (Throwable ignored) {
            }
        }
        for (UUID uuid : memberIds) {
            try {
                region.getMembers().addPlayer(uuid);
            } catch (Throwable ignored) {
            }
        }
    }

    private static Set<UUID> parseUuids(String raw) {
        Set<UUID> set = new LinkedHashSet<>();
        if (raw == null || raw.trim().isEmpty()) return set;
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) continue;
            try {
                set.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return set;
    }

    public String key() {
        return world + "\u0000" + regionId;
    }
}
