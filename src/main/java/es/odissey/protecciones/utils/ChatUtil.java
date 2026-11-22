package es.odissey.protecciones.utils;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSRegion;
import org.bukkit.entity.Player;

import java.util.List;

public class ChatUtil {
    public static void displayDuplicateRegionAliases(Player p, List<PSRegion> r) {
        StringBuilder rep = new StringBuilder(r.get(0).getId() + " (" + r.get(0).getWorld().getName() + ")");

        for (int i = 1; i < r.size(); i++) {
            rep.append(String.format(", %s (%s)", r.get(i).getId(), r.get(i).getWorld().getName()));
        }

        PSL.msg(p, PSL.SPECIFY_ID_INSTEAD_OF_ALIAS.msg().replace("%regions%", rep.toString()));
    }
}
