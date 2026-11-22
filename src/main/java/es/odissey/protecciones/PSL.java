package es.odissey.protecciones;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PSL {
    // messages.yml

    COOLDOWN("cooldown", ChatColor.GOLD + "Aviso: " + ChatColor.GRAY + "¡Espera %time% segundos antes de colocar de nuevo!"),
    NO_SUCH_COMMAND("no_such_command", ChatColor.RED + "Comando no encontrado. Usa /ps help para mas informacion."),
    NO_ACCESS("no_access", ChatColor.RED + "No tienes permitido hacer eso aqui."),
    NO_ROOM_IN_INVENTORY("no_room_in_inventory", ChatColor.RED + "No tienes suficiente espacio en tu inventario."),
    NO_ROOM_DROPPING_ON_FLOOR("no_room_dropping_on_floor", ChatColor.RED + "No tienes suficiente espacio en tu inventario. Soltando el objeto al suelo."),
    INVALID_BLOCK("invalid_block", ChatColor.RED + "Bloque de proteccion invalido."),
    NOT_ENOUGH_MONEY("not_enough_money", ChatColor.RED + "¡No tienes suficiente dinero! El precio es %price%."),
    PAID_MONEY("paid_money", ChatColor.AQUA + "Has pagado $%price%."),
    INVALID_WORLD("invalid_world", ChatColor.RED + "Mundo invalido."),
    MUST_BE_PLAYER("must_be_player", ChatColor.RED + "Debes ser un jugador para ejecutar este comando."),
    GO_BACK_PAGE("go_back_page", "Volver una pagina."),
    GO_NEXT_PAGE("go_next_page", "Ir a la pagina siguiente."),
    PAGE_DOES_NOT_EXIST("page_does_not_exist", ChatColor.RED + "La pagina no existe."),

    HELP("help", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Ayuda " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "=====\n" + ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps help"),
    HELP_NEXT("help_next", ChatColor.GRAY + "¡Usa /ps help %page% para ir a la siguiente pagina!"),

    COMMAND_REQUIRES_PLAYER_NAME("command_requires_player_name", ChatColor.RED + "Este comando requiere el nombre de un jugador."),

    NO_PERMISSION_TOGGLE("no_permission_toggle", ChatColor.RED + "No tienes permiso para usar el comando toggle."),
    NO_PERMISSION_CREATE("no_permission_create", ChatColor.RED + "No tienes permiso para colocar un bloque de proteccion."),
    NO_PERMISSION_CREATE_SPECIFIC("no_permission_create_specific", ChatColor.RED + "No tienes permiso para colocar este tipo de bloque de proteccion."),
    NO_PERMISSION_DESTROY("no_permission_destroy", ChatColor.RED + "No tienes permiso para destruir un bloque de proteccion."),
    NO_PERMISSION_MEMBERS("no_permission_members", "&cNo tienes permiso para usar comandos de miembros."),
    NO_PERMISSION_OWNERS("no_permission_owners", "&cNo tienes permiso para usar comandos de duenos."),
    MEMBER_PERM_DENIED("member_perm_denied", ChatColor.RED + "No tienes permiso para hacer eso en esta proteccion."),
    NO_PERMISSION_ADMIN("no_permission_admin", ChatColor.RED + "No tienes permiso para usar ese comando."),
    NO_PERMISSION_COUNT("no_permission_count", ChatColor.RED + "No tienes permiso para usar ese comando."),
    NO_PERMISSION_COUNT_OTHERS("no_permission_count_others", ChatColor.RED + "No tienes permiso para usar ese comando."),
    NO_PERMISSION_FLAGS("no_permission_flags", "&cNo tienes permiso para usar comandos de flags."),
    NO_PERMISSION_PER_FLAG("no_permission_per_flag", ChatColor.RED + "No tienes permiso para usar esa flag."),
    NO_PERMISSION_RENT("no_permission_rent", ChatColor.RED + "No tienes permiso para alquilar."),
    NO_PERMISSION_TAX("no_permission_tax", ChatColor.RED + "No tienes permiso para usar el comando tax."),
    NO_PERMISSION_BUYSELL("no_permission_buysell", ChatColor.RED + "No tienes permiso para comprar y vender regiones."),
    NO_PERMISSION_UNHIDE("no_permission_unhide", ChatColor.RED + "No tienes permiso para mostrar bloques de proteccion."),
    NO_PERMISSION_HIDE("no_permission_hide", ChatColor.RED + "No tienes permiso para ocultar bloques de proteccion."),
    NO_PERMISSION_INFO("no_permission_info", ChatColor.RED + "No tienes permiso para usar el comando de informacion de region."),
    NO_PERMISSION_PRIORITY("no_permission_priority", ChatColor.RED + "No tienes permiso para usar el comando de prioridad."),
    NO_PERMISSION_REGION("no_permission_region", ChatColor.RED + "No tienes permiso para usar los comandos de region."),
    NO_PERMISSION_TP("no_permission_tp", ChatColor.RED + "No tienes permiso para teletransportarte a los bloques de proteccion de otros jugadores."),
    NO_PERMISSION_HOME("no_permission_home", ChatColor.RED + "No tienes permiso para teletransportarte a tus bloques de proteccion."),
    NO_PERMISSION_UNCLAIM("no_permission_unclaim", ChatColor.RED + "No tienes permiso para usar el comando unclaim."),
    NO_PERMISSION_UNCLAIM_REMOTE("no_permission_unclaim_remote", ChatColor.RED + "No tienes permiso para usar el comando unclaim remoto."),
    NO_PERMISSION_VIEW("no_permission_view", ChatColor.RED + "No tienes permiso para usar el comando view."),
    NO_PERMISSION_GIVE("no_permission_give", ChatColor.RED + "No tienes permiso para usar el comando give."),
    NO_PERMISSION_GET("no_permission_get", ChatColor.RED + "No tienes permiso para usar el comando get."),
    NO_PERMISSION_SETHOME("no_permission_sethome", ChatColor.RED + "No tienes permiso para usar el comando sethome."),
    NO_PERMISSION_LIST("no_permission_list", ChatColor.RED + "No tienes permiso para usar el comando list."),
    NO_PERMISSION_LIST_OTHERS("no_permission_list_others", ChatColor.RED + "No tienes permiso para usar el comando list para otros."),
    NO_PERMISSION_NAME("no_permission_name", ChatColor.RED + "No tienes permiso para usar el comando name."),
    NO_PERMISSION_SETPARENT("no_permission_setparent", ChatColor.RED + "No tienes permiso para usar el comando setparent."),
    NO_PERMISSION_SETPARENT_OTHERS("no_permission_setparent_others", ChatColor.RED + "No tienes permiso para heredar de regiones que no posees."),
    NO_PERMISSION_MERGE("no_permission_merge", ChatColor.RED + "No tienes permiso para usar /ps merge."),

    ADDED_TO_REGION("psregion.added_to_region", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ha sido agregado a esta region."),
    ADDED_TO_REGION_SPECIFIC("psregion.added_to_region_specific", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ha sido agregado a la region %region%."),
    REMOVED_FROM_REGION("psregion.removed_from_region", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ha sido eliminado de la region."),
    REMOVED_FROM_REGION_SPECIFIC("psregion.removed_from_region_specific", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ha sido eliminado de la region %region%."),
    NOT_IN_REGION("psregion.not_in_region", ChatColor.RED + "¡No estas dentro de una region protegida!"),
    PLAYER_NOT_FOUND("psregion.player_not_found", ChatColor.RED + "Jugador no encontrado."),
    NOT_PS_REGION("psregion.not_ps_region", ChatColor.RED + "Esto no es una region protegida."),
    REGION_DOES_NOT_EXIST("psregion.region_does_not_exist", ChatColor.RED + "La region no existe."),
    NO_REGIONS_OWNED("psregion.no_regions_owned", ChatColor.RED + "¡No posees ninguna region protegida en este mundo!"),
    NO_REGION_PERMISSION("psregion.no_region_permission", ChatColor.RED + "No tienes permiso para hacer esto en esta region."),
    PROTECTED("psregion.protected", ChatColor.GREEN + "" + ChatColor.BOLD + "Zona protegida! " + ChatColor.GRAY + "Tu proteccion se creo correctamente."),
    NO_LONGER_PROTECTED("psregion.no_longer_protected", ChatColor.YELLOW + "" + ChatColor.BOLD + "Proteccion eliminada. " + ChatColor.GRAY + "Esta zona ya no esta protegida."),
    CANT_PROTECT_THAT("psregion.cant_protect_that", ChatColor.RED + "No puedes proteger esa zona."),
    REACHED_REGION_LIMIT("psregion.reached_region_limit", ChatColor.RED + "No puedes tener mas regiones protegidas (%limit%)."),
    REACHED_PER_BLOCK_REGION_LIMIT("psregion.reached_per_block_region_limit", ChatColor.RED + "No puedes tener mas regiones de este tipo (%limit%)."),
    WORLD_DENIED_CREATE("psregion.world_denied_create", ChatColor.RED + "No puedes crear protecciones en este mundo."),
    REGION_OVERLAP("psregion.region_overlap", ChatColor.RED + "No puedes colocar un bloque de proteccion aqui porque se superpone con otra region."),
    REGION_TOO_CLOSE("psregion.region_too_close", ChatColor.RED + "¡Tu bloque de proteccion debe estar a un minimo de %num% bloques del borde de otras regiones!"),
    REGION_CANT_TELEPORT("psregion.cant_teleport", ChatColor.RED + "¡Tu teletransporte fue bloqueado por una region protegida!"),
    SPECIFY_ID_INSTEAD_OF_ALIAS("psregion.specify_id_instead_of_alias", ChatColor.GRAY + "¡Se encontraron varias regiones con este nombre! Usa un ID en su lugar.\n Regiones con este nombre: " + ChatColor.AQUA + "%regions%"),
    REGION_NOT_ADJACENT("psregion.region_not_adjacent", ChatColor.RED + "¡Has superado el limite de regiones no adyacentes! Intenta colocar tu bloque de proteccion mas cerca de otras regiones que ya posees."),
    REGION_NOT_OVERLAPPING("psregion.not_overlapping", ChatColor.RED + "¡Estas regiones no se superponen entre si!"),
    MULTI_REGION_DOES_NOT_EXIST("psregion.multi_region_does_not_exist", "¡Una de estas regiones no existe!"),
    NO_REGION_HOLES("psregion.no_region_holes", ChatColor.RED + "¡Se detecto un area sin proteger dentro de la region! ¡Esto no esta permitido!"),
    DELETE_REGION_PREVENTED_NO_HOLES("psregion.delete_region_prevented", ChatColor.GRAY + "No se pudo eliminar la region, posiblemente porque crea un hueco en la region existente."),
    NOT_OWNER("psregion.not_owner", ChatColor.RED + "¡No eres un dueno de esta region!"),
    CANNOT_MERGE_RENTED_REGION("psregion.cannot_merge_rented_region", ChatColor.RED + "No se pueden fusionar las regiones porque la region %region% esta en proceso de alquiler."),
    NO_PERMISSION_REGION_TYPE("psregion.no_permission_region_type", ChatColor.RED + "No tienes permiso para tener este tipo de region."),
    REGION_HIDDEN("psregion.hidden", ChatColor.GRAY + "El bloque de proteccion ahora esta oculto."),
    MUST_BE_PLACED_IN_EXISTING_REGION("psregion.must_be_placed_in_existing_region", ChatColor.RED + "¡Esto debe colocarse dentro de una region existente!"),
    REGION_ALREADY_IN_LOCATION_IS_HIDDEN("psregion.already_in_location_is_hidden", ChatColor.RED + "Ya existe una region en esta ubicacion (¿esta el bloque de proteccion oculto?)"),
    CANNOT_REMOVE_YOURSELF_LAST_OWNER("psregion.cannot_remove_yourself_last_owner", ChatColor.RED + "No puedes eliminarte a ti mismo porque eres el ultimo dueno."),
    CANNOT_REMOVE_YOURSELF_FROM_ALL_REGIONS("psregion.cannot_remove_yourself_all_regions", ChatColor.RED + "Por seguridad, no puedes eliminarte a ti mismo de todas tus regiones a la vez."),

    // ps toggle
    TOGGLE_HELP("toggle.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps toggle|on|off"),
    TOGGLE_HELP_DESC("toggle.help_desc", "Usa este comando para activar o desactivar la colocacion de bloques de proteccion."),
    TOGGLE_ON("toggle.toggle_on", ChatColor.AQUA + "Colocacion de bloques de proteccion activada."),
    TOGGLE_OFF("toggle.toggle_off", ChatColor.AQUA + "Colocacion de bloques de proteccion desactivada."),

    // ps count
    COUNT_HELP("count.count_help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps count [jugador (opcional)]"),
    COUNT_HELP_DESC("count.count_help_desc", "Cuenta el numero de regiones que posees tu u otro jugador."),
    PERSONAL_REGION_COUNT("count.personal_region_count", ChatColor.GRAY + "Tu cantidad de regiones en este mundo: " + ChatColor.AQUA + "%num%"),
    PERSONAL_REGION_COUNT_MERGED("count.personal_region_count_merged", ChatColor.GRAY + "- Incluyendo cada region fusionada: " + ChatColor.AQUA + "%num%"),
    OTHER_REGION_COUNT("count.other_region_count", ChatColor.GRAY + "Cantidad de regiones de %player% en este mundo: " + ChatColor.AQUA + "%num%"),
    OTHER_REGION_COUNT_MERGED("count.other_region_count_merged", ChatColor.GRAY + "- Incluyendo cada region fusionada: " + ChatColor.AQUA + "%num%"),

    // ps flag
    FLAG_HELP("flag.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps flag [nombre_flag] [valor|null|default]"),
    FLAG_HELP_DESC("flag.help_desc", "Usa este comando para establecer una flag en tu region protegida."),
    FLAG_SET("flag.flag_set", ChatColor.AQUA + "%flag%" + ChatColor.GRAY + " ha sido establecida."),
    FLAG_NOT_SET("flag.flag_not_set", ChatColor.AQUA + "%flag%" + ChatColor.GRAY + " " + ChatColor.RED + "no" + ChatColor.GRAY + " ha sido establecida. Revisa tus valores nuevamente."),
    FLAG_PREVENT_EXPLOIT("flag.flag_prevent_exploit", ChatColor.RED + "Esto ha sido deshabilitado para prevenir exploits."),
    FLAG_PREVENT_EXPLOIT_HOVER("flag.flag_prevent_exploit_hover", ChatColor.RED + "Deshabilitado por razones de seguridad."),
    FLAG_GUI_HEADER("flag.gui_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Flags (click para cambiar) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    FLAG_GUI_HOVER_SET("flag.gui_hover_set", ChatColor.AQUA + "Click para establecer."),
    FLAG_GUI_HOVER_SET_TEXT("flag.gui_hover_set_text", ChatColor.AQUA + "Click para cambiar." + ChatColor.WHITE + "\nValor actual:\n%value%"),
    FLAG_GUI_HOVER_CHANGE_GROUP("flag.hover_change_group", "Click para que esta flag solo se aplique a %group%."),
    FLAG_GUI_HOVER_CHANGE_GROUP_NULL("flag.hover_change_group_null", ChatColor.RED + "Debes establecer un valor para esta flag antes de cambiar el grupo."),

    // ps rent
    RENT_HELP("rent.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps rent"),
    RENT_HELP_DESC("rent.help_desc", "Usa este comando para gestionar alquileres (comprar y vender)."),
    RENT_HELP_HEADER("rent.help_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Ayuda de alquiler " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    RENT_ALREADY_RENTING("rent.already_renting", ChatColor.RED + "¡La region ya esta siendo alquilada! Primero debes detener el alquiler."),
    RENT_NOT_RENTED("rent.not_rented", ChatColor.RED + "Esta region no esta siendo alquilada."),
    RENT_LEASE_SUCCESS("rent.lease_success", ChatColor.AQUA + "Terminos de alquiler de la region establecidos:\n" + ChatColor.AQUA + "Precio: " + ChatColor.GRAY + "%price%\n" + ChatColor.AQUA + "Periodo de pago: " + ChatColor.GRAY + "%period%"),
    RENT_STOPPED("rent.stopped", ChatColor.AQUA + "Alquiler detenido."),
    RENT_EVICTED("rent.evicted", ChatColor.GRAY + "Inquilino %tenant% desalojado."),
    RENT_NOT_RENTING("rent.not_renting", ChatColor.RED + "Esta region no esta siendo alquilada a inquilinos."),
    RENT_PAID_LANDLORD("rent.paid_landlord", ChatColor.AQUA + "%tenant%" + ChatColor.GRAY + " ha pagado " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + " por alquilar " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + "."),
    RENT_PAID_TENANT("rent.paid_tenant", ChatColor.GRAY + "Pagaste " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + " a " + ChatColor.AQUA + "%landlord%" + ChatColor.GRAY + " por la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + "."),
    RENT_RENTING_LANDLORD("rent.renting_landlord", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ahora esta alquilando la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + "."),
    RENT_RENTING_TENANT("rent.renting_tenant", ChatColor.GRAY + "Ahora estas alquilando la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " por " + ChatColor.AQUA + "%price%" + ChatColor.GRAY + " por " + ChatColor.AQUA + "%period%" + ChatColor.GRAY + "."),
    RENT_NOT_TENANT("rent.not_tenant", ChatColor.RED + "¡No eres el inquilino de esta region!"),
    RENT_TENANT_STOPPED_LANDLORD("rent.tenant_stopped_landlord", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " ha dejado de alquilar la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + ". Ahora esta disponible para que otros la alquilen."),
    RENT_TENANT_STOPPED_TENANT("rent.tenant_stopped_tenant", ChatColor.AQUA + "Has dejado de alquilar la region %region%."),
    RENT_BEING_SOLD("rent.being_sold", ChatColor.RED + "¡La region esta en venta! Haz /ps sell stop primero."),
    RENT_EVICT_NO_MONEY_TENANT("rent.evict_no_money_tenant", ChatColor.GRAY + "Has sido " + ChatColor.RED + "desalojado" + ChatColor.GRAY + " de la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " porque no tienes suficiente dinero (%price%) para pagar el alquiler."),
    RENT_EVICT_NO_MONEY_LANDLORD("rent.evict_no_money_landlord", ChatColor.AQUA + "%tenant%" + ChatColor.GRAY + " ha sido " + ChatColor.RED + "desalojado" + ChatColor.GRAY + " de la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " porque no puede pagar el alquiler."),
    RENT_CANNOT_RENT_OWN_REGION("rent.cannot_rent_own_region", ChatColor.RED + "¡No puedes alquilar tu propia region!"),
    RENT_REACHED_LIMIT("rent.reached_limit", ChatColor.RED + "¡Has alcanzado el limite de regiones que puedes alquilar!"),
    RENT_PRICE_TOO_LOW("rent.price_too_low", ChatColor.RED + "El precio del alquiler es demasiado bajo (debe ser mayor que %price%)."),
    RENT_PRICE_TOO_HIGH("rent.price_too_high", ChatColor.RED + "El precio del alquiler es demasiado alto (debe ser menor que %price%)."),
    RENT_PERIOD_TOO_SHORT("rent.period_too_short", ChatColor.RED + "El periodo de alquiler es demasiado corto (debe ser mayor que %period% segundos)."),
    RENT_PERIOD_TOO_LONG("rent.period_too_long", ChatColor.RED + "El periodo de alquiler es demasiado largo (debe ser menor que %period% segundos)."),
    RENT_PERIOD_INVALID("rent.period_invalid", ChatColor.RED + "¡Formato de periodo invalido! Ejemplo: 24h para una vez al dia."),
    RENT_CANNOT_BREAK_WHILE_RENTING("rent.cannot_break_while_renting", ChatColor.RED + "No puedes romper la region mientras esta alquilada."),
    RENT_EXPIRING_TENANT("rent.expiring_tenant", ChatColor.GOLD + "Aviso: " + ChatColor.GRAY + "el alquiler de " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " vence en " + ChatColor.RED + "%time%" + ChatColor.GRAY + ". Asegurate de tener " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + " para renovarlo."),
    RENT_EXPIRING_LANDLORD("rent.expiring_landlord", ChatColor.GOLD + "Aviso: " + ChatColor.GRAY + "el alquiler de " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " por " + ChatColor.AQUA + "%tenant%" + ChatColor.GRAY + " vence en " + ChatColor.RED + "%time%" + ChatColor.GRAY + "."),

    // ps tax
    TAX_HELP("tax.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps tax"),
    TAX_HELP_DESC("tax.help_desc", "Usa este comando para gestionar y pagar impuestos."),
    TAX_HELP_HEADER("tax.help_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Ayuda de impuestos " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    TAX_DISABLED_REGION("tax.disabled_region", ChatColor.RED + "Los impuestos estan deshabilitados para esta region."),
    TAX_SET_AS_AUTOPAYER("tax.set_as_autopayer", ChatColor.GRAY + "Los impuestos de la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " ahora seran pagados automaticamente por ti."),
    TAX_SET_NO_AUTOPAYER("tax.set_no_autopayer", ChatColor.GRAY + "Los impuestos de la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " ahora deben pagarse manualmente."),
    TAX_PAID("tax.paid", ChatColor.GRAY + "Pagaste " + ChatColor.AQUA + "$%amount%" + ChatColor.GRAY + " en impuestos por la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + "."),
    TAX_INFO_HEADER("tax.info_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Info de impuestos (click para mas info) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    TAX_JOIN_MSG_PENDING_PAYMENTS("tax.join_msg_pending_payments", ChatColor.GRAY + "¡Tienes " + ChatColor.AQUA + "$%money%" + ChatColor.GRAY + " en impuestos pendientes en tus regiones!\nVeelos con /ps tax info."),
    TAX_PLAYER_REGION_INFO("tax.player_region_info", ChatColor.GRAY + "> " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + "$%money% pendiente"),
    TAX_PLAYER_REGION_INFO_AUTOPAYER("tax.player_region_info_autopayer", ChatColor.GRAY + "> " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + "$%money% pendiente" + ChatColor.GRAY + " (pago automatico)"),
    TAX_CLICK_TO_SHOW_MORE_INFO("tax.click_to_show_more_info", "Click para mostrar mas informacion."),
    TAX_REGION_INFO_HEADER("tax.region_info_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Info de impuestos de %region% " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    TAX_REGION_INFO("tax.region_info", ChatColor.BLUE + "Tasa de impuesto: " + ChatColor.GRAY + "$%taxrate% (suma de todas las regiones fusionadas)" + "\n"
            + ChatColor.BLUE + "Tiempo entre ciclos de impuesto: " + ChatColor.GRAY + "%taxperiod%" + "\n"
            + ChatColor.BLUE + "Tiempo para pagar impuestos tras el ciclo: " + ChatColor.GRAY + "%taxpaymentperiod%" + "\n"
            + ChatColor.BLUE + "Pago automatico de impuestos: " + ChatColor.GRAY + "%taxautopayer%" + "\n"
            + ChatColor.BLUE + "Impuestos debidos: " + ChatColor.GRAY + "$%taxowed%"),
    TAX_NEXT("tax.next_page", ChatColor.GRAY + "¡Usa /ps tax info -p %page% para ir a la siguiente pagina!"),

    // ps buy
    BUY_HELP("buy.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps buy"),
    BUY_HELP_DESC("buy.help_desc", "Compra la region en la que te encuentras actualmente."),
    BUY_NOT_FOR_SALE("buy.not_for_sale", ChatColor.RED + "Esta region no esta en venta."),
    BUY_STOP_SELL("buy.stop_sell", ChatColor.GRAY + "La region ya no esta en venta."),
    BUY_SOLD_BUYER("buy.sold_buyer", ChatColor.GRAY + "Compraste la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " por " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + " a " + ChatColor.AQUA + "%player%" + ChatColor.GRAY + "."),
    BUY_SOLD_SELLER("buy.sold_seller", ChatColor.GRAY + "Vendiste la region " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " por " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + " a " + ChatColor.AQUA + "%player%" + ChatColor.GRAY + "."),

    // ps sell
    SELL_HELP("sell.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps sell [precio|stop]"),
    SELL_HELP_DESC("sell.help_desc", "Vende la region en la que te encuentras actualmente."),
    SELL_RENTED_OUT("sell.rented_out", ChatColor.RED + "¡La region esta alquilada! Debes detener el alquiler para venderla."),
    SELL_FOR_SALE("sell.for_sale", ChatColor.GRAY + "La region ahora esta en venta por " + ChatColor.AQUA + "$%price%" + ChatColor.GRAY + "."),

    // ps hide/unhide
    VISIBILITY_HIDE_HELP("visibility.hide_help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps hide"),
    VISIBILITY_HIDE_HELP_DESC("visibility.hide_help_desc", "Usa este comando para ocultar o mostrar tu bloque de proteccion."),
    VISIBILITY_UNHIDE_HELP("visibility.unhide_help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps unhide"),
    VISIBILITY_UNHIDE_HELP_DESC("visibility.unhide_help_desc", "Usa este comando para ocultar o mostrar tu bloque de proteccion."),
    ALREADY_NOT_HIDDEN("visibility.already_not_hidden", ChatColor.GRAY + "El bloque de proteccion no parece estar oculto..."),
    ALREADY_HIDDEN("visibility.already_hidden", ChatColor.GRAY + "El bloque de proteccion parece estar ya oculto..."),

    // ps info
    INFO_HELP("info.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps info members|owners|flags"),
    INFO_HELP_DESC("info.help_desc", "Usa este comando dentro de una region para ver mas informacion sobre ella."),
    INFO_HEADER("info.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Info de la region " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    INFO_TYPE2("info.type2", "&9Tipo: &7%type%", "%type%"),
    INFO_MAY_BE_MERGED("info.may_be_merged", "(puede estar fusionada con otros tipos)"),
    INFO_MERGED2("info.merged2", ChatColor.BLUE + "Regiones fusionadas: " + ChatColor.GRAY + "%merged%", "%merged%"),
    INFO_MEMBERS2("info.members2", "&9Miembros: &7%members%", "%members%"),
    INFO_NO_MEMBERS("info.no_members", ChatColor.RED + "(sin miembros)"),
    INFO_OWNERS2("info.owners2", "&9Duenos: &7%owners%", "%owners%"),
    INFO_NO_OWNERS("info.no_owners", ChatColor.RED + "(sin duenos)"),
    INFO_FLAGS2("info.flags2", "&9Flags: &7%flags%", "%flags%"),
    INFO_NO_FLAGS("info.no_flags", "(ninguna)"),
    INFO_REGION2("info.region2", "&9Region: &b%region%", "%region%"),
    INFO_PRIORITY2("info.priority2", "&9Prioridad: &b%priority%", "%priority%"),
    INFO_PARENT2("info.parent2", "&9Padre: &b%parentregion%", "%parentregion%"),
    INFO_BOUNDS_XYZ("info.bounds_xyz", "&9Limites: &b(%minx%,%miny%,%minz%) -> (%maxx%,%maxy%,%maxz%)",
            "%minx%", "%miny%", "%minz%", "%maxx%", "%maxy%", "%maxz%"
    ),
    INFO_BOUNDS_XZ("info.bounds_xz", "&9Limites: &b(%minx%, %minz%) -> (%maxx%, %maxz%)",
            "%minx%", "%minz%", "%maxx%", "%maxz%"
    ),
    INFO_SELLER2("info.seller2", "&9Vendedor: &7%seller%", "%seller%"),
    INFO_PRICE2("info.price2", "&9Precio: &7%price%", "%price%"),
    INFO_TENANT2("info.tenant2", "&9Inquilino: &7%tenant%", "%tenant%"),
    INFO_LANDLORD2("info.landlord2", "&9Arrendador: &7%landlord%", "%landlord%"),
    INFO_RENT2("info.rent2", "&9Alquiler: &7%rent%", "%rent%"),
    INFO_AVAILABLE_FOR_SALE("info.available_for_sale", ChatColor.AQUA + "¡Region disponible para la venta!"),
    INFO_AVAILABLE_FOR_RENT("info.available_for_rent", ChatColor.AQUA + "¡Region disponible para alquiler!"),

    // ps priority
    PRIORITY_HELP("priority.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps priority [numero|null]"),
    PRIORITY_HELP_DESC("priority.help_desc", "Usa este comando para establecer la prioridad de tu region."),
    PRIORITY_INFO("priority.info", ChatColor.GRAY + "Prioridad: %priority%"),
    PRIORITY_SET("priority.set", ChatColor.YELLOW + "La prioridad ha sido establecida."),
    PRIORITY_ERROR("priority.error", ChatColor.RED + "Error al interpretar la entrada, ¿revisala de nuevo?"),

    // ps region
    REGION_HELP("region.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps region [list|remove|disown] [jugador]"),
    REGION_HELP_DESC("region.help_desc", "Usa este comando para encontrar informacion o editar las regiones protegidas de otros jugadores (o las tuyas)."),
    REGION_NOT_FOUND_FOR_PLAYER("region.not_found_for_player", ChatColor.GRAY + "No se encontraron regiones para %player% en este mundo."),
    REGION_LIST("region.list", ChatColor.GRAY + "Regiones de %player% en este mundo: " + ChatColor.AQUA + "%regions%"),
    REGION_REMOVE("region.remove", ChatColor.YELLOW + "Las regiones de %player% han sido eliminadas en este mundo, y ha sido eliminado de las regiones que co-poseia."),
    REGION_DISOWN("region.disown", ChatColor.YELLOW + "%player% ha sido eliminado como dueno de todas las regiones en este mundo."),
    REGION_ERROR_SEARCH("region.error_search", ChatColor.RED + "Error al buscar las regiones de %player%. Asegurate de haber ingresado el nombre correcto."),

    // ps tp
    TP_HELP("tp.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps tp [id|jugador] [num (opcional)]"),
    TP_HELP_DESC("tp.help_desc", "Te teletransporta a una de las regiones de un jugador."),
    NUMBER_ABOVE_ZERO("tp.number_above_zero", ChatColor.RED + "Por favor ingresa un numero mayor que 0."),
    TP_VALID_NUMBER("tp.valid_number", ChatColor.RED + "Por favor ingresa un numero valido."),
    ONLY_HAS_REGIONS("tp.only_has_regions", ChatColor.RED + "¡%player% solo tiene %num% regiones protegidas en este mundo!"),
    TPING("tp.tping", ChatColor.GREEN + "Teletransportando..."),
    TP_ERROR_NAME("tp.error_name", ChatColor.RED + "¡Error al teletransportarse a la region protegida! (error al parsear el nombre de la region de WG)"),
    TP_ERROR_TP("tp.error_tp", ChatColor.RED + "¡Error al encontrar la region a la que teletransportarse!"),
    TP_IN_SECONDS("tp.in_seconds", ChatColor.GRAY + "Teletransportando en " + ChatColor.AQUA + "%seconds%" + ChatColor.GRAY + " segundos."),
    TP_CANCELLED_MOVED("tp.cancelled_moved", ChatColor.RED + "Teletransporte cancelado. ¡Te moviste!"),

    // ps home
    HOME_HELP("home.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps home [nombre|id]"),
    HOME_HELP_DESC("home.help_desc", "Te teletransporta a una de tus regiones protegidas."),
    HOME_HEADER("home.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Homes (click para teletransportarte) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    HOME_CLICK_TO_TP("home.click_to_tp", "¡Click para teletransportarte!"),
    HOME_NEXT("home.next_page", ChatColor.GRAY + "¡Usa /ps home -p %page% para ir a la siguiente pagina!"),

    // ps unclaim
    UNCLAIM_HELP("unclaim.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps unclaim"),
    UNCLAIM_HELP_DESC("unclaim.help_desc", "Usa este comando para recoger un bloque de proteccion colocado y eliminar la region."),
    UNCLAIM_HEADER("unclaim.header",ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Reclamar (click para reclamar) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),

    // ps view
    VIEW_HELP("view.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps view"),
    VIEW_HELP_DESC("view.help_desc", "Usa este comando para ver los bordes de una region protegida."),
    VIEW_COOLDOWN("view.cooldown", ChatColor.RED + "Espera un momento antes de usar /ps view de nuevo."),
    VIEW_GENERATING("view.generating", ChatColor.GRAY + "Generando borde..."),
    VIEW_GENERATE_DONE("view.generate_done", ChatColor.GREEN + "¡Listo! ¡El borde desaparecera despues de 30 segundos!"),
    VIEW_REMOVING("view.removing", ChatColor.AQUA + "Eliminando borde...\n" + ChatColor.GREEN + "¡Si aun ves bloques fantasma, reconectate!"),

    // ps admin
    ADMIN_HELP("admin.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps admin"),
    ADMIN_HELP_DESC("admin.help_desc", "Haz /ps admin help para mas informacion."),
    ADMIN_CLEANUP_HEADER("admin.cleanup_header", ChatColor.YELLOW + "Limpieza %arg% %time%\n================"),
    ADMIN_CLEANUP_FOOTER("admin.cleanup_footer", ChatColor.YELLOW + "================\nLimpieza %arg% completada."),
    ADMIN_HIDE_TOGGLED("admin.hide_toggled", ChatColor.YELLOW + "Todos los bloques de proteccion han sido %message% en este mundo."),
    ADMIN_LAST_LOGON("admin.last_logon", ChatColor.YELLOW + "%player% jugo por ultima vez hace %time%."),
    ADMIN_IS_BANNED("admin.is_banned", ChatColor.YELLOW + "%player% esta baneado."),
    ADMIN_ERROR_PARSING("admin.error_parsing", ChatColor.RED + "Error al interpretar los dias, ¿estas seguro de que es un numero?"),
    ADMIN_CONSOLE_WORLD("admin.console_world", ChatColor.RED + "Especifica el mundo como ultimo parametro."),
    ADMIN_LASTLOGONS_HEADER("admin.lastlogons_header", ChatColor.YELLOW + "%days% Dias o mas:\n================"),
    ADMIN_LASTLOGONS_LINE("admin.lastlogons_line", ChatColor.YELLOW + "%player% %time% dias"),
    ADMIN_LASTLOGONS_FOOTER("admin.lastlogons_footer", ChatColor.YELLOW + "================\n%count% Jugadores en total mostrados\n%checked% Jugadores en total revisados"),

    // ps reload
    RELOAD_HELP("reload.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps reload"),
    RELOAD_HELP_DESC("reload.help_desc", "Recarga la configuracion desde el config."),
    RELOAD_START("reload.start", ChatColor.AQUA + "Recargando configuracion..."),
    RELOAD_COMPLETE("reload.complete", ChatColor.AQUA + "¡Recarga de configuracion completada!"),

    // ps add/remove
    ADDREMOVE_HELP("addremove.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps add|remove [jugador]"),
    ADDREMOVE_HELP_DESC("addremove.help_desc", "Usa este comando para agregar o quitar un miembro de tu region protegida."),
    ADDREMOVE_OWNER_HELP("addremove.owner_help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps addowner|removeowner [jugador]"),
    ADDREMOVE_OWNER_HELP_DESC("addremove.owner_help_desc", "Usa este comando para agregar o quitar un dueno de tu region protegida."),
    ADDREMOVE_PLAYER_REACHED_LIMIT("addremove.player_reached_limit", ChatColor.RED + "Este jugador ha alcanzado su limite de regiones."),
    ADDREMOVE_PLAYER_NEEDS_TO_BE_ONLINE("addremove.player_needs_to_be_online", ChatColor.RED + "El jugador debe estar en linea para agregarlo."),

    // ps get
    GET_HELP("get.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps get [bloque]"),
    GET_HELP_DESC("get.help_desc", "Usa este comando para obtener o comprar un bloque de proteccion."),
    GET_GOTTEN("get.gotten", ChatColor.AQUA + "¡Bloque de proteccion agregado al inventario!"),
    GET_NO_PERMISSION_BLOCK("get.no_permission_block", ChatColor.RED + "No tienes permiso para obtener este bloque."),
    GET_HEADER("get.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Bloques de proteccion (click para obtener) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    GET_GUI_BLOCK("get.gui_block", ChatColor.GRAY + "> " + ChatColor.AQUA + "%alias% " + ChatColor.GRAY + "- %description% (" + ChatColor.WHITE + "$%price%" + ChatColor.GRAY + ")"),
    GET_GUI_HOVER("get.gui_hover", "¡Click para comprar un %alias%!"),

    // ps give
    GIVE_HELP("give.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps give [bloque] [jugador] [cantidad (opcional)]"),
    GIVE_HELP_DESC("give.help_desc", "Usa este comando para darle a un jugador un bloque de proteccion."),
    GIVE_GIVEN("give.given", ChatColor.GRAY + "Le diste " + ChatColor.AQUA + "%block%" + ChatColor.GRAY + " a " + ChatColor.AQUA + "%player%" + ChatColor.GRAY + "."),
    GIVE_NO_INVENTORY_ROOM("give.no_inventory_room", ChatColor.RED + "El jugador no tiene suficiente espacio en el inventario."),

    // ps sethome
    SETHOME_HELP("sethome.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps sethome"),
    SETHOME_HELP_DESC("sethome.help_desc", "Usa este comando para establecer el home de una region a donde estas ahora."),
    SETHOME_SET("sethome.set", ChatColor.GRAY + "El home de " + ChatColor.AQUA + "%psid%" + ChatColor.GRAY + " ha sido establecido en tu ubicacion."),

    // ps list
    LIST_HELP("list.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps list [jugador (opcional)]"),
    LIST_HELP_DESC("list.help_desc", "Usa este comando para listar las regiones que posees tu u otro jugador."),
    LIST_HEADER("list.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Regiones de %player% " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    LIST_OWNER("list.owner", ChatColor.GRAY + "Dueno de:"),
    LIST_MEMBER("list.member", ChatColor.GRAY + "Miembro de:"),
    LIST_NO_REGIONS("list.no_regions", ChatColor.GRAY + "Actualmente no posees ni eres miembro de ninguna region."),
    LIST_NO_REGIONS_PLAYER("list.no_regions_player", ChatColor.AQUA + "%player% " + ChatColor.GRAY + "no posee ni es miembro de ninguna region."),

    // ps name
    NAME_HELP("name.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps name [nombre|none]"),
    NAME_HELP_DESC("name.help_desc", "Usa este comando para darle un apodo a tu region, para facilitar su identificacion."),
    NAME_REMOVED("name.removed", ChatColor.GRAY + "Se elimino el nombre de %id%."),
    NAME_SET_NAME("name.set_name", ChatColor.GRAY + "Se establecio el nombre de %id% a " + ChatColor.AQUA + "%name%" + ChatColor.GRAY + "."),
    NAME_TAKEN("name.taken", ChatColor.GRAY + "¡El nombre de region " + ChatColor.AQUA + "%name%" + ChatColor.GRAY + " ya esta en uso! Prueba con otro."),

    // ps transfer
    TRANSFER_HELP("transfer.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps transfer [jugador]"),
    TRANSFER_HELP_DESC("transfer.help_desc", "Transfiere la proteccion en la que estas a otro jugador."),
    NO_PERMISSION_TRANSFER("no_permission_transfer", ChatColor.RED + "No tienes permiso para transferir protecciones."),
    TRANSFER_NOT_OWNER("transfer.not_owner", ChatColor.RED + "Solo el dueno de la proteccion puede transferirla."),
    TRANSFER_SELF("transfer.self", ChatColor.RED + "No puedes transferirte la proteccion a ti mismo."),
    TRANSFER_RENTED("transfer.rented", ChatColor.RED + "No puedes transferir una proteccion que esta alquilada."),
    TRANSFER_SUCCESS("transfer.success", ChatColor.GRAY + "Proteccion transferida correctamente a " + ChatColor.AQUA + "%player%" + ChatColor.GRAY + "."),
    TRANSFER_RECEIVED("transfer.received", ChatColor.AQUA + "%player%" + ChatColor.GRAY + " te ha transferido una proteccion."),

    // auto-eliminacion
    AUTO_REMOVE_WARNING("auto_remove.warning", ChatColor.RED + "Aviso: " + ChatColor.GRAY + "tu proteccion " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + " sera eliminada por inactividad en " + ChatColor.RED + "%time%" + ChatColor.GRAY + ". Entra al servidor o visita la proteccion para conservarla."),

    // ps setparent
    SETPARENT_HELP("setparent.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps setparent [region|none]"),
    SETPARENT_HELP_DESC("setparent.help_desc", "Usa este comando para permitir que esta region herede propiedades de otra region (duenos, miembros, flags, etc.)."),
    SETPARENT_SUCCESS("setparent.success", ChatColor.GRAY + "Se establecio correctamente el padre de " + ChatColor.AQUA + "%id%" + ChatColor.GRAY + " a " + ChatColor.AQUA + "%parent%" + ChatColor.GRAY + "."),
    SETPARENT_SUCCESS_REMOVE("setparent.success_remove", ChatColor.GRAY + "Se elimino correctamente el padre de " + ChatColor.AQUA + "%id%" + ChatColor.GRAY + "."),
    SETPARENT_CIRCULAR_INHERITANCE("setparent.circular_inheritance", ChatColor.RED + "Se detecto herencia circular (¿el padre ya hereda de esta region?). Padre no establecido."),

    // ps merge
    MERGE_HELP("merge.help", ChatColor.AQUA + "> " + ChatColor.GRAY + "/ps merge"),
    MERGE_HELP_DESC("merge.help_desc", "Usa este comando para fusionar la region en la que estas con otras regiones superpuestas."),
    MERGE_DISABLED("merge.disabled", "¡La fusion de regiones esta deshabilitada en la config!"),
    MERGE_MERGED("merge.merged", ChatColor.AQUA + "¡Las regiones se fusionaron correctamente!"),
    MERGE_HEADER("merge.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Fusionar %region% (click para fusionar) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    MERGE_WARNING("merge.warning", ChatColor.GRAY + "Nota: ¡Esto eliminara toda la configuracion de la region actual!"),
    MERGE_NOT_ALLOWED("merge.not_allowed", ChatColor.RED + "No tienes permitido fusionar este tipo de region protegida."),
    MERGE_INTO("merge.into", ChatColor.AQUA + "¡Esta region se superpone con otras regiones con las que puedes fusionarte!"),
    MERGE_NO_REGIONS("merge.no_region", ChatColor.GRAY + "No hay regiones superpuestas con las que fusionarse."),
    MERGE_CLICK_TO_MERGE("merge.click_to_merge", "¡Click para fusionar con %region%!"),
    MERGE_AUTO_MERGED("merge.auto_merged", ChatColor.GRAY + "La region se fusiono automaticamente con " + ChatColor.AQUA + "%region%" + ChatColor.GRAY + "."),

    // mensajes de feedback de los menus de inventario (se muestran con el prefijo ODISSEY)
    MENU_NOT_FOUND("menu.not_found", ChatColor.RED + "Menu no encontrado: " + ChatColor.WHITE + "%id%", "%id%"),
    MENU_NO_PERMISSION("menu.no_permission", ChatColor.RED + "No tienes permiso para esto."),
    MENU_NOT_IN_REGION("menu.not_in_region", ChatColor.RED + "No estas dentro de ninguna proteccion."),
    MENU_NO_ACCESS("menu.no_access", ChatColor.RED + "No tienes acceso a esta proteccion."),
    MENU_NOT_OWNER("menu.not_owner", ChatColor.RED + "No eres dueno de esta proteccion."),
    MENU_NOT_MEMBER("menu.not_member", ChatColor.RED + "No eres miembro de esta proteccion."),
    MENU_REGION_DELETED("menu.region_deleted", ChatColor.GREEN + "Proteccion eliminada: " + ChatColor.WHITE + "%region%", "%region%"),
    MENU_REGION_DELETE_FAILED("menu.region_delete_failed", ChatColor.RED + "No se pudo eliminar la proteccion."),
    MENU_HIDE_TOGGLED("menu.hide_toggled", ChatColor.GRAY + "Bloque protector: " + ChatColor.WHITE + "%state%", "%state%"),
    MENU_RENAME_PROMPT("menu.rename_prompt", ChatColor.YELLOW + "Escribe el nuevo nombre en el chat (" + ChatColor.GRAY + "cancelar" + ChatColor.YELLOW + " para cancelar):"),
    MENU_RENAME_CANCELLED("menu.rename_cancelled", ChatColor.GRAY + "Renombrado cancelado."),
    MENU_RENAME_TOO_LONG("menu.rename_too_long", ChatColor.RED + "El nombre es demasiado largo (max 32)."),
    MENU_RENAME_SUCCESS("menu.rename_success", ChatColor.GREEN + "Proteccion renombrada a " + ChatColor.WHITE + "%name%" + ChatColor.GREEN + ".", "%name%"),
    MENU_OWNER_ADDED("menu.owner_added", ChatColor.GREEN + "Anadido como co-dueno: " + ChatColor.WHITE + "%player%", "%player%"),
    MENU_MEMBER_ADDED("menu.member_added", ChatColor.GREEN + "Anadido como miembro: " + ChatColor.WHITE + "%player%", "%player%"),
    MENU_LAST_OWNER("menu.last_owner", ChatColor.RED + "No puedes quitar al ultimo dueno de la proteccion."),
    MENU_MEMBER_REMOVED("menu.member_removed", ChatColor.GREEN + "Miembro eliminado: " + ChatColor.WHITE + "%player%", "%player%"),
    MENU_NO_HOME("menu.no_home", ChatColor.RED + "Esta proteccion no tiene home establecido."),
    MENU_TP_HOME("menu.tp_home", ChatColor.GREEN + "Teletransportado al home de " + ChatColor.WHITE + "%region%", "%region%"),
    MENU_HOME_SET("menu.home_set", ChatColor.GREEN + "Home establecido en tu ubicacion actual."),
    MENU_TP_BLOCK("menu.tp_block", ChatColor.GREEN + "Teletransportado al bloque de " + ChatColor.WHITE + "%region%", "%region%"),
    MENU_NO_PERMISSION_BUY("menu.no_permission_buy", ChatColor.RED + "No tienes permiso para comprar esta proteccion."),
    MENU_CANNOT_BUY("menu.cannot_buy", ChatColor.RED + "No puedes comprar esta proteccion."),
    MENU_REFUND_NEEDS_VAULT("menu.refund_needs_vault", ChatColor.RED + "Se requiere Vault para reembolsar protecciones."),
    MENU_REFUND_FAILED("menu.refund_failed", ChatColor.RED + "No se pudo reembolsar la proteccion."),
    MENU_REFUNDED("menu.refunded", ChatColor.GRAY + "Proteccion reembolsada: " + ChatColor.WHITE + "%region%", "%region%"),
    MENU_REFUND_RECEIVED("menu.refund_received", ChatColor.GREEN + "Has recibido " + ChatColor.WHITE + "$%amount%" + ChatColor.GREEN + " por el reembolso.", "%amount%"),

    ;

    // Sube este numero para forzar que messages.yml se regenere con los textos por defecto actuales.
    public static final int MESSAGES_VERSION = 3;

    private final String path;
    private final String defaultMessage;

    private final String[] placeholders;
    private final int placeholdersCount;
    private String message;
    private boolean isEmpty;

    private static final File conf = new File(OdisseyProtecciones.getInstance().getDataFolder(), "messages.yml");

    PSL(String path, String defaultMessage, String... placeholders) {
        this.path = path;
        this.defaultMessage = defaultMessage;

        this.placeholders = placeholders;
        this.placeholdersCount = placeholders.length;
        this.message = defaultMessage;
        this.isEmpty = message.isEmpty();
    }

    public String msg() {
        return message;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    @Nullable
    public String format(final Object... args) {
        if (isEmpty) {
            return null;
        }

        if (this.placeholdersCount == 0) {
            return this.message;
        }

        if (this.placeholdersCount != args.length) {
            throw new IllegalArgumentException("Expected " + this.placeholdersCount + " arguments but got " + args.length);
        }

        return StringUtils.replaceEach(
                this.message,
                this.placeholders,
                Arrays.stream(args).map(a -> a == null ? "" : a.toString()).toArray(String[]::new)
        );
    }

    public boolean send(@NotNull final CommandSender receiver, @NotNull final Object... args) {
        final String msg = this.format(args);

        if (msg != null) {
            receiver.sendMessage(conPrefijo(msg));
        }

        return true;
    }

    /**
     * Antepone el prefijo de identidad ODISSEY a las lineas de feedback simples.
     * Se omite en bloques multilinea (ayuda/info) y en lineas de separadores/listas
     * (que empiezan por {@code =}, {@code -}, {@code >}, {@code ·} o {@code #}) para no romper su formato.
     */
    public static String conPrefijo(String msg) {
        if (msg == null || msg.isEmpty()) return msg;
        if (msg.indexOf('\n') >= 0) return msg;
        String plain = ChatColor.stripColor(msg).trim();
        if (plain.isEmpty()) return msg;
        char c = plain.charAt(0);
        if (c == '=' || c == '-' || c == '>' || c == '#' || c == '\u00b7') return msg;
        return es.odissey.protecciones.utils.Chat.prefijo() + msg;
    }

    public void append(@NotNull final StringBuilder builder, @NotNull final Object... args) {
        final String msg = this.format(args);

        if (msg != null) {
            builder.append(msg);
        }
    }

    // Sends a message to a commandsender if the string is not empty

    public static boolean msg(CommandSender p, String str) {
        if (str != null && !str.isEmpty() && p != null) {
            p.sendMessage(conPrefijo(str));
        }
        return true;
    }

    public static boolean msg(PSPlayer p, String str) {
        return msg(p.getPlayer(), str);
    }

    public static void loadConfig() {
        YamlConfiguration yml = new YamlConfiguration();

        // check if messages.yml exists
        if (!conf.exists()) {
            try {
                conf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // load config
        try {
            yml.load(conf); // can throw error

            // Si el archivo es de una version anterior, se hace una copia de seguridad y se
            // anaden unicamente las claves que falten, para NO borrar las traducciones del usuario.
            boolean desactualizado = yml.getInt("messages_version", 0) < MESSAGES_VERSION;
            if (desactualizado && conf.exists() && conf.length() > 0) {
                try {
                    Files.copy(conf.toPath(),
                            new File(conf.getParentFile(), "messages.yml.old").toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (PSL psl : PSL.values()) {

                // fix message if need be
                if (yml.getString(psl.path) == null) { // if msg not found in config
                    yml.set(psl.path, applyConfigColours(psl.defaultMessage));
                } else {
                    // perform message upgrades
                    messageUpgrades(psl, yml);
                }

                // load message
                psl.message = applyInGameColours(yml.getString(psl.path));
                psl.isEmpty = psl.message.isEmpty();
            }

            yml.set("messages_version", MESSAGES_VERSION);
            try {
                yml.save(conf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) { // prevent bad messages.yml file from resetting the file
            e.printStackTrace();
        }
    }

    // message upgrades over time
    private static void messageUpgrades(PSL psl, YamlConfiguration yml) {
        String value = yml.getString(psl.path);
        assert(value != null);

        // psl upgrade conversions
        if (psl == PSL.REACHED_REGION_LIMIT && value.equals("&cYou can not create any more protected regions.")) {
            yml.set(psl.path, psl.defaultMessage);
        } else if (psl == PSL.REACHED_PER_BLOCK_REGION_LIMIT && value.equals("&cYou can not create any more regions of this type.")) {
            yml.set(psl.path, psl.defaultMessage);
        } else if (value.contains("§")) {
            yml.set(psl.path, applyConfigColours(value));
        }
    }

    // match all &#123abc format for hex
    private static final Pattern hexPattern = Pattern.compile("(?<!\\\\\\\\)(&#[a-fA-F0-9]{6})");

    private static String applyInGameColours(String msg) {

        Matcher matcher = hexPattern.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start() + 1, matcher.end());
            msg = msg.replace(msg.substring(matcher.start(), matcher.end()), "" + net.md_5.bungee.api.ChatColor.of(color));
        }

        return msg.replace('&', '§');
    }

    private static String applyConfigColours(String msg) {
        return msg.replace('§', '&');
    }
}
