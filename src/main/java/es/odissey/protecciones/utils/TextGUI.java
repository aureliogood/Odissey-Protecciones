package es.odissey.protecciones.utils;

import es.odissey.protecciones.PSL;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TextGUI {

    // page starts at zero, but displays start at one
    // pageCommand will be replacing %page%
    public static void displayGUI(CommandSender s, String header, String pageCommand, int currentPage, int guiSize, List<TextComponent> lines, boolean sendBlankLines) {
        int currentLine = currentPage * guiSize;
        if (currentPage < 0 || currentLine > lines.size()) {
            return;
        }

        PSL.msg(s, header);

        for (int i = currentPage*guiSize; i < Math.min((currentPage+1) * guiSize, lines.size()); i++) {
            if (sendBlankLines || !lines.get(i).equals(new TextComponent("")))
                s.spigot().sendMessage(lines.get(i));
        }

        // footer page buttons
        TextComponent backPage = new TextComponent(ChatColor.AQUA + " <<"), nextPage = new TextComponent(ChatColor.AQUA + ">> ");
        backPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(PSL.GO_BACK_PAGE.msg()).create()));
        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(PSL.GO_NEXT_PAGE.msg()).create()));
        backPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pageCommand.replace("%page%", ""+currentPage)));
        nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pageCommand.replace("%page%", currentPage+2+"")));

        TextComponent footer = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET);
        // add back page button if the page isn't 0
        if (currentPage != 0) footer.addExtra(backPage);
        // add page number
        footer.addExtra(new TextComponent(ChatColor.WHITE + " " + (currentPage + 1) + " "));
        // add forward page button if the page isn't last
        if (currentPage * guiSize + guiSize < lines.size()) footer.addExtra(nextPage);
        footer.addExtra(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====");

        // display footer only if there are more than one page of entries
        if (lines.size() >= guiSize) s.spigot().sendMessage(footer);
    }

}
