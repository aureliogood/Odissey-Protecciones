package es.odissey.protecciones.gui;

import es.odissey.protecciones.PSRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Runtime instance of an opened menu.
 */
public class Menu {

    public final Player player;
    public final MenuDefinition definition;
    public final Map<String, String> placeholders = new HashMap<>();
    public final List<ButtonInstance> buttons = new ArrayList<>();

    public Menu parent;
    public PSRegion region;
    public String role;
    public int page;
    public Object data;
    public Runnable pendingConfirm;
    public Consumer<Menu> refreshCallback;

    private Inventory inventory;

    public Menu(Player player, MenuDefinition definition) {
        this.player = player;
        this.definition = definition;
    }

    public String resolve(String input) {
        return applyPlaceholders(input, placeholders);
    }

    /**
     * Replaces every "clave" placeholder in a single pass. A sequential replace would corrupt
     * placeholders that contain other placeholders as a substring (ej. "%x%" dentro de
     * "%size_x%x%size_z%"), por eso se usa una sola pasada con regex.
     */
    private static final java.util.Map<String, java.util.regex.Pattern> PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static String applyPlaceholders(String input, Map<String, String> values) {
        if (input == null || values == null || values.isEmpty()) return input;
        StringBuilder pattern = new StringBuilder();
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            if (pattern.length() > 0) pattern.append('|');
            pattern.append(java.util.regex.Pattern.quote(e.getKey()));
        }
        if (pattern.length() == 0) return input;

        // el conjunto de claves se repite constantemente (menus/hologramas), cachear el Pattern evita
        // recompilar la misma expresion en cada linea y en cada refresco
        String patternKey = pattern.toString();
        if (PATTERN_CACHE.size() > 256) PATTERN_CACHE.clear();
        java.util.regex.Pattern compiled = PATTERN_CACHE.computeIfAbsent(patternKey, java.util.regex.Pattern::compile);

        java.util.regex.Matcher m = compiled.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String v = values.get(m.group());
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(v == null ? "" : v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public void setPlaceholders(Map<String, String> values) {
        placeholders.putAll(values);
    }

    public void addButton(int slot, ItemStack item, String action, Object payload) {
        ButtonInstance instance = new ButtonInstance(slot, item, action);
        instance.payload = payload;
        buttons.add(instance);
    }

    public void addButton(ButtonInstance instance) {
        buttons.add(instance);
    }

    public ButtonInstance getButton(int slot) {
        for (ButtonInstance b : buttons) {
            if (b.slot == slot) return b;
        }
        return null;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, definition.size, ColorUtil.color(resolve(definition.title)));
        render();
        player.openInventory(inventory);
        MenuManager.getInstance().registerOpenMenu(player, this);
        if (definition.openSound != null) {
            MenuManager.playSound(player, definition.openSound);
        }
    }

    public void refresh() {
        // rebuild every button (static + dynamic) through the controller before rendering
        buttons.clear();
        if (refreshCallback != null) {
            refreshCallback.accept(this);
        }
        render();
    }

    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        if (inventory == null) return;
        inventory.clear();
        if (definition.fillerEnabled) {
            ItemStack filler = new ItemBuilder(Material.matchMaterial(definition.fillerMaterial) == null
                    ? Material.GRAY_STAINED_GLASS_PANE : Material.matchMaterial(definition.fillerMaterial))
                    .name(definition.fillerName)
                    .modelData(definition.fillerModelData)
                    .build();
            for (int i = 0; i < definition.size; i++) {
                inventory.setItem(i, filler);
            }
        }
        for (ButtonInstance button : buttons) {
            if (button.slot >= 0 && button.slot < definition.size) {
                inventory.setItem(button.slot, button.item);
            }
        }
    }

    /**
     * A single clickable element inside a menu.
     */
    public static class ButtonInstance {
        public final int slot;
        public final ItemStack item;
        public final String action;
        public String leftAction;
        public String rightAction;
        public String permission;
        public String sound;
        public Object payload;
        public boolean dynamic = true;

        public ButtonInstance(int slot, ItemStack item, String action) {
            this.slot = slot;
            this.item = item;
            this.action = action;
        }

        public String actionFor(boolean rightClick) {
            if (rightClick && rightAction != null) return rightAction;
            if (!rightClick && leftAction != null) return leftAction;
            return action;
        }
    }
}
