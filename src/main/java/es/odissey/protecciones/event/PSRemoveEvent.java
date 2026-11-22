package es.odissey.protecciones.event;

import es.odissey.protecciones.PSRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Evento que se llama cuando se elimina una region de proteccion
 */

public class PSRemoveEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private PSRegion region;
    private Player p = null;
    private boolean isCancelled = false;

    public PSRemoveEvent(PSRegion psr, Player player) {
        this.region = checkNotNull(psr);
        this.p = player;
    }

    public PSRemoveEvent(PSRegion psr) {
        this.region = checkNotNull(psr);
    }

    /**
     * Returns the player that removed the protect block, if applicable
     * @return the player, or null if the region was not removed because of a player
     */
    public Player getPlayer() {
        return p;
    }

    /**
     * Returns the region being removed.
     * @return the region being removed
     */
    public PSRegion getRegion() {
        return region;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
