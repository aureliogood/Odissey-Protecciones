package es.odissey.protecciones;

import org.bukkit.Material;

/**
 * Permisos que el dueno de una proteccion puede activar o desactivar individualmente
 * para cada miembro. Se almacenan como un bitmask en la flag {@code ps-member-perms}
 * (una entrada por jugador con el formato "&lt;uuid&gt; &lt;bitmask&gt;").
 */
public enum MemberPermission {

    BUILD("build", "Romper y colocar", Material.DIAMOND_PICKAXE),
    CONTAINERS("containers", "Contenedores", Material.CHEST),
    INTERACT("interact", "Botones y puertas", Material.OAK_DOOR),
    PRESSURE("pressure", "Placas de presion", Material.STONE_PRESSURE_PLATE),
    ITEMS("items", "Usar objetos", Material.FLINT_AND_STEEL),
    ANIMALS("animals", "Animales", Material.WHEAT),
    VEHICLES("vehicles", "Vehiculos", Material.OAK_BOAT),
    PVP("pvp", "PvP", Material.DIAMOND_SWORD);

    public final String id;
    public final String display;
    public final Material icon;

    MemberPermission(String id, String display, Material icon) {
        this.id = id;
        this.display = display;
        this.icon = icon;
    }

    /** valor del bit de este permiso dentro del mask */
    public int bit() {
        return 1 << ordinal();
    }

    public static MemberPermission byId(String id) {
        if (id == null) return null;
        for (MemberPermission p : values()) {
            if (p.id.equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    /** mask con todos los permisos activados */
    public static int all() {
        int mask = 0;
        for (MemberPermission p : values()) mask |= p.bit();
        return mask;
    }
}
