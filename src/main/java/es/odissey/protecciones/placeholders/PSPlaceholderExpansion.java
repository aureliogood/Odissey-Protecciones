package es.odissey.protecciones.placeholders;

import es.odissey.protecciones.OdisseyProtecciones;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PSPlaceholderExpansion extends PlaceholderExpansion {

    // persist through reloads
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getIdentifier() {
        return "odisseyprotecciones";
    }

    @Override
    public String getAuthor() {
        return OdisseyProtecciones.getInstance().getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return OdisseyProtecciones.getInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        String[] split = identifier.split("_");
        if (split.length > 0) {
            switch (split[0]) {
                case "config":
                    return ConfigPlaceholders.resolveConfig(identifier);
                case "currentregion":
                    return RegionPlaceholders.resolveCurrentRegionPlaceholders(p, identifier);
                case "region":
                    return RegionPlaceholders.resolveSpecifiedRegionPlaceholders(p, identifier);
                case "currentplayer":
                    return PlayerPlaceholders.resolvePlayer(p, identifier);
            }
        }

        return "";
    }
}
