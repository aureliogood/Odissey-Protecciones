package es.odissey.protecciones.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.MemberPermission;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSPlayer;
import es.odissey.protecciones.PSConfig;
import es.odissey.protecciones.PSProtectBlock;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.utils.Chat;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.text.DecimalFormat;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central manager of the inventory based menu system. Loads the configurable menu files,
 * opens menus for players, builds dynamic content and dispatches button actions.
 */
public class MenuManager {

    private static MenuManager instance;

    private static final String[] BUNDLED_MENUS = {
            "main.yml", "protections.yml", "manage.yml", "members.yml",
            "player-selector.yml", "homes.yml", "confirm.yml",
            "shop.yml", "shop-refund.yml", "shop-refund-confirm.yml",
            "permissions.yml", "permissions-member.yml"
    };

    private final Map<String, MenuDefinition> definitions = new HashMap<>();
    private final Map<UUID, Menu> openMenus = new HashMap<>();
    private final Map<UUID, Consumer<String>> pendingInput = new ConcurrentHashMap<>();

    public static MenuManager getInstance() {
        if (instance == null) instance = new MenuManager();
        return instance;
    }

    public static void playSound(Player p, String name) {
        if (p == null || name == null || name.isEmpty()) return;
        PSConfig conf = OdisseyProtecciones.getInstance().getConfigOptions();
        if (conf != null && conf.menuSounds != null && !conf.menuSounds) return;
        String key = name.toLowerCase(Locale.ROOT).replace('_', '.');
        try {
            p.playSound(p.getLocation(), key, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    public boolean isEnabled() {
        return OdisseyProtecciones.getInstance().getConfigOptions().menuEnabled == null
                || OdisseyProtecciones.getInstance().getConfigOptions().menuEnabled;
    }

    public boolean openOnCommand() {
        return OdisseyProtecciones.getInstance().getConfigOptions().menuOpenOnCommand == null
                || OdisseyProtecciones.getInstance().getConfigOptions().menuOpenOnCommand;
    }

    public boolean shopEnabled() {
        return OdisseyProtecciones.getInstance().getConfigOptions().shopEnabled == null
                || OdisseyProtecciones.getInstance().getConfigOptions().shopEnabled;
    }

    public boolean refundEnabled() {
        return OdisseyProtecciones.getInstance().getConfigOptions().refundEnabled == null
                || OdisseyProtecciones.getInstance().getConfigOptions().refundEnabled;
    }

    public double shopRefundPercentage() {
        Double v = OdisseyProtecciones.getInstance().getConfigOptions().shopRefundPercentage;
        if (v == null) return 50.0;
        return Math.max(0.0, Math.min(100.0, v));
    }

    // ~~~~~~~~~~~~~~~~~ Loading ~~~~~~~~~~~~~~~~~

    public void load() {
        definitions.clear();
        File folder = new File(OdisseyProtecciones.getInstance().getDataFolder(), "menu");
        if (!folder.exists()) folder.mkdirs();

        for (String bundled : BUNDLED_MENUS) {
            File target = new File(folder, bundled);
            if (!target.exists()) {
                try {
                    Files.copy(OdisseyProtecciones.getInstance().getResource("menu/" + bundled),
                            target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    OdisseyProtecciones.getPluginLogger().warning("No se pudo crear el menu " + bundled + ": " + e.getMessage());
                }
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            try {
                migrateBundledLayout(id, file);
                definitions.put(id, new MenuDefinition(id, file));
            } catch (Exception e) {
                OdisseyProtecciones.getPluginLogger().warning("Error cargando el menu " + id + ": " + e.getMessage());
            }
        }
        OdisseyProtecciones.getPluginLogger().info("Cargados " + definitions.size() + " menus.");
    }

    /** Migrates bundled menu layouts and applies the shared title and typography style. */
    private void migrateBundledLayout(String id, File file) {
        if (!Arrays.asList("main", "confirm", "homes", "manage", "members", "permissions",
                "permissions-member", "player-selector", "protections", "shop", "shop-refund",
                "shop-refund-confirm").contains(id)) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        int version = yml.getInt("layout-version", 0);
        if (version >= 4) return;

        if (version < 2 && id.equals("manage")) {
            Map<String, Integer> slots = new LinkedHashMap<>();
            slots.put("home", 20);
            slots.put("block", 21);
            slots.put("members", 22);
            slots.put("flags", 23);
            slots.put("rename", 24);
            slots.put("hide", 25);
            slots.put("permissions", 26);
            slots.put("delete", 40);
            slots.put("back", 48);
            slots.put("close", 50);
            for (Map.Entry<String, Integer> entry : slots.entrySet()) {
                if (yml.isConfigurationSection("items." + entry.getKey())) {
                    yml.set("items." + entry.getKey() + ".slot", entry.getValue());
                }
            }
        } else if (version < 2 && id.equals("protections")) {
            if (!yml.isConfigurationSection("items.header")) {
                yml.set("items.header.material", "COMPASS");
                yml.set("items.header.name", "&bMis protecciones");
                yml.set("items.header.lore", Arrays.asList(
                        "&7Tus terrenos protegidos", "&8Selecciona uno para administrarlo."));
            }
            yml.set("items.header.slot", 4);
            Map<String, Integer> slots = new LinkedHashMap<>();
            slots.put("previous", 45);
            slots.put("back", 48);
            slots.put("close", 50);
            slots.put("next", 53);
            for (Map.Entry<String, Integer> entry : slots.entrySet()) {
                if (yml.isConfigurationSection("items." + entry.getKey())) {
                    yml.set("items." + entry.getKey() + ".slot", entry.getValue());
                }
            }
        }

        String title = switch (id) {
            case "main" -> "&7» Menú Principal «";
            case "confirm" -> "&7» Confirmar acción «";
            case "homes" -> "&7» Mis homes «";
            case "manage" -> "&7» %region% «";
            case "members" -> "&7» Miembros de %region% «";
            case "permissions" -> "&7» Permisos de %region% «";
            case "permissions-member" -> "&7» Permisos de %player% «";
            case "player-selector" -> "&7» Seleccionar jugador «";
            case "protections" -> "&7» Mis protecciones «";
            case "shop" -> "&7» Tienda «";
            case "shop-refund" -> "&7» Reembolsos «";
            case "shop-refund-confirm" -> "&7» Confirmar reembolso «";
            default -> yml.getString("title", "&7» Menú «");
        };
        yml.set("title", title);

        for (String path : new ArrayList<>(yml.getKeys(true))) {
            boolean menuText = path.equals("title") || (path.startsWith("items.") &&
                    (path.endsWith(".name") || path.endsWith(".lore") || path.contains(".lore.")));
            if (!menuText) continue;
            Object value = yml.get(path);
            if (value instanceof String) {
                yml.set(path, normalizeRadiusLore(stripBoldFormatting((String) value)));
            } else if (value instanceof List<?>) {
                List<String> cleaned = new ArrayList<>();
                for (Object line : (List<?>) value) {
                    cleaned.add(line == null ? "" : normalizeRadiusLore(stripBoldFormatting(String.valueOf(line))));
                }
                yml.set(path, cleaned);
            }
        }
        yml.set("layout-version", 4);
        try {
            yml.save(file);
        } catch (Exception e) {
            OdisseyProtecciones.getPluginLogger().warning(
                    "No se pudo actualizar el estilo del menu " + id + ": " + e.getMessage());
        }
    }

    private static String normalizeRadiusLore(String text) {
        if (text.contains("Radio:") && text.contains("%size_x%") && text.contains("%size_z%")) {
            return text.substring(0, text.indexOf("%size_x%")) + "%radius%";
        }
        return text;
    }

    private static String stripBoldFormatting(String text) {
        return text.replace("&l", "").replace("&L", "")
                .replace("§l", "").replace("§L", "");
    }
    // ~~~~~~~~~~~~~~~~~ Opening ~~~~~~~~~~~~~~~~~

    public void openMain(Player p) {
        open(p, "main", null, null, null);
    }

    public Menu open(Player p, String id, Menu parent, PSRegion region, String role) {
        MenuDefinition def = definitions.get(id);
        if (def == null) {
            Chat.enviar(p, PSL.MENU_NOT_FOUND.format(id));
            return null;
        }
        Menu menu = new Menu(p, def);
        menu.parent = parent;
        menu.region = region;
        menu.role = role;
        menu.refreshCallback = this::rebuild;
        rebuild(menu);
        menu.open();
        return menu;
    }

    private void rebuild(Menu menu) {
        if (menu.region != null) {
            menu.setPlaceholders(regionPlaceholders(menu.region));
        }
        buildStatic(menu);
        buildDynamic(menu);
    }

    private void buildStatic(Menu menu) {
        for (MenuButton def : menu.definition.buttons.values()) {
            ItemStack item = buildItem(def, menu);
            Menu.ButtonInstance instance = new Menu.ButtonInstance(def.slot, item, resolveAction(def.action, menu));
            instance.leftAction = resolveAction(def.leftAction, menu);
            instance.rightAction = resolveAction(def.rightAction, menu);
            instance.permission = def.permission;
            instance.sound = def.sound;
            instance.dynamic = false;
            menu.addButton(instance);
        }
    }

    private ItemStack buildItem(MenuButton def, Menu menu) {
        return buildItem(def, menu, null);
    }

    private ItemStack buildItem(MenuButton def, Menu menu, String playerName) {
        Material material = Material.matchMaterial(menu.resolve(def.material));
        if (material == null) material = Material.STONE;
        ItemBuilder builder = new ItemBuilder(material, def.amount)
                .name(replaceName(menu.resolve(def.name), playerName))
                .lore(replaceLore(resolveLore(def.loreOrEmpty(), menu), playerName))
                .glow(def.glow)
                .modelData(def.modelData);
        String headOwner = def.headOwner;
        if (headOwner != null) {
            headOwner = (playerName != null && headOwner.contains("%player%")) ? playerName : menu.resolve(headOwner);
        }
        if (material == Material.PLAYER_HEAD) {
            builder.skullOwner(headOwner != null ? headOwner : playerName);
        }
        return builder.build();
    }

    private List<String> resolveLore(List<String> lore, Menu menu) {
        List<String> out = new ArrayList<>();
        for (String line : lore) out.add(menu.resolve(line));
        return out;
    }

    private String resolveAction(String action, Menu menu) {
        return action == null ? null : menu.resolve(action);
    }

    private String replaceName(String name, String playerName) {
        if (name == null || playerName == null) return name;
        return name.replace("%player%", playerName);
    }

    private List<String> replaceLore(List<String> lore, String playerName) {
        if (playerName == null) return lore;
        List<String> out = new ArrayList<>();
        for (String line : lore) out.add(line.replace("%player%", playerName));
        return out;
    }

    private void buildDynamic(Menu menu) {
        String id = menu.definition.id;
        if (id.equals("protections")) buildProtections(menu);
        else if (id.equals("members")) buildMembers(menu);
        else if (id.equals("permissions")) buildPermissions(menu);
        else if (id.equals("permissions-member")) buildPermToggles(menu);
        else if (id.equals("player-selector")) buildPlayerSelector(menu);
        else if (id.equals("homes")) buildHomes(menu);
        else if (id.equals("shop")) buildShop(menu);
        else if (id.equals("shop-refund")) buildRefunds(menu);
    }

    // ~~~~~~~~~~~~~~~~~ Dynamic content ~~~~~~~~~~~~~~~~~

    private List<Integer> contentSlots(Menu menu) {
        if (!menu.definition.contentSlots.isEmpty()) return menu.definition.contentSlots;
        List<Integer> slots = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (Menu.ButtonInstance b : menu.buttons) used.add(b.slot);
        for (int i = 0; i < menu.definition.size; i++) {
            if (!used.contains(i)) slots.add(i);
        }
        return slots;
    }

    private void buildProtections(Menu menu) {
        List<PSRegion> regions = getPlayerRegions(menu.player);
        List<Integer> slots = contentSlots(menu);
        MenuButton template = menu.definition.getTemplate("entry");
        if (template == null) return;

        if (slots.isEmpty()) return;
        int perPage = slots.size();
        int maxPage = Math.max(0, (regions.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= regions.size()) break;
            PSRegion region = regions.get(index);
            Menu resolver = new Menu(menu.player, menu.definition);
            resolver.region = region;
            resolver.setPlaceholders(regionPlaceholders(region));
            ItemStack item = buildItem(template, resolver);
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "open:manage");
            instance.payload = region;
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private void buildMembers(Menu menu) {
        PSRegion region = menu.region;
        if (region == null) return;
        List<Integer> slots = contentSlots(menu);
        MenuButton ownerTemplate = menu.definition.getTemplate("owner");
        MenuButton memberTemplate = menu.definition.getTemplate("member");
        int i = 0;

        if (ownerTemplate != null) {
            for (UUID uuid : region.getOwners()) {
                if (i >= slots.size()) break;
                menu.addButton(memberButton(slots.get(i), ownerTemplate, menu, uuid));
                i++;
            }
        }
        if (memberTemplate != null) {
            for (UUID uuid : region.getMembers()) {
                if (i >= slots.size()) break;
                menu.addButton(memberButton(slots.get(i), memberTemplate, menu, uuid));
                i++;
            }
        }
        applyPagination(menu, false, false);
    }

    private Menu.ButtonInstance memberButton(int slot, MenuButton template, Menu menu, UUID uuid) {
        ItemStack item = buildItem(template, menu, nameOf(uuid));
        Menu.ButtonInstance instance = new Menu.ButtonInstance(slot, item, "member:remove");
        instance.payload = uuid;
        return instance;
    }

    private void buildPermissions(Menu menu) {
        PSRegion region = menu.region;
        if (region == null) return;
        List<Integer> slots = contentSlots(menu);
        MenuButton template = menu.definition.getTemplate("member");
        if (template == null || slots.isEmpty()) return;

        List<UUID> members = new ArrayList<>(region.getMembers());
        int perPage = slots.size();
        int maxPage = Math.max(0, (members.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= members.size()) break;
            UUID uuid = members.get(index);
            ItemStack item = buildItem(template, menu, nameOf(uuid));
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "perm:select");
            instance.payload = uuid;
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private void buildPermToggles(Menu menu) {
        PSRegion region = menu.region;
        if (region == null || !(menu.data instanceof UUID)) return;
        UUID target = (UUID) menu.data;
        List<Integer> slots = contentSlots(menu);
        MenuButton template = menu.definition.getTemplate("perm");
        if (template == null || slots.isEmpty()) return;

        MemberPermission[] perms = MemberPermission.values();
        for (int i = 0; i < perms.length && i < slots.size(); i++) {
            MemberPermission perm = perms[i];
            boolean on = region.hasMemberPermission(target, perm);
            Menu resolver = new Menu(menu.player, menu.definition);
            resolver.region = region;
            resolver.setPlaceholders(regionPlaceholders(region));
            resolver.placeholders.put("%player%", nameOf(target));
            resolver.placeholders.put("%perm_icon%", perm.icon.name());
            resolver.placeholders.put("%perm_name%", perm.display);
            resolver.placeholders.put("%perm_state%", on ? "&aActivado" : "&cDesactivado");
            ItemStack item = buildItem(template, resolver, nameOf(target));
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "perm:toggle:" + perm.id);
            instance.payload = perm;
            menu.addButton(instance);
        }
        applyPagination(menu, false, false);
    }

    private void buildPlayerSelector(Menu menu) {
        PSRegion region = menu.region;
        if (region == null) return;
        List<Integer> slots = contentSlots(menu);
        MenuButton template = menu.definition.getTemplate("player");
        if (template == null) return;

        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (region.isOwner(online.getUniqueId()) || region.isMember(online.getUniqueId())) continue;
            candidates.add(online);
        }

        if (slots.isEmpty()) return;
        int perPage = slots.size();
        int maxPage = Math.max(0, (candidates.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= candidates.size()) break;
            Player target = candidates.get(index);
            ItemStack item = buildItem(template, menu, target.getName());
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "member:select");
            instance.payload = target.getUniqueId();
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private void buildHomes(Menu menu) {
        List<PSRegion> homes = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            for (PSRegion r : PSPlayer.fromPlayer(menu.player).getHomes(w)) {
                if (r.getHome() != null) homes.add(r);
            }
        }
        List<Integer> slots = contentSlots(menu);
        MenuButton template = menu.definition.getTemplate("region");
        if (template == null) return;

        if (slots.isEmpty()) return;
        int perPage = slots.size();
        int maxPage = Math.max(0, (homes.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= homes.size()) break;
            PSRegion region = homes.get(index);
            Menu resolver = new Menu(menu.player, menu.definition);
            resolver.region = region;
            resolver.setPlaceholders(regionPlaceholders(region));
            Location home = region.getHome();
            resolver.placeholders.put("%home%", home == null ? "-"
                    : String.format("%.0f, %.0f, %.0f", home.getX(), home.getY(), home.getZ()));
            ItemStack item = buildItem(template, resolver);
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "home:tp");
            instance.leftAction = "home:tp";
            instance.rightAction = "home:set";
            instance.payload = region;
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private void buildShop(Menu menu) {
        MenuButton template = menu.definition.getTemplate("block");
        if (template == null) return;
        List<PSProtectBlock> blocks = new ArrayList<>();
        for (PSProtectBlock b : OdisseyProtecciones.getInstance().getConfiguredBlocks()) {
            if (Boolean.FALSE.equals(b.shopEnabled)) continue; // tienda desactivada para este bloque
            if (b.permission != null && !b.permission.isEmpty() && !menu.player.hasPermission(b.permission)) continue;
            if (b.preventPsGet && !menu.player.hasPermission("odisseyprotecciones.admin")) continue;
            blocks.add(b);
        }

        List<Integer> slots = contentSlots(menu);
        if (slots.isEmpty()) return;
        int perPage = slots.size();
        int maxPage = Math.max(0, (blocks.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= blocks.size()) break;
            PSProtectBlock b = blocks.get(index);
            Menu resolver = new Menu(menu.player, menu.definition);
            resolver.setPlaceholders(shopPlaceholders(b));
            ItemStack item = buildShopItem(template, resolver, b);
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "shop:buy");
            instance.payload = b;
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private void buildRefunds(Menu menu) {
        MenuButton template = menu.definition.getTemplate("protection");
        if (template == null) return;

        List<RefundEntry> entries = new ArrayList<>();
        for (PSRegion r : getPlayerRegions(menu.player)) {
            PSProtectBlock b = r.getTypeOptions();
            if (b == null || b.price <= 0 || Boolean.FALSE.equals(b.refundEnabled)) continue;
            entries.add(new RefundEntry(r, -1, b, b.price));
        }
        ItemStack[] contents = menu.player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            PSProtectBlock b = OdisseyProtecciones.getBlockOptions(it);
            if (b == null || b.price <= 0 || Boolean.FALSE.equals(b.refundEnabled)) continue;
            entries.add(new RefundEntry(null, i, b, b.price));
        }

        // hide the "empty" placeholder when there is content
        if (!entries.isEmpty()) menu.buttons.removeIf(btn -> "noop".equals(btn.action));

        List<Integer> slots = contentSlots(menu);
        if (slots.isEmpty()) return;
        int perPage = slots.size();
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        if (menu.page > maxPage) menu.page = maxPage;

        for (int i = 0; i < perPage; i++) {
            int index = menu.page * perPage + i;
            if (index >= entries.size()) break;
            RefundEntry entry = entries.get(index);
            Menu resolver = new Menu(menu.player, menu.definition);
            resolver.setPlaceholders(refundPlaceholders(entry));
            ItemStack item = buildItem(template, resolver);
            Menu.ButtonInstance instance = new Menu.ButtonInstance(slots.get(i), item, "shop:refund");
            instance.payload = entry;
            menu.addButton(instance);
        }
        applyPagination(menu, menu.page > 0, menu.page < maxPage);
    }

    private ItemStack buildShopItem(MenuButton template, Menu resolver, PSProtectBlock b) {
        ItemStack base = b.createItem();
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            if (template.name != null && !template.name.isEmpty()) {
                meta.setDisplayName(ColorUtil.color(resolver.resolve(template.name)));
            }
            if (template.lore != null && !template.lore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : template.lore) lore.add(ColorUtil.color(resolver.resolve(line)));
                meta.setLore(lore);
            }
            if (template.glow) meta.setEnchantmentGlintOverride(true);
            base.setItemMeta(meta);
        }
        return base;
    }

    private Map<String, String> shopPlaceholders(PSProtectBlock b) {
        Map<String, String> ph = new LinkedHashMap<>();
        ph.put("%alias%", b.alias);
        ph.put("%type%", b.type);
        ph.put("%description%", b.description == null ? "" : b.description);
        ph.put("%price%", fmt(b.price));
        ph.put("%xradius%", String.valueOf(b.xRadius));
        ph.put("%yradius%", String.valueOf(b.yRadius));
        ph.put("%zradius%", String.valueOf(b.zRadius));
        return ph;
    }

    private Map<String, String> refundPlaceholders(RefundEntry e) {
        Map<String, String> ph = new LinkedHashMap<>();
        double pct = shopRefundPercentage();
        ph.put("%percentage%", fmt(pct));
        ph.put("%price%", fmt(e.original));
        ph.put("%amount%", fmt(e.original * pct / 100.0));
        ph.put("%type%", e.block.alias);
        ph.put("%protection_name%", e.block.alias);
        if (e.region != null) {
            try {
                ph.putAll(regionPlaceholders(e.region));
            } catch (Exception ignored) {
            }
            ph.put("%region%", displayName(e.region));
            ph.put("%source%", "Region");
        } else {
            ph.put("%region%", e.block.alias + " (Inventario)");
            ph.put("%id%", "-");
            ph.put("%world%", "Inventario");
            ph.put("%x%", "-");
            ph.put("%y%", "-");
            ph.put("%z%", "-");
            ph.put("%source%", "Inventario");
            ph.put("%inventory_slot%", String.valueOf(e.inventorySlot));
        }
        return ph;
    }

    private void applyPagination(Menu menu, boolean hasPrev, boolean hasNext) {
        if (!hasPrev) removeButtonsByAction(menu, "page:prev");
        if (!hasNext) removeButtonsByAction(menu, "page:next");
    }

    private void removeButtonsByAction(Menu menu, String action) {
        menu.buttons.removeIf(b -> action.equals(b.action) || action.equals(b.leftAction) || action.equals(b.rightAction));
    }

    // ~~~~~~~~~~~~~~~~~ Click handling ~~~~~~~~~~~~~~~~~

    public boolean onClick(Player p, int rawSlot, boolean rightClick) {
        Menu menu = openMenus.get(p.getUniqueId());
        if (menu == null) return false;
        if (rawSlot < 0 || rawSlot >= menu.definition.size) return true;

        Menu.ButtonInstance button = menu.getButton(rawSlot);
        if (button == null) return true;

        if (button.permission != null && !button.permission.isEmpty() && !p.hasPermission(button.permission)) {
            Chat.enviar(p, PSL.MENU_NO_PERMISSION.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return true;
        }
        if (button.sound != null) playSound(p, button.sound);
        executeAction(menu, button, button.actionFor(rightClick));
        return true;
    }

    private void executeAction(Menu menu, Menu.ButtonInstance button, String action) {
        if (action == null) return;
        Player p = menu.player;

        if (action.equals("close")) {
            p.closeInventory();
            return;
        }
        if (action.equals("back")) {
            if (menu.parent != null) menu.parent.open();
            else p.closeInventory();
            return;
        }
        if (action.equals("page:prev")) {
            menu.page = Math.max(0, menu.page - 1);
            menu.refresh();
            return;
        }
        if (action.equals("page:next")) {
            menu.page++;
            menu.refresh();
            return;
        }
        if (action.startsWith("cmd:")) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () ->
                    Bukkit.dispatchCommand(p, action.substring(4).trim()));
            return;
        }
        if (action.startsWith("open:")) {
            String target = action.substring(5);
            if (target.equals("manage-current")) {
                PSRegion current = PSRegion.fromLocationGroup(p.getLocation());
                if (current == null) {
                    Chat.enviar(p, PSL.MENU_NOT_IN_REGION.msg());
                    return;
                }
                if (!canAccess(p, current)) {
                    Chat.enviar(p, PSL.MENU_NO_ACCESS.msg());
                    playSound(p, "ENTITY_VILLAGER_NO");
                    return;
                }
                open(p, "manage", menu, current, null);
                return;
            }
            PSRegion region = (button.payload instanceof PSRegion) ? (PSRegion) button.payload : menu.region;
            if (region != null && !canAccess(p, region)) {
                Chat.enviar(p, PSL.MENU_NO_ACCESS.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            open(p, target, menu, region, menu.role);
            return;
        }
        if (action.equals("home:tp")) {
            tpHome(p, regionOf(button, menu));
            return;
        }
        if (action.equals("home:set")) {
            setHome(p, regionOf(button, menu), menu);
            return;
        }
        if (action.equals("block:tp")) {
            tpBlock(p, menu.region);
            return;
        }
        if (action.equals("hide:toggle")) {
            toggleHide(p, menu.region, menu);
            return;
        }
        if (action.equals("rename")) {
            requestRename(p, menu.region, menu);
            return;
        }
        if (action.equals("delete")) {
            openConfirm(menu, () -> deleteRegion(p, menu.region, menu));
            return;
        }
        if (action.equals("confirm")) {
            Runnable runnable = menu.pendingConfirm;
            p.closeInventory();
            if (runnable != null) runnable.run();
            return;
        }
        if (action.equals("cancel")) {
            if (menu.parent != null) menu.parent.open();
            else p.closeInventory();
            return;
        }
        if (action.equals("flags")) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () ->
                    Bukkit.dispatchCommand(p, OdisseyProtecciones.getInstance().getConfigOptions().base_command + " flag"));
            return;
        }
        if (action.equals("member:add-member")) {
            open(p, "player-selector", menu, menu.region, "member");
            return;
        }
        if (action.equals("member:add-owner")) {
            open(p, "player-selector", menu, menu.region, "owner");
            return;
        }
        if (action.equals("member:select")) {
            addMember(menu, button.payload instanceof UUID ? (UUID) button.payload : null);
            return;
        }
        if (action.equals("member:remove")) {
            UUID target = button.payload instanceof UUID ? (UUID) button.payload : null;
            openConfirm(menu, () -> removeMember(menu, target, menu.parent));
            return;
        }
        if (action.equals("perm:select")) {
            if (!isOwnerOrAdmin(p, menu.region)) {
                PSL.msg(p, PSL.NO_PERMISSION_MEMBERS.msg());
                return;
            }
            UUID uuid = button.payload instanceof UUID ? (UUID) button.payload : null;
            if (uuid != null && menu.region != null) {
                Menu editor = open(p, "permissions-member", menu, menu.region, null);
                if (editor != null) {
                    editor.data = uuid;
                    editor.refresh();
                }
            }
            return;
        }
        if (action.startsWith("perm:toggle:")) {
            if (!isOwnerOrAdmin(p, menu.region) || menu.region == null || !(menu.data instanceof UUID)) return;
            MemberPermission perm = MemberPermission.byId(action.substring("perm:toggle:".length()));
            if (perm == null) return;
            UUID target = (UUID) menu.data;
            menu.region.setMemberPermission(target, perm, !menu.region.hasMemberPermission(target, perm));
            guardar(menu.region);
            playSound(p, "UI_BUTTON_CLICK");
            menu.refresh();
            return;
        }
        if (action.equals("perm:all") || action.equals("perm:none")) {
            if (!isOwnerOrAdmin(p, menu.region) || menu.region == null || !(menu.data instanceof UUID)) return;
            UUID target = (UUID) menu.data;
            menu.region.setMemberPermissionMask(target, action.equals("perm:all") ? MemberPermission.all() : 0);
            guardar(menu.region);
            playSound(p, "UI_BUTTON_CLICK");
            menu.refresh();
            return;
        }
        if (action.equals("shop:buy")) {
            if (button.payload instanceof PSProtectBlock) buyBlock(p, (PSProtectBlock) button.payload);
            return;
        }
        if (action.equals("shop:refund")) {
            if (!(button.payload instanceof RefundEntry)) return;
            RefundEntry entry = (RefundEntry) button.payload;
            Menu confirm = openCustom(p, "shop-refund-confirm", menu, refundPlaceholders(entry));
            if (confirm != null) {
                confirm.pendingConfirm = () -> {
                    doRefund(p, entry);
                    open(p, "shop-refund", menu.parent, null, null);
                };
            }
            return;
        }
    }

    private Menu openCustom(Player p, String id, Menu parent, Map<String, String> placeholders) {
        MenuDefinition def = definitions.get(id);
        if (def == null) {
            Chat.enviar(p, PSL.MENU_NOT_FOUND.format(id));
            return null;
        }
        Menu menu = new Menu(p, def);
        menu.parent = parent;
        if (placeholders != null) menu.placeholders.putAll(placeholders);
        menu.refreshCallback = this::rebuild;
        rebuild(menu);
        menu.open();
        return menu;
    }

    private PSRegion regionOf(Menu.ButtonInstance button, Menu menu) {
        return button.payload instanceof PSRegion ? (PSRegion) button.payload : menu.region;
    }

    private void openConfirm(Menu parent, Runnable action) {
        Menu confirm = open(parent.player, "confirm", parent, parent.region, null);
        if (confirm != null) confirm.pendingConfirm = action;
    }

    // ~~~~~~~~~~~~~~~~~ Actions ~~~~~~~~~~~~~~~~~

    private void deleteRegion(Player p, PSRegion region, Menu menu) {
        if (region == null) return;
        if (!region.isOwner(p.getUniqueId()) && !p.hasPermission("odisseyprotecciones.admin")) {
            Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
            return;
        }
        if (lacks(p, "odisseyprotecciones.destroy", "odisseyprotecciones.unclaim")) {
            PSL.msg(p, PSL.NO_PERMISSION_DESTROY.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        boolean success = region.deleteRegion(true, p);
        if (success) {
            Chat.enviar(p, PSL.MENU_REGION_DELETED.format(displayName(region)));
            playSound(p, "ENTITY_PLAYER_LEVELUP");
            guardar(region);
            open(p, "protections", null, null, null);
        } else {
            Chat.enviar(p, PSL.MENU_REGION_DELETE_FAILED.msg());
        }
    }

    private void toggleHide(Player p, PSRegion region, Menu menu) {
        if (region == null) return;
        if (!region.isOwner(p.getUniqueId()) && !p.hasPermission("odisseyprotecciones.admin")) {
            Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
            return;
        }
        if (lacks(p, "odisseyprotecciones.hide", "odisseyprotecciones.unhide")) {
            PSL.msg(p, PSL.NO_PERMISSION_HIDE.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        region.toggleHide();
        guardar(region);
        Chat.enviar(p, PSL.MENU_HIDE_TOGGLED.format(region.isHidden() ? "oculto" : "visible"));
        open(p, "manage", menu.parent, region, null);
    }

    private void requestRename(Player p, PSRegion region, Menu menu) {
        if (region == null) return;
        if (!isOwnerOrAdmin(p, region)) {
            Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        if (lacks(p, "odisseyprotecciones.name")) {
            PSL.msg(p, PSL.NO_PERMISSION_NAME.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        p.closeInventory();
        Chat.enviar(p, PSL.MENU_RENAME_PROMPT.msg());
        requestInput(p, input -> {
            if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) {
                Chat.enviar(p, PSL.MENU_RENAME_CANCELLED.msg());
                return;
            }
            if (input.length() > 32) {
                Chat.enviar(p, PSL.MENU_RENAME_TOO_LONG.msg());
                return;
            }
            region.setName(input);
            guardar(region);
            Chat.enviar(p, PSL.MENU_RENAME_SUCCESS.format(input));
            open(p, "manage", menu.parent, region, null);
        });
    }

    private void addMember(Menu menu, UUID target) {
        if (target == null || menu.region == null) return;
        Player p = menu.player;
        if (!isOwnerOrAdmin(p, menu.region)) {
            Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            open(p, "manage", menu.parent, menu.region, null);
            return;
        }
        if ("owner".equals(menu.role) ? lacks(p, "odisseyprotecciones.owners") : lacks(p, "odisseyprotecciones.members")) {
            PSL.msg(p, "owner".equals(menu.role) ? PSL.NO_PERMISSION_OWNERS.msg() : PSL.NO_PERMISSION_MEMBERS.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            open(p, "manage", menu.parent, menu.region, null);
            return;
        }
        if ("owner".equals(menu.role)) {
            menu.region.addOwner(target);
            Chat.enviar(p, PSL.MENU_OWNER_ADDED.format(nameOf(target)));
        } else {
            menu.region.addMember(target);
            Chat.enviar(p, PSL.MENU_MEMBER_ADDED.format(nameOf(target)));
        }
        guardar(menu.region);
        open(p, "members", menu.parent, menu.region, null);
    }

    private void removeMember(Menu menu, UUID target, Menu after) {
        if (target == null || menu.region == null) return;
        Player p = menu.player;
        if (!isOwnerOrAdmin(p, menu.region)) {
            Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            open(p, "manage", menu.parent, menu.region, null);
            return;
        }
        if (menu.region.isOwner(target) ? lacks(p, "odisseyprotecciones.owners") : lacks(p, "odisseyprotecciones.members")) {
            PSL.msg(p, menu.region.isOwner(target) ? PSL.NO_PERMISSION_OWNERS.msg() : PSL.NO_PERMISSION_MEMBERS.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            open(p, "manage", menu.parent, menu.region, null);
            return;
        }
        if (menu.region.isOwner(target)) {
            if (menu.region.getOwners().size() <= 1) {
                Chat.enviar(p, PSL.MENU_LAST_OWNER.msg());
                open(p, "members", after, menu.region, null);
                return;
            }
            menu.region.removeOwner(target);
        } else {
            menu.region.removeMember(target);
        }
        menu.region.clearMemberPermissions(target);
        guardar(menu.region);
        Chat.enviar(p, PSL.MENU_MEMBER_REMOVED.format(nameOf(target)));
        open(p, "members", after, menu.region, null);
    }

    private void tpHome(Player p, PSRegion region) {
        if (region == null) return;
        if (!canAccess(p, region)) {
            Chat.enviar(p, PSL.MENU_NO_ACCESS.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        if (lacks(p, "odisseyprotecciones.home")) {
            PSL.msg(p, PSL.NO_PERMISSION_HOME.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        Location home = region.getHome();
        if (home == null) {
            Chat.enviar(p, PSL.MENU_NO_HOME.msg());
            return;
        }
        p.closeInventory();
        p.teleport(home);
        Chat.enviar(p, PSL.MENU_TP_HOME.format(displayName(region)));
    }

    private void setHome(Player p, PSRegion region, Menu menu) {
        if (region == null) return;
        if (!region.isOwner(p.getUniqueId()) && !region.isMember(p.getUniqueId()) && !p.hasPermission("odisseyprotecciones.admin")) {
            Chat.enviar(p, PSL.MENU_NOT_MEMBER.msg());
            return;
        }
        if (lacks(p, "odisseyprotecciones.sethome")) {
            PSL.msg(p, PSL.NO_PERMISSION_SETHOME.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        p.closeInventory();
        Location loc = p.getLocation();
        region.setHome(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        guardar(region);
        Chat.enviar(p, PSL.MENU_HOME_SET.msg());
        if (menu.definition.id.equals("homes")) open(p, "homes", menu.parent, null, null);
    }

    private void tpBlock(Player p, PSRegion region) {
        if (region == null) return;
        if (!canAccess(p, region)) {
            Chat.enviar(p, PSL.MENU_NO_ACCESS.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        if (lacks(p, "odisseyprotecciones.tp")) {
            PSL.msg(p, PSL.NO_PERMISSION_TP.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        Location loc = region.getProtectBlock().getLocation().add(0.5, 1, 0.5);
        p.closeInventory();
        p.teleport(loc);
        Chat.enviar(p, PSL.MENU_TP_BLOCK.format(displayName(region)));
    }

    // ~~~~~~~~~~~~~~~~~ Shop actions ~~~~~~~~~~~~~~~~~

    private void buyBlock(Player p, PSProtectBlock b) {
        if (b == null) return;
        if (b.permission != null && !b.permission.isEmpty() && !p.hasPermission(b.permission)) {
            Chat.enviar(p, PSL.MENU_NO_PERMISSION_BUY.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        if (b.preventPsGet && !p.hasPermission("odisseyprotecciones.admin")) {
            Chat.enviar(p, PSL.MENU_CANNOT_BUY.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }

        PSPlayer psp = PSPlayer.fromPlayer(p);
        boolean vault = OdisseyProtecciones.getInstance().isVaultSupportEnabled();
        if (vault && b.price != 0) {
            if (!psp.hasAmount(b.price)) {
                PSL.msg(p, PSL.NOT_ENOUGH_MONEY.msg().replace("%price%", fmt(b.price)));
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            EconomyResponse er = psp.withdrawBalance(b.price);
            if (!er.transactionSuccess()) {
                PSL.msg(p, er.errorMessage);
                return;
            }
        }

        if (!p.getInventory().addItem(b.createItem()).isEmpty()) {
            if (OdisseyProtecciones.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                p.getWorld().dropItem(p.getLocation(), b.createItem());
                PSL.msg(p, PSL.NO_ROOM_DROPPING_ON_FLOOR.msg());
            } else {
                PSL.msg(p, PSL.NO_ROOM_IN_INVENTORY.msg());
                if (vault && b.price != 0) psp.depositBalance(b.price);
            }
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        PSL.msg(p, PSL.GET_GOTTEN.msg());
        playSound(p, "ENTITY_PLAYER_LEVELUP");
    }

    private void doRefund(Player p, RefundEntry entry) {
        if (entry == null) return;
        if (!OdisseyProtecciones.getInstance().isVaultSupportEnabled()) {
            Chat.enviar(p, PSL.MENU_REFUND_NEEDS_VAULT.msg());
            return;
        }
        if (entry.block != null && Boolean.FALSE.equals(entry.block.refundEnabled)) {
            Chat.enviar(p, PSL.MENU_REFUND_FAILED.msg());
            playSound(p, "ENTITY_VILLAGER_NO");
            return;
        }
        double amount = entry.original * shopRefundPercentage() / 100.0;

        if (entry.region != null) {
            PSRegion r = entry.region;
            if (!r.isOwner(p.getUniqueId()) && !p.hasPermission("odisseyprotecciones.admin")) {
                Chat.enviar(p, PSL.MENU_NOT_OWNER.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            // la region pudo desaparecer despues de abrir el menu (/ps unclaim, merge, admin...)
            if (!(r instanceof es.odissey.protecciones.PSMergedRegion)
                    && r.getWGRegionManager().getRegion(r.getId()) == null) {
                Chat.enviar(p, PSL.MENU_REFUND_FAILED.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            if (!r.deleteRegion(true, p)) {
                Chat.enviar(p, PSL.MENU_REFUND_FAILED.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            Chat.enviar(p, PSL.MENU_REFUNDED.format(displayName(r)));
            guardar(r);
        } else if (entry.inventorySlot >= 0) {
            ItemStack it = p.getInventory().getItem(entry.inventorySlot);
            if (it == null) {
                Chat.enviar(p, PSL.MENU_REFUND_FAILED.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            // revalida que el item del slot siga siendo el bloque registrado al abrir el menu
            PSProtectBlock current = OdisseyProtecciones.getBlockOptions(it);
            if (current == null || entry.block == null
                    || !java.util.Objects.equals(current.alias, entry.block.alias)
                    || !java.util.Objects.equals(current.type, entry.block.type)) {
                Chat.enviar(p, PSL.MENU_REFUND_FAILED.msg());
                playSound(p, "ENTITY_VILLAGER_NO");
                return;
            }
            if (it.getAmount() > 1) {
                it.setAmount(it.getAmount() - 1);
            } else {
                p.getInventory().setItem(entry.inventorySlot, null);
            }
        } else {
            return;
        }

        PSPlayer.fromPlayer(p).depositBalance(amount);
        Chat.enviar(p, PSL.MENU_REFUND_RECEIVED.format(fmt(amount)));
        playSound(p, "ENTITY_PLAYER_LEVELUP");
    }

    private String fmt(double value) {
        return new DecimalFormat("#.##").format(value);
    }

    /**
     * A refundable entry: either a placed region or a protection block sitting in the player's inventory.
     */
    public static class RefundEntry {
        public final PSRegion region;
        public final int inventorySlot;
        public final PSProtectBlock block;
        public final double original;

        public RefundEntry(PSRegion region, int inventorySlot, PSProtectBlock block, double original) {
            this.region = region;
            this.inventorySlot = inventorySlot;
            this.block = block;
            this.original = original;
        }
    }

    // ~~~~~~~~~~~~~~~~~ Input handling ~~~~~~~~~~~~~~~~~

    public void requestInput(Player p, Consumer<String> onInput) {
        pendingInput.put(p.getUniqueId(), onInput);
    }

    public boolean isAwaitingInput(Player p) {
        return pendingInput.containsKey(p.getUniqueId());
    }

    public void handleInput(Player p, String message) {
        Consumer<String> consumer = pendingInput.remove(p.getUniqueId());
        if (consumer != null) consumer.accept(message);
    }

    // ~~~~~~~~~~~~~~~~~ Registry ~~~~~~~~~~~~~~~~~

    public void registerOpenMenu(Player p, Menu menu) {
        openMenus.put(p.getUniqueId(), menu);
    }

    public void unregister(Player p) {
        openMenus.remove(p.getUniqueId());
        pendingInput.remove(p.getUniqueId());
    }

    public Menu getOpenMenu(Player p) {
        return openMenus.get(p.getUniqueId());
    }

    // ~~~~~~~~~~~~~~~~~ Helpers ~~~~~~~~~~~~~~~~~

    private List<PSRegion> getPlayerRegions(Player p) {
        Set<String> seen = new HashSet<>();
        List<PSRegion> regions = new ArrayList<>();
        PSPlayer psp = PSPlayer.fromPlayer(p);
        for (World w : Bukkit.getWorlds()) {
            for (PSRegion r : psp.getPSRegions(w, false)) {
                String key = w.getName() + " " + r.getId();
                if (seen.add(key)) regions.add(r);
            }
        }
        return regions;
    }

    public Map<String, String> regionPlaceholders(PSRegion region) {
        Map<String, String> ph = new LinkedHashMap<>();
        ProtectedRegion wr = region.getWGRegion();
        int rx = 0, rz = 0, ry = 0;
        if (wr != null) {
            rx = wr.getMaximumPoint().getX() - wr.getMinimumPoint().getX() + 1;
            rz = wr.getMaximumPoint().getZ() - wr.getMinimumPoint().getZ() + 1;
            ry = wr.getMaximumPoint().getY() - wr.getMinimumPoint().getY() + 1;
        }
        Block protectBlock = region.getProtectBlock();
        Location loc = protectBlock == null ? region.getWorld().getSpawnLocation() : protectBlock.getLocation();
        List<UUID> owners = region.getOwners();
        ph.put("%region%", displayName(region));
        ph.put("%id%", region.getId());
        ph.put("%world%", region.getWorld().getName());
        ph.put("%x%", String.valueOf(loc.getBlockX()));
        ph.put("%y%", String.valueOf(loc.getBlockY()));
        ph.put("%z%", String.valueOf(loc.getBlockZ()));
        PSProtectBlock blockOptions = region.getTypeOptions();
        String radius = blockOptions != null && blockOptions.chunkRadius <= 0
                ? blockOptions.xRadius + "x" + blockOptions.zRadius : rx + "x" + rz;
        ph.put("%radius%", radius);
        ph.put("%size_x%", String.valueOf(rx));
        ph.put("%size_z%", String.valueOf(rz));
        ph.put("%size_y%", String.valueOf(ry));
        ph.put("%owner%", owners.isEmpty() ? "-" : nameOf(owners.get(0)));
        ph.put("%owners%", String.valueOf(owners.size()));
        ph.put("%members%", String.valueOf(region.getMembers().size()));
        ph.put("%hidden%", region.isHidden() ? "Si" : "No");
        ph.put("%status%", statusOf(region));
        ph.put("%price%", region.getPrice() == null ? "-" : String.format("%.2f", region.getPrice()));
        ph.put("%rent_until%", rentUntil(region));
        ph.put("%days_until_removal%", es.odissey.protecciones.utils.AutoRemove.daysUntilRemoval(region));
        return ph;
    }

    private String statusOf(PSRegion region) {
        if (region.forSale()) return "En venta";
        PSRegion.RentStage stage = region.getRentStage();
        if (stage == PSRegion.RentStage.RENTING) return "Alquilada";
        if (stage == PSRegion.RentStage.LOOKING_FOR_TENANT) return "En alquiler";
        return "Protegida";
    }

    private String rentUntil(PSRegion region) {
        Long lastPaid = region.getRentLastPaid();
        String period = region.getRentPeriod();
        if (lastPaid == null || period == null || lastPaid <= 0) return "-";
        try {
            long seconds = es.odissey.protecciones.utils.MiscUtil.parseRentPeriod(period).getSeconds();
            long remaining = lastPaid + seconds - System.currentTimeMillis() / 1000L;
            if (remaining <= 0) return "Vencido";
            long days = remaining / 86400;
            long hours = (remaining % 86400) / 3600;
            long minutes = (remaining % 3600) / 60;
            if (days > 0) return days + "d " + hours + "h";
            if (hours > 0) return hours + "h " + minutes + "m";
            return Math.max(1, minutes) + "m";
        } catch (Exception e) {
            return "-";
        }
    }

    private String displayName(PSRegion region) {
        String name = region.getName();
        if (name != null && !name.isEmpty()) return name;
        List<UUID> owners = region.getOwners();
        if (!owners.isEmpty()) return "Proteccion de " + nameOf(owners.get(0));
        return "Proteccion";
    }

    private String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString().substring(0, 8) : name;
    }

    // true si el jugador es dueno de la region o es admin
    private boolean isOwnerOrAdmin(Player p, PSRegion region) {
        return region != null && (region.isOwner(p.getUniqueId()) || p.hasPermission("odisseyprotecciones.admin"));
    }

    // true si el jugador es dueno, miembro o admin
    public boolean canAccess(Player p, PSRegion region) {
        return region != null && (region.isOwner(p.getUniqueId()) || region.isMember(p.getUniqueId())
                || p.hasPermission("odisseyprotecciones.admin"));
    }

    /**
     * Fuerza el guardado de la region en WorldGuard. Se usa tras acciones de usuario que modifican
     * datos (borrar, renombrar, miembros, permisos, home, ocultar) para no depender solo del autoguardado.
     */
    private void guardar(PSRegion region) {
        if (region == null) return;
        try {
            com.sk89q.worldguard.protection.managers.RegionManager rgm =
                    es.odissey.protecciones.utils.WGUtils.getRegionManagerWithWorld(region.getWorld());
            if (rgm != null) rgm.saveChanges();
        } catch (Exception ignored) {
        }
    }

    // true si al jugador le falta cualquiera de los permisos (admin siempre pasa)
    private boolean lacks(Player p, String... perms) {
        if (p.hasPermission("odisseyprotecciones.admin")) return false;
        for (String perm : perms) {
            if (p.hasPermission(perm)) return false;
        }
        return true;
    }
}
