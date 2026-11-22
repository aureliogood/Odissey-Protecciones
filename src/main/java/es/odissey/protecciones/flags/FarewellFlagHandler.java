package es.odissey.protecciones.flags;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import es.odissey.protecciones.FlagHandler;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

// farewell-action flag
public class FarewellFlagHandler extends FlagValueChangeHandler<String> {
    public static final FarewellFlagHandler.Factory FACTORY = new FarewellFlagHandler.Factory();
    public static class Factory extends Handler.Factory<FarewellFlagHandler> {
        @Override
        public FarewellFlagHandler create(Session session) {
            return new FarewellFlagHandler(session);
        }
    }

    protected FarewellFlagHandler(Session session) {
        super(session, FlagHandler.FAREWELL_ACTION);
    }

    @Override
    protected void onInitialValue(LocalPlayer localPlayer, ApplicableRegionSet applicableRegionSet, String s) {

    }

    @Override
    protected boolean onSetValue(LocalPlayer localPlayer, Location from, Location to, ApplicableRegionSet toSet, String currentValue, String lastValue, MoveType moveType) {
        Player p = Bukkit.getPlayer(localPlayer.getUniqueId());

        // the greeting action flag for the other region should show instead if it is set
        for (ProtectedRegion r : toSet.getRegions()) {
            if (r.getFlag(FlagHandler.GREET_ACTION) != null) {
                return true;
            }
        }
        if (p != null && lastValue != null && !lastValue.equals(currentValue)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', lastValue)));
        }
        return true;
    }

    @Override
    protected boolean onAbsentValue(LocalPlayer localPlayer, Location location, Location location1, ApplicableRegionSet applicableRegionSet, String lastValue, MoveType moveType) {
        Player p = Bukkit.getPlayer(localPlayer.getUniqueId());
        if (p != null && lastValue != null) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', lastValue)));
        }
        return true;
    }
}
