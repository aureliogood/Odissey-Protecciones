package es.odissey.protecciones.storage;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Acceso a las tablas de protecciones y auditoria.
 */
public class RegionRepository {

    private final Database database;

    public RegionRepository(Database database) {
        this.database = database;
    }

    public synchronized void saveAll(List<RegionSnapshot> snapshots) {
        if (snapshots == null) return;
        try {
            Connection conn = database.connection();
            Map<String, Long> existingCreatedAt = loadCreatedAt(conn);

            // claves presentes en el snapshot: todo lo que no aparezca aqui se purga
            Set<String> present = new HashSet<>();
            for (RegionSnapshot s : snapshots) {
                if (s != null && s.world != null && s.regionId != null) present.add(s.key());
            }

            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                purgeMissing(conn, present);
                if (!snapshots.isEmpty()) deleteAndInsert(conn, snapshots, existingCreatedAt);
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            OdisseyProtecciones.getPluginLogger().log(Level.WARNING,
                    "No se pudo guardar el respaldo de protecciones: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina de la tabla las regiones que ya no existen en WorldGuard. Sin esto, las
     * regiones borradas por vias no hookeadas (merge, /region remove, recreacion) volvian
     * a aparecer al reiniciar por culpa de {@link #restoreMissing()}.
     */
    private void purgeMissing(Connection conn, Set<String> present) throws SQLException {
        List<String[]> toDelete = new ArrayList<>();
        String selectSql = "SELECT world, region_id FROM " + database.regionsTable();
        try (PreparedStatement select = conn.prepareStatement(selectSql);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String world = rs.getString("world");
                String regionId = rs.getString("region_id");
                if (world == null || regionId == null) continue;
                // si el mundo no esta cargado no se puede saber si la region existe:
                // se conserva el respaldo para no borrarlo por error
                if (Bukkit.getWorld(world) == null) continue;
                if (!present.contains(world + "\u0000" + regionId)) {
                    toDelete.add(new String[]{world, regionId});
                }
            }
        }
        if (toDelete.isEmpty()) return;

        String deleteSql = "DELETE FROM " + database.regionsTable() + " WHERE world = ? AND region_id = ?";
        try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
            for (String[] key : toDelete) {
                delete.setString(1, key[0]);
                delete.setString(2, key[1]);
                delete.addBatch();
            }
            delete.executeBatch();
        }
    }

    /**
     * Guarda una sola region sin leer la tabla completa (acciones puntuales: miembros,
     * flags, home, transferencias...).
     */
    public synchronized void saveOne(RegionSnapshot s) {
        if (s == null || s.world == null || s.regionId == null) return;
        try {
            Connection conn = database.connection();
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                long createdAt = s.createdAt;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT created_at FROM " + database.regionsTable()
                                + " WHERE world = ? AND region_id = ?")) {
                    ps.setString(1, s.world);
                    ps.setString(2, s.regionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) createdAt = rs.getLong(1);
                    }
                }
                deleteAndInsert(conn, Collections.singletonList(s),
                        Collections.singletonMap(s.key(), createdAt));
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            OdisseyProtecciones.getPluginLogger().log(Level.WARNING,
                    "No se pudo guardar el respaldo de la region " + s.regionId + ": " + e.getMessage(), e);
        }
    }

    private void deleteAndInsert(Connection conn, List<RegionSnapshot> snapshots,
                                 Map<String, Long> existingCreatedAt) throws SQLException {
        String deleteSql = "DELETE FROM " + database.regionsTable() + " WHERE world = ? AND region_id = ?";
        String insertSql = "INSERT INTO " + database.regionsTable()
                + " (world, region_id, name, block_material, block_x, block_y, block_z,"
                + " min_x, min_y, min_z, max_x, max_y, max_z, priority, owners, members, flags,"
                + " created_at, updated_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement delete = conn.prepareStatement(deleteSql);
             PreparedStatement insert = conn.prepareStatement(insertSql)) {
            for (RegionSnapshot s : snapshots) {
                if (s == null || s.world == null || s.regionId == null) continue;

                delete.setString(1, s.world);
                delete.setString(2, s.regionId);
                delete.addBatch();

                long createdAt = existingCreatedAt.getOrDefault(s.key(),
                        s.createdAt > 0 ? s.createdAt : System.currentTimeMillis());

                int i = 1;
                insert.setString(i++, s.world);
                insert.setString(i++, s.regionId);
                insert.setString(i++, s.name);
                insert.setString(i++, s.blockMaterial);
                insert.setInt(i++, s.blockX);
                insert.setInt(i++, s.blockY);
                insert.setInt(i++, s.blockZ);
                insert.setInt(i++, s.minX);
                insert.setInt(i++, s.minY);
                insert.setInt(i++, s.minZ);
                insert.setInt(i++, s.maxX);
                insert.setInt(i++, s.maxY);
                insert.setInt(i++, s.maxZ);
                insert.setInt(i++, s.priority);
                insert.setString(i++, s.owners);
                insert.setString(i++, s.members);
                insert.setString(i++, s.flagsJson);
                insert.setLong(i++, createdAt);
                insert.setLong(i, s.updatedAt > 0 ? s.updatedAt : System.currentTimeMillis());
                insert.addBatch();
            }
            delete.executeBatch();
            insert.executeBatch();
        }
    }

    private Map<String, Long> loadCreatedAt(Connection conn) throws SQLException {
        Map<String, Long> map = new HashMap<>();
        String sql = "SELECT world, region_id, created_at FROM " + database.regionsTable();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("world") + "\u0000" + rs.getString("region_id"), rs.getLong("created_at"));
            }
        }
        return map;
    }

    public synchronized void delete(String world, String regionId) {
        try (PreparedStatement ps = database.connection().prepareStatement(
                "DELETE FROM " + database.regionsTable() + " WHERE world = ? AND region_id = ?")) {
            ps.setString(1, world);
            ps.setString(2, regionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            OdisseyProtecciones.getPluginLogger().log(Level.WARNING,
                    "No se pudo eliminar el respaldo de la region " + regionId + ": " + e.getMessage(), e);
        }
    }

    public synchronized List<RegionSnapshot> loadAll() {
        List<RegionSnapshot> list = new ArrayList<>();
        String sql = "SELECT * FROM " + database.regionsTable();
        try (PreparedStatement ps = database.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RegionSnapshot s = new RegionSnapshot();
                s.world = rs.getString("world");
                s.regionId = rs.getString("region_id");
                s.name = rs.getString("name");
                s.blockMaterial = rs.getString("block_material");
                s.blockX = rs.getInt("block_x");
                s.blockY = rs.getInt("block_y");
                s.blockZ = rs.getInt("block_z");
                s.minX = rs.getInt("min_x");
                s.minY = rs.getInt("min_y");
                s.minZ = rs.getInt("min_z");
                s.maxX = rs.getInt("max_x");
                s.maxY = rs.getInt("max_y");
                s.maxZ = rs.getInt("max_z");
                s.priority = rs.getInt("priority");
                s.owners = rs.getString("owners");
                s.members = rs.getString("members");
                s.flagsJson = rs.getString("flags");
                s.createdAt = rs.getLong("created_at");
                s.updatedAt = rs.getLong("updated_at");
                list.add(s);
            }
        } catch (SQLException e) {
            OdisseyProtecciones.getPluginLogger().log(Level.WARNING,
                    "No se pudo leer el respaldo de protecciones: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Restaura las regiones que existen en la base de datos pero no en WorldGuard.
     *
     * @return numero de regiones restauradas
     */
    public synchronized int restoreMissing() {
        int restored = 0;
        Set<RegionManager> changedManagers = new HashSet<>();

        for (RegionSnapshot snapshot : loadAll()) {
            World world = snapshot.world == null ? null : Bukkit.getWorld(snapshot.world);
            if (world == null) continue;

            RegionManager manager = WGUtils.getRegionManagerWithWorld(world);
            if (manager == null) continue;
            if (manager.getRegion(snapshot.regionId) != null) continue;

            try {
                ProtectedRegion region = snapshot.toProtectedRegion();
                if (region == null) {
                    OdisseyProtecciones.getPluginLogger().warning("No se restaura la region "
                            + snapshot.regionId + ": el respaldo no contiene una geometria segura.");
                    continue;
                }
                manager.addRegion(region);
                changedManagers.add(manager);
                restored++;

                OdisseyProtecciones.getPluginLogger().info("Proteccion restaurada desde la base de datos: "
                        + snapshot.regionId + " (" + snapshot.world + ")");
            } catch (Throwable t) {
                OdisseyProtecciones.getPluginLogger().warning("No se pudo restaurar la region "
                        + snapshot.regionId + ": " + t.getMessage());
            }
        }

        for (RegionManager manager : changedManagers) {
            try {
                manager.save();
            } catch (Throwable t) {
                OdisseyProtecciones.getPluginLogger().warning(
                        "No se pudieron guardar las regiones restauradas: " + t.getMessage());
            }
        }

        return restored;
    }

    public synchronized void audit(String actorUuid, String actorName, String action,
                      String world, String regionId, String details) {
        if (!database.isAuditEnabled()) return;
        try (PreparedStatement ps = database.connection().prepareStatement(
                "INSERT INTO " + database.auditTable()
                        + " (created_at, actor_uuid, actor_name, action, world, region_id, details)"
                        + " VALUES (?,?,?,?,?,?,?)")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, actorUuid);
            ps.setString(3, actorName);
            ps.setString(4, action);
            ps.setString(5, world);
            ps.setString(6, regionId);
            ps.setString(7, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            OdisseyProtecciones.getPluginLogger().log(Level.WARNING,
                    "No se pudo guardar la auditoria: " + e.getMessage(), e);
        }
    }
}

