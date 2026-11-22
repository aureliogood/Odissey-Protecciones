package es.odissey.protecciones.storage;

import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.PSConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gestiona la conexion y el esquema de la base de datos de copia de seguridad.
 */
public class Database {

    private final OdisseyProtecciones plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "OdisseyProtecciones-DB");
        t.setDaemon(true);
        return t;
    });

    private Connection connection;
    private boolean enabled = false;
    private boolean mysql = false;
    private boolean auditEnabled = true;
    private String tablePrefix = "ps_";
    private String url;
    private String username;
    private String password;
    private RegionRepository repository;

    public Database(OdisseyProtecciones plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        PSConfig c = plugin.getConfigOptions();
        if (c == null) return false;

        enabled = c.databaseEnabled == null || c.databaseEnabled;
        if (!enabled) return false;

        mysql = "mysql".equalsIgnoreCase(c.databaseType == null ? "sqlite" : c.databaseType.trim());
        auditEnabled = c.databaseAuditEnabled == null || c.databaseAuditEnabled;
        tablePrefix = c.databaseTablePrefix == null || c.databaseTablePrefix.trim().isEmpty()
                ? "ps_" : c.databaseTablePrefix.trim();

        try {
            if (mysql) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String host = c.databaseMysqlHost == null ? "localhost" : c.databaseMysqlHost;
                int port = c.databaseMysqlPort == null ? 3306 : c.databaseMysqlPort;
                String db = c.databaseMysqlDatabase == null ? "odisseyprotecciones" : c.databaseMysqlDatabase;
                username = c.databaseMysqlUsername == null ? "root" : c.databaseMysqlUsername;
                password = c.databaseMysqlPassword == null ? "" : c.databaseMysqlPassword;
                url = "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true&characterEncoding=utf8";
            } else {
                Class.forName("org.sqlite.JDBC");
                File file = new File(plugin.getDataFolder(),
                        c.databaseSqliteFile == null || c.databaseSqliteFile.trim().isEmpty()
                                ? "protecciones.db" : c.databaseSqliteFile.trim());
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                url = "jdbc:sqlite:" + file.getAbsolutePath();
                username = null;
                password = null;
            }

            connection = openConnection();
            createTables();
            repository = new RegionRepository(this);
            plugin.getLogger().info("Base de datos de protecciones conectada (" + (mysql ? "MySQL" : "SQLite") + ").");
            return true;
        } catch (Throwable t) {
            enabled = false;
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
                connection = null;
            }
            plugin.getLogger().log(Level.WARNING, "No se pudo iniciar la base de datos de protecciones: " + t.getMessage(), t);
            return false;
        }
    }

    private Connection openConnection() throws SQLException {
        Connection conn = username == null
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, username, password);
        conn.setAutoCommit(true);
        return conn;
    }

    private void createTables() throws SQLException {
        String regions = regionsTable();
        String audit = auditTable();

        String regionsSql = "CREATE TABLE IF NOT EXISTS " + regions + " ("
                + "world VARCHAR(64) NOT NULL,"
                + "region_id VARCHAR(96) NOT NULL,"
                + "name VARCHAR(160),"
                + "block_material VARCHAR(160),"
                + "block_x INT NOT NULL DEFAULT 0,"
                + "block_y INT NOT NULL DEFAULT 0,"
                + "block_z INT NOT NULL DEFAULT 0,"
                + "min_x INT NOT NULL,"
                + "min_y INT NOT NULL,"
                + "min_z INT NOT NULL,"
                + "max_x INT NOT NULL,"
                + "max_y INT NOT NULL,"
                + "max_z INT NOT NULL,"
                + "priority INT NOT NULL DEFAULT 0,"
                + "owners TEXT,"
                + "members TEXT,"
                + "flags TEXT,"
                + "created_at BIGINT NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (world, region_id)"
                + ")";

        String auditSql = "CREATE TABLE IF NOT EXISTS " + audit + " ("
                + (mysql ? "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                : "id INTEGER PRIMARY KEY AUTOINCREMENT,")
                + "created_at BIGINT NOT NULL,"
                + "actor_uuid VARCHAR(64),"
                + "actor_name VARCHAR(64),"
                + "action VARCHAR(48) NOT NULL,"
                + "world VARCHAR(64),"
                + "region_id VARCHAR(96),"
                + "details TEXT"
                + ")";

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(regionsSql);
            if (auditEnabled) st.executeUpdate(auditSql);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMysql() {
        return mysql;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public String regionsTable() {
        return tablePrefix + "regions";
    }

    public String auditTable() {
        return tablePrefix + "audit";
    }

    public RegionRepository getRepository() {
        return repository;
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
        }
        return connection;
    }

    public void runAsync(Runnable runnable) {
        if (!enabled || executor.isShutdown()) return;
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Error en la base de datos de protecciones: " + t.getMessage(), t);
            }
        });
    }

    public void saveAllAsync(List<RegionSnapshot> snapshots) {
        runAsync(() -> repository.saveAll(snapshots));
    }

    public void saveRegionAsync(RegionSnapshot snapshot) {
        runAsync(() -> repository.saveOne(snapshot));
    }

    public void deleteRegionAsync(String world, String regionId) {
        runAsync(() -> repository.delete(world, regionId));
    }

    public void audit(String actorUuid, String actorName, String action, String world, String regionId, String details) {
        if (!enabled || !auditEnabled || repository == null) return;
        runAsync(() -> repository.audit(actorUuid, actorName, action, world, regionId, details));
    }

    public void saveAllSync(List<RegionSnapshot> snapshots) {
        if (!enabled || repository == null) return;
        repository.saveAll(snapshots);
    }

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
                connection = null;
            }
        }
        enabled = false;
    }
}
