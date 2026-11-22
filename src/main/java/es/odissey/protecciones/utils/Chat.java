package es.odissey.protecciones.utils;

import es.odissey.protecciones.PSConfig;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.gui.ColorUtil;
import org.bukkit.command.CommandSender;

/**
 * Helper central de mensajes de chat del plugin. Aplica el prefijo de identidad ODISSEY
 * (configurable en config.yml -> messages.prefix) a los mensajes de feedback de los menus.
 */
public final class Chat {

    /** Prefijo por defecto (estilo ODISSEY). */
    public static final String PREFIJO_POR_DEFECTO = "&8[&eODISSEY&8]&r ";

    private Chat() {
    }

    /** @return el prefijo configurado, ya con colores aplicados. */
    public static String prefijo() {
        String prefix = null;
        if (OdisseyProtecciones.getInstance() != null) {
            PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
            if (c != null) prefix = c.messagesPrefix;
        }
        if (prefix == null) prefix = PREFIJO_POR_DEFECTO;
        return ColorUtil.color(prefix);
    }

    /** @return el texto con el prefijo ODISSEY delante. */
    public static String con(String texto) {
        return prefijo() + ColorUtil.color(texto == null ? "" : texto);
    }

    /** Envia el texto al receptor, anteponiendo el prefijo ODISSEY. */
    public static void enviar(CommandSender sender, String texto) {
        if (sender == null || texto == null || texto.isEmpty()) return;
        sender.sendMessage(con(texto));
    }
}
