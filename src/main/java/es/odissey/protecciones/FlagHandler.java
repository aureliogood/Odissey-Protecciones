package es.odissey.protecciones;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.ExitFlag;
import es.odissey.protecciones.flags.FarewellFlagHandler;
import es.odissey.protecciones.flags.GreetingFlagHandler;
import es.odissey.protecciones.utils.WGUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.stream.Collectors;

public class FlagHandler {
    public static final List<String> FLAG_GROUPS = Arrays.asList("all", "members", "owners", "nonmembers", "nonowners");

    // Custom WorldGuard Flags
    public static final Flag<String> GREET_ACTION = new StringFlag("greeting-action");
    public static final Flag<String> FAREWELL_ACTION = new StringFlag("farewell-action");

    // Custom WorldGuard Flags used by OdisseyProtecciones
    // Added to blocks on BlockPlaceEvent Listener
    // When adding flags, you may want to add them to the hidden_flags_from_info config option list
    public static final Flag<String> PS_HOME = new StringFlag("ps-home");
    public static final Flag<String> PS_BLOCK_MATERIAL = new StringFlag("ps-block-material");
    public static final Flag<String> PS_NAME = new StringFlag("ps-name");
    public static final Flag<Set<String>> PS_MERGED_REGIONS = new SetFlag<>("ps-merged-regions", new StringFlag("ps-merged-region"));
    public static final Flag<Set<String>> PS_MERGED_REGIONS_TYPES = new SetFlag<>("ps-merged-regions-types", new StringFlag("ps-merged-region-type")); // each entry: "[psID] [type]"

    public static final Flag<String> PS_LANDLORD = new StringFlag("ps-landlord");
    public static final Flag<String> PS_TENANT = new StringFlag("ps-tenant");
    public static final Flag<String> PS_RENT_PERIOD = new StringFlag("ps-rent-period");
    public static final Flag<Double> PS_PRICE = new DoubleFlag("ps-price");
    public static final Flag<Double> PS_RENT_LAST_PAID = new DoubleFlag("ps-rent-last-paid");
    public static final Flag<Boolean> PS_FOR_SALE = new BooleanFlag("ps-for-sale");
    public static final Flag<Set<String>> PS_RENT_SETTINGS = new SetFlag<>("ps-rent-settings", new StringFlag("ps-rent-setting")); // TODO
    public static final Flag<Set<String>> PS_TAX_PAYMENTS_DUE = new SetFlag<>("ps-tax-payments-due", new StringFlag("ps-tax-payment"));
    public static final Flag<Set<String>> PS_TAX_LAST_PAYMENT_ADDED = new SetFlag<>("ps-tax-last-payment-added", new StringFlag("ps-tax-last-payment-entry"));
    public static final Flag<String> PS_TAX_AUTOPAYER = new StringFlag("ps-tax-autopayer");
    // Permisos por miembro: cada entrada es "<uuid> <bitmask>" (ver MemberPermission)
    public static final Flag<Set<String>> PS_MEMBER_PERMS = new SetFlag<>("ps-member-perms", new StringFlag("ps-member-perm"));
    // Hologram entity references (stored so holograms survive restarts)
    public static final Flag<String> PS_HOLOGRAM = new StringFlag("ps-hologram");
    public static final Flag<String> PS_HOLOGRAM_CLICK = new StringFlag("ps-hologram-click");

    // called on initial start
    static void registerFlags() {
        FlagRegistry registry = WGUtils.getFlagRegistry();

        List<Flag<?>> flags = Arrays.asList(
                PS_HOME, PS_BLOCK_MATERIAL, PS_NAME, PS_MERGED_REGIONS, PS_MERGED_REGIONS_TYPES,
                PS_LANDLORD, PS_TENANT, PS_RENT_PERIOD, PS_PRICE, PS_RENT_LAST_PAID, PS_FOR_SALE,
                PS_RENT_SETTINGS, PS_TAX_PAYMENTS_DUE, PS_TAX_LAST_PAYMENT_ADDED, PS_TAX_AUTOPAYER,
                PS_MEMBER_PERMS, PS_HOLOGRAM, PS_HOLOGRAM_CLICK,
                GREET_ACTION, FAREWELL_ACTION);

        // registrar cada flag por separado: antes, un unico conflicto abortaba el registro del resto
        for (Flag<?> flag : flags) {
            try {
                registry.register(flag);
            } catch (FlagConflictException e) {
                if (flag == GREET_ACTION || flag == FAREWELL_ACTION) {
                    // compatibilidad: si otra fuente ya registro estas flags, no es un problema
                } else {
                    Bukkit.getLogger().severe("Conflicto de flag \"" + flag.getName() + "\"! El plugin no funcionara correctamente! Contacta a los desarrolladores del plugin.");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("No se pudo registrar la flag \"" + flag.getName() + "\": " + e.getMessage());
            }
        }
    }

    static void registerHandlers() {
        SessionManager sessionManager = WorldGuard.getInstance().getPlatform().getSessionManager();
        sessionManager.registerHandler(GreetingFlagHandler.FACTORY, ExitFlag.FACTORY);
        sessionManager.registerHandler(FarewellFlagHandler.FACTORY, ExitFlag.FACTORY);
    }

    // adds flag permissions for ALL registered WorldGuard flags
    // by default, all players have access to it
    static void initializePermissions() {
        for (Flag<?> flag : WGUtils.getFlagRegistry().getAll()) {
            Bukkit.getPluginManager().addPermission(new Permission("odisseyprotecciones.flags.edit." + flag.getName(),
                    "Given to all players by default. Remove if you do not want the player to have the ability to edit this flag with /ps flag.",
                    PermissionDefault.TRUE));
        }
    }

    // Add the correct flags for the ps region
    static void initCustomFlagsForPS(ProtectedRegion region, Location l, PSProtectBlock cpb) {
        String home = l.getBlockX() + cpb.homeXOffset + " ";
        home += (l.getBlockY() + cpb.homeYOffset) + " ";
        home += (l.getBlockZ() + cpb.homeZOffset);
        region.setFlag(PS_HOME, home);

        region.setFlag(PS_BLOCK_MATERIAL, cpb.type);
    }

    public static List<String> getPlayerPlaceholderFlags() {
        return Arrays.asList("greeting", "greeting-action", "farewell", "farewell-action");
    }

    // Edit flags that require placeholders (variables)
    public static void initDefaultFlagPlaceholders(HashMap<Flag<?>, Object> flags, Player p) {
        for (Flag<?> f : getPlayerPlaceholderFlags().stream().map(WGUtils.getFlagRegistry()::get).collect(Collectors.toList())) {
            if (flags.get(f) != null) {
                String s = (String) flags.get(f);

                // apply placeholders
                if (OdisseyProtecciones.getInstance().isPlaceholderAPISupportEnabled()) {
                    s = PlaceholderAPI.setPlaceholders(p, s);
                }
                flags.put(f, s.replaceAll("%player%", p.getName()));
            }
        }
    }

    // Initializes user defined default flags for block
    // also initializes allowed flags list
    static void initDefaultFlagsForBlock(PSProtectBlock b) {
        // initialize allowed flags list
        b.allowedFlags = new LinkedHashMap<>();
        for (String f : b.allowedFlagsRaw == null ? java.util.Collections.<String>emptyList() : b.allowedFlagsRaw) {
            try {
                String[] spl = f.split(" ");
                if (spl[0].equals("-g")) {
                    String[] splGroups = spl[1].split(",");
                    List<String> groups = new ArrayList<>();
                    for (String g : splGroups) {
                        if (FLAG_GROUPS.contains(g)) groups.add(g);
                    }

                    b.allowedFlags.put(spl[2], groups);
                } else {
                    b.allowedFlags.put(f, FLAG_GROUPS);
                }
            } catch (Exception e) {
                OdisseyProtecciones.getInstance().getLogger().warning("Omitiendo flag " + f + ". Configuraste correctamente la seccion allowed_flags?");
                e.printStackTrace();
            }
        }

        // initialize default flags
        b.regionFlags = new HashMap<>();

        // loop through default flags
        List<String> configuredFlags = new ArrayList<>();
        for (String rawFlag : b.flags == null ? java.util.Collections.<String>emptyList() : b.flags) {
            configuredFlags.addAll(normalizeLegacyDefaultFlag(rawFlag));
        }
        for (String flagraw : configuredFlags) {
            String[] split = flagraw.split(" ");
            String settings = "", // flag settings (after flag name)
                    group = "", // -g group
                    flagName = split[0];
            boolean isEmpty = false; // whether or not it's the -e flag at beginning

            try {
                int startInd = 1;
                if (split[0].equals("-g")) { // if it's a group flag
                    group = split[1];
                    flagName = split[2];
                    startInd = 3; // ex. -g nonmembers tnt deny
                } else if (split[0].equals("-e")) { // if it's an empty flag
                    isEmpty = true;
                    flagName = split[1];
                    startInd = 2; // ex. -e deny-message
                }

                // get settings (after flag name)
                for (int i = startInd; i < split.length; i++) settings += split[i] + " ";
                settings = settings.trim();

                // if the setting is set to -e, change to empty flag
                if (settings.equals("-e")) {
                    isEmpty = true;
                }

                // determine worldguard flag object
                Flag<?> flag = Flags.fuzzyMatchFlag(WGUtils.getFlagRegistry(), flagName);
                if (flag == null) {
                    OdisseyProtecciones.getPluginLogger().warning(
                            String.format("Flag predeterminada desconocida \"%s\"; se omite. Revisa la compatibilidad con WorldGuard.", flagName));
                    continue;
                }
                FlagContext fc = FlagContext.create().setInput(settings).build();

                // warn if flag setting has already been set
                if (b.regionFlags.containsKey(flag)) {
                    OdisseyProtecciones.getPluginLogger().warning(String.format("Flags predeterminadas duplicadas (solo se puede aplicar una configuracion por flag)! Sobrescribiendo el valor anterior establecido para %s con \"%s\" ...", flagName, flagraw));
                }

                // apply flag
                if (isEmpty) { // empty flag
                    b.regionFlags.put(flag, "");
                } else if (!group.equals("")) { // group flag

                    if (flag.getRegionGroupFlag() == null) {
                        OdisseyProtecciones.getPluginLogger().warning(String.format(
                                "La flag \"%s\" no admite grupos; se omite.", flagName));
                        continue;
                    }
                    RegionGroup rGroup = flag.getRegionGroupFlag().detectValue(group);
                    if (rGroup == null) {
                        OdisseyProtecciones.getPluginLogger().severe(String.format("Error al interpretar la flag \"%s\", el valor del grupo no es valido!", flagraw));
                        continue;
                    }

                    b.regionFlags.put(flag, flag.parseInput(fc)); // add flag
                    b.regionFlags.put(flag.getRegionGroupFlag(), rGroup); // apply group

                } else { // normal flag
                    b.regionFlags.put(flag, flag.parseInput(fc));
                }

            } catch (Exception e) {
                OdisseyProtecciones.getPluginLogger().warning("Error al interpretar la flag: " + split[0] + "\nError: ");
                e.printStackTrace();
            }
        }
    }    private static List<String> normalizeLegacyDefaultFlag(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        String[] parts = raw.trim().split("\\s+");
        int flagIndex = parts[0].equals("-g") ? 2 : (parts[0].equals("-e") ? 1 : 0);
        if (flagIndex >= parts.length) return Collections.singletonList(raw);

        List<String> replacements;
        switch (parts[flagIndex].toLowerCase(Locale.ROOT)) {
            case "liquid-flow":
                replacements = Arrays.asList("water-flow", "lava-flow");
                break;
            case "invincibility":
                replacements = Collections.singletonList("invincible");
                break;
            case "trample-blocks":
                replacements = Collections.singletonList("block-trampling");
                break;
            default:
                return Collections.singletonList(raw);
        }

        List<String> normalized = new ArrayList<>(replacements.size());
        for (String replacement : replacements) {
            String[] copy = parts.clone();
            copy[flagIndex] = replacement;
            normalized.add(String.join(" ", copy));
        }
        return normalized;
    }


}

