package es.odissey.protecciones.commands;

import es.odissey.protecciones.OdisseyProtecciones;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ArgAdminHelp {

    private static void send(CommandSender p, String text, String info, String clickCommand, boolean run) {
        // Create the main text component from legacy text.
        BaseComponent[] mainComponents = TextComponent.fromLegacyText(text);
        TextComponent mainText = new TextComponent("");
        for (BaseComponent component : mainComponents) {
            mainText.addExtra(component);
        }

        // Create the hover event from the info text, add click event after
        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(info);
        mainText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));
        //toggle for running on mouse click, currently disabled
        if (run) {
            mainText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ChatColor.stripColor(clickCommand)));
        } else {
            mainText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ChatColor.stripColor(clickCommand)));
        }

        // Send the assembled message.
        p.spigot().sendMessage(mainText);
    }

    static boolean argumentAdminHelp(CommandSender p, String[] args) {
        String bc = "/" + OdisseyProtecciones.getInstance().getConfigOptions().base_command;
        String tx = ChatColor.AQUA + "> " + ChatColor.GRAY + bc;

        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "===============" +
                ChatColor.RESET + " Ayuda de admin " +
                ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "===============\n");

        send(p,
                tx + " admin version",
                "Muestra el numero de version del plugin.\n\n" + bc + " admin version",
                bc + " admin version",
                false);

        send(p,
                tx + " admin hide",
                "Oculta todos los bloques de proteccion en el mundo en el que estas.\n\n" + bc + " admin hide",
                bc + " admin hide",
                false);

        send(p,
                tx + " admin unhide",
                "Muestra todos los bloques de proteccion en el mundo en el que estas.\n\n" + bc + " admin unhide",
                bc + " admin unhide",
                false);

        send(p,
                tx + " admin cleanup remove",
                "Elimina a los jugadores inactivos que no se han conectado en los ultimos [days] dias de las regiones protegidas del mundo en el que estas (o el especificado). Luego, elimina las regiones sin duenos.\n\n" +
                        bc + " admin cleanup remove [days] [-t typealias (optional)] [world (console)]",
                bc + " admin cleanup remove",
                false);

        send(p,
                tx + " admin cleanup disown",
                "Elimina a los jugadores inactivos que no se han conectado en los ultimos [days] dias de las regiones protegidas del mundo en el que estas (o el especificado).\n\n" +
                        bc + " admin cleanup disown",
                bc + " admin cleanup disown",
                false);

        send(p,
                tx + " admin flag",
                "Establece una flag para todas las regiones de proteccion de un mundo.\n\n" +
                        bc + " admin flag [world] [flagname] [value|null|default]",
                bc + " admin flag [world] [flagname] [value|null|default]",
                false);

        send(p,
                tx + " admin lastlogon",
                "Muestra la ultima vez que se conecto un jugador.\n\n" + bc + " admin lastlogon [player]",
                bc + " admin lastlogon",
                false);

        send(p,
                tx + " admin lastlogons",
                "Lista las ultimas conexiones de cada jugador.\n\n" + bc + " admin lastlogons",
                bc + " admin lastlogons",
                false);

        send(p,
                tx + " admin stats",
                "Muestra algunas estadisticas del plugin.\n\n" + bc + " admin stats [player (optional)]",
                bc + " admin stats",
                false);

        send(p,
                tx + " admin recreate",
                "Recrea todas las regiones usando el radio establecido en la config.\n\n" + bc + " admin recreate",
                bc + " admin recreate",
                false);

        send(p,
                tx + " admin debug",
                "Activa o desactiva el modo debug.\n\n" + bc + " admin debug",
                bc + " admin debug",
                false);

        send(p,
                tx + " admin settaxautopayers",
                "Agrega un pago automatico de impuestos para cada region del servidor que no tenga uno.\n\n" + bc + " admin settaxautopayers",
                bc + " admin settaxautopayers",
                false);

        send(p,
                tx + " admin forcemerge",
                "Fusiona las regiones superpuestas si tienen los mismos duenos, miembros y flags.\n\n" +
                        bc + " admin forcemerge [world]",
                bc + " admin forcemerge [world]",
                false);

        send(p,
                tx + " admin changeblock",
                "Cambia todos los bloques y regiones de un mundo a un bloque diferente. Ambos bloques deben estar configurados en la config.\n\n" +
                        bc + " admin changeblock [world] [oldtypealias] [newtypealias]",
                bc + " admin changeblock [world] [oldtypealias] [newtypealias]",
                false);

        send(p,
                tx + " admin changeregiontype",
                "Cambia el tipo interno de todas las regiones de cierto tipo. Util para corregir errores.\n\n" +
                        bc + " admin changeregiontype [world] [oldtype] [newtype]",
                bc + " admin changeregiontype [world] [oldtype] [newtype]",
                false);

        send(p,
                tx + " admin fixregions",
                "Usa este comando para recalcular los tipos de bloque de las regiones de un mundo.\n\n" + bc + " admin fixregions",
                bc + " admin fixregions",
                false);
        //add footer since it was missing
        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=============================================");

        return true;
    }
}
