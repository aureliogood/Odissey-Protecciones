package es.odissey.protecciones;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.gui.ColorUtil;
import es.odissey.protecciones.gui.MenuManager;
import es.odissey.protecciones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and maintains a floating hologram (a TextDisplay entity)
 * above every protection block. All content and behaviour is configurable in config.yml ([hologram]).
 */
public class HologramManager {

    private static HologramManager instance;

    private final List<String> defaultLines = Arrays.asList(
            "&b&l%region%",
            "&7Estado: &f%status%",
            "&7Dueno: &f%owner%",
            "&7Miembros: &f%members%",
            "&7Radio: &f%radius%",
            "&7Se elimina en: &c%days_until_removal%",
            "&7Ubicacion: &f%world% &7(&f%x%&7, &f%y%&7, &f%z%&7)",
            "&8&m--------------------",
            "&eClick para gestionar"
    );

    private BukkitTask task;
    private BukkitTask refreshTask;

    /** numero de regiones procesadas por tick al refrescar hologramas (evita picos con muchas regiones) */
    private static final int REFRESH_PER_TICK = 40;

    private boolean enabled = true;
    private double offsetY = 1.3;
    private int updateInterval = 60;
    private Display.Billboard billboard = Display.Billboard.CENTER;
    private boolean seeThrough = false;
    private boolean background = true;
    private Color backgroundColor = null;
    private boolean shadowed = false;
    private List<String> lines = new ArrayList<>(defaultLines);

    // cache de nombres para no consultar OfflinePlayer en cada refresco de hologramas
    private static final long NAME_CACHE_TTL_MS = 5 * 60 * 1000L;
    private final Map<UUID, String> nameCache = new java.util.HashMap<>();
    private final Map<UUID, Long> nameCacheTime = new java.util.HashMap<>();

    public static HologramManager getInstance() {
        if (instance == null) instance = new HologramManager();
        return instance;
    }

    public static NamespacedKey getRegionKey() {
        return new NamespacedKey(OdisseyProtecciones.getInstance(), "ps_holo_region");
    }

    public static NamespacedKey getWorldKey() {
        return new NamespacedKey(OdisseyProtecciones.getInstance(), "ps_holo_world");
    }

    // ~~~~~~~~~~~~~~~~~ Lifecycle ~~~~~~~~~~~~~~~~~

    public void loadSettings() {
        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        enabled = c.hologramEnabled == null || c.hologramEnabled;
        offsetY = c.hologramOffsetY == null ? 1.3 : c.hologramOffsetY;
        updateInterval = c.hologramUpdateInterval == null ? 60 : Math.max(0, c.hologramUpdateInterval);
        seeThrough = c.hologramSeeThrough != null && c.hologramSeeThrough;
        background = c.hologramBackground == null || c.hologramBackground;
        backgroundColor = parseColor(c.hologramBackgroundColor);
        shadowed = c.hologramShadowed != null && c.hologramShadowed;

        if (c.hologramBillboard != null) {
            try {
                billboard = Display.Billboard.valueOf(c.hologramBillboard.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                billboard = Display.Billboard.CENTER;
            }
        } else {
            billboard = Display.Billboard.CENTER;
        }

        if (c.hologramLines != null && !c.hologramLines.isEmpty()) {
            lines = new ArrayList<>();
            for (String line : c.hologramLines) lines.add(normalizeRadiusLine(line));
        } else {
            lines = new ArrayList<>(defaultLines);
        }
    }

    public void start() {
        loadSettings();
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (enabled) {
            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), this::queueRefresh);
            if (updateInterval > 0) {
                long ticks = updateInterval * 20L;
                task = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), this::queueRefresh, ticks, ticks);
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        nameCache.clear();
        nameCacheTime.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ~~~~~~~~~~~~~~~~~ Spawning ~~~~~~~~~~~~~~~~~

    public void spawn(PSRegion r) {
        if (!enabled || r == null || r instanceof PSGroupRegion || r.getTypeOptions() == null) return;
        try {
            remove(r);
            Location base = r.getProtectBlock().getLocation();
            World w = base.getWorld();
            if (w == null) return;
            Location loc = base.clone().add(0.5, offsetY, 0.5);

            TextDisplay td = w.spawn(loc, TextDisplay.class);
            td.setText(buildText(r));
            applyStyle(td);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setPersistent(false);
            PersistentDataContainer pdc = td.getPersistentDataContainer();
            pdc.set(getRegionKey(), PersistentDataType.STRING, r.getId());
            pdc.set(getWorldKey(), PersistentDataType.STRING, w.getName());
            r.getWGRegion().setFlag(FlagHandler.PS_HOLOGRAM, td.getUniqueId().toString());
        } catch (Throwable ex) {
            OdisseyProtecciones.getPluginLogger().warning("No se pudo crear el holograma: " + ex.getMessage());
        }
    }

    public void update(PSRegion r) {
        if (!enabled || r == null || r instanceof PSGroupRegion || r.getTypeOptions() == null) return;
        try {
            Entity e = getEntity(r.getWGRegion().getFlag(FlagHandler.PS_HOLOGRAM));
            if (!(e instanceof TextDisplay)) {
                spawn(r);
                return;
            }
            ((TextDisplay) e).setText(buildText(r));
            applyStyle((TextDisplay) e);

            String oldClickId = r.getWGRegion().getFlag(FlagHandler.PS_HOLOGRAM_CLICK);
            removeEntity(oldClickId);
            if (oldClickId != null) r.getWGRegion().setFlag(FlagHandler.PS_HOLOGRAM_CLICK, null);
        } catch (Throwable ignored) {
        }
    }

    public void remove(PSRegion r) {
        if (r == null) return;
        try {
            ProtectedRegion wr = r.getWGRegion();
            if (wr == null) return;
            removeEntity(wr.getFlag(FlagHandler.PS_HOLOGRAM));
            removeEntity(wr.getFlag(FlagHandler.PS_HOLOGRAM_CLICK));
            if (wr.getFlag(FlagHandler.PS_HOLOGRAM) != null) wr.setFlag(FlagHandler.PS_HOLOGRAM, null);
            if (wr.getFlag(FlagHandler.PS_HOLOGRAM_CLICK) != null) wr.setFlag(FlagHandler.PS_HOLOGRAM_CLICK, null);
        } catch (Exception ignored) {
        }
    }

    // ~~~~~~~~~~~~~~~~~ Refresh ~~~~~~~~~~~~~~~~~

    public void refreshLoaded() {
        queueRefresh();
    }

    /**
     * Agenda un refresco incremental: se reparte sobre varios ticks (REFRESH_PER_TICK regiones por tick)
     * en lugar de bloquear un unico tick recorriendo todas las regiones del servidor.
     */
    private void queueRefresh() {
        if (!enabled || refreshTask != null) return;

        List<Map.Entry<World, ProtectedRegion>> entries = new ArrayList<>();
        for (Map.Entry<World, RegionManager> entry : WGUtils.getAllRegionManagers().entrySet()) {
            World w = entry.getKey();
            for (ProtectedRegion pr : entry.getValue().getRegions().values()) {
                if (!OdisseyProtecciones.isPSRegion(pr)) continue;
                entries.add(new java.util.AbstractMap.SimpleEntry<>(w, pr));
            }
        }
        if (entries.isEmpty()) return;

        final int[] index = {0};
        refreshTask = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), () -> {
            int processed = 0;
            while (index[0] < entries.size() && processed < REFRESH_PER_TICK) {
                Map.Entry<World, ProtectedRegion> e = entries.get(index[0]++);
                processed++;
                try {
                    World w = e.getKey();
                    // usa el id para calcular la posicion SIN tocar el mundo: getProtectBlock()
                    // cargaria el chunk antes de poder comprobar isChunkLoaded
                    PSLocation psl = WGUtils.parsePSRegionToLocation(e.getValue().getId());
                    if (psl == null) continue;
                    if (!w.isChunkLoaded(psl.x >> 4, psl.z >> 4)) continue;
                    PSRegion r = PSRegion.fromWGRegion(w, e.getValue());
                    if (r == null || r instanceof PSGroupRegion) continue;
                    update(r);
                } catch (Exception ignored) {
                }
            }
            if (index[0] >= entries.size()) {
                refreshTask.cancel();
                refreshTask = null;
            }
        }, 1L, 1L);
    }

    // ~~~~~~~~~~~~~~~~~ Helpers ~~~~~~~~~~~~~~~~~

    private static String normalizeRadiusLine(String line) {
        if (line != null && line.contains("Radio:")
                && line.contains("%size_x%") && line.contains("%size_z%")) {
            return line.substring(0, line.indexOf("%size_x%")) + "%radius%";
        }
        return line;
    }
    private void applyStyle(TextDisplay td) {
        td.setBillboard(billboard);
        td.setSeeThrough(seeThrough);
        td.setShadowed(shadowed);
        if (backgroundColor != null) {
            td.setDefaultBackground(false);
            td.setBackgroundColor(backgroundColor);
        } else {
            td.setDefaultBackground(background);
        }
    }

    /**
     * Acepta "transparent"/"none", "#AARRGGBB", "#RRGGBB" o "a,r,g,b" / "r,g,b".
     * Devuelve null para usar el ajuste booleano [hologram.background].
     */
    private static Color parseColor(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("default")) return null;
        if (s.equalsIgnoreCase("transparent") || s.equalsIgnoreCase("none")) {
            return Color.fromARGB(0, 0, 0, 0);
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
            try {
                if (s.length() == 8) {
                    long v = Long.parseLong(s, 16);
                    return Color.fromARGB((int) ((v >> 24) & 0xFF), (int) ((v >> 16) & 0xFF),
                            (int) ((v >> 8) & 0xFF), (int) (v & 0xFF));
                }
                if (s.length() == 6) {
                    return Color.fromRGB(Integer.parseInt(s.substring(0, 2), 16),
                            Integer.parseInt(s.substring(2, 4), 16),
                            Integer.parseInt(s.substring(4, 6), 16));
                }
            } catch (IllegalArgumentException ignored) {
            }
            return null;
        }
        String[] parts = s.split(",");
        try {
            if (parts.length == 4) {
                return Color.fromARGB(clamp(parts[0]), clamp(parts[1]), clamp(parts[2]), clamp(parts[3]));
            }
            if (parts.length == 3) {
                return Color.fromRGB(clamp(parts[0]), clamp(parts[1]), clamp(parts[2]));
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private static int clamp(String value) {
        return Math.max(0, Math.min(255, Integer.parseInt(value.trim())));
    }

    private String buildText(PSRegion r) {
        Map<String, String> ph = placeholders(r);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = es.odissey.protecciones.gui.Menu.applyPlaceholders(lines.get(i), ph);
            if (i > 0) sb.append('\n');
            sb.append(ColorUtil.color(line));
        }
        return sb.toString();
    }

    private Map<String, String> placeholders(PSRegion r) {
        Map<String, String> ph = new LinkedHashMap<>();
        try {
            ph.putAll(MenuManager.getInstance().regionPlaceholders(r));
        } catch (Exception ignored) {
        }
        ph.put("%location%", locationPlaceholder(r));
        List<String> memberNames = new ArrayList<>();
        for (UUID uuid : r.getMembers()) {
            memberNames.add(nameOf(uuid));
        }
        ph.put("%members_list%", memberNames.isEmpty() ? "-" : String.join(", ", memberNames));
        return ph;
    }

    private String nameOf(UUID uuid) {
        long now = System.currentTimeMillis();
        Long t = nameCacheTime.get(uuid);
        if (t != null && now - t < NAME_CACHE_TTL_MS) {
            String cached = nameCache.get(uuid);
            if (cached != null) return cached;
        }
        // purga perezosa para que la cache no crezca sin limite
        if (nameCache.size() > 256) {
            nameCacheTime.entrySet().removeIf(entry -> now - entry.getValue() >= NAME_CACHE_TTL_MS);
            nameCache.keySet().removeIf(id -> !nameCacheTime.containsKey(id));
        }
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) return uuid.toString().substring(0, 8);
        nameCache.put(uuid, name);
        nameCacheTime.put(uuid, now);
        return name;
    }

    private static String locationPlaceholder(PSRegion r) {
        try {
            PSLocation psl = WGUtils.parsePSRegionToLocation(r.getId());
            if (psl != null) {
                return r.getWorld().getName() + " " + psl.x + ", " + psl.y + ", " + psl.z;
            }
        } catch (Exception ignored) {
        }
        return r.getWorld().getName();
    }

    private Entity getEntity(String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;
        try {
            return Bukkit.getEntity(UUID.fromString(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    private void removeEntity(String uuid) {
        Entity e = getEntity(uuid);
        if (e != null && !e.isDead()) e.remove();
    }
}
