package es.odissey.protecciones.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Routes inventory and chat events to the {@link MenuManager}.
 */
public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Menu menu = MenuManager.getInstance().getOpenMenu(p);
        if (menu == null || e.getView().getTopInventory() != menu.getInventory()) return;

        boolean rightClick = e.getClick().isRightClick();
        MenuManager.getInstance().onClick(p, e.getRawSlot(), rightClick);
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Menu menu = MenuManager.getInstance().getOpenMenu(p);
        if (menu == null) return;
        if (e.getView().getTopInventory() != menu.getInventory()) return;
        // con un menu abierto no se permite arrastrar en NINGUN slot: si no, se puede
        // recolocar un bloque de proteccion y cobrar el reembolso de otro distinto
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        Menu menu = MenuManager.getInstance().getOpenMenu(p);
        if (menu == null) return;
        if (menu.getInventory() != e.getInventory()) return;
        MenuManager.getInstance().unregister(p);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!MenuManager.getInstance().isAwaitingInput(p)) return;
        e.setCancelled(true);
        String message = e.getMessage();
        Bukkit.getScheduler().runTask(es.odissey.protecciones.OdisseyProtecciones.getInstance(),
                () -> MenuManager.getInstance().handleInput(p, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        MenuManager.getInstance().unregister(e.getPlayer());
    }
}
