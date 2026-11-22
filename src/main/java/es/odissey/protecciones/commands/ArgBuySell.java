package es.odissey.protecciones.commands;

import es.odissey.protecciones.PSL;
import es.odissey.protecciones.PSPlayer;
import es.odissey.protecciones.PSRegion;
import es.odissey.protecciones.OdisseyProtecciones;
import es.odissey.protecciones.utils.LimitUtil;
import es.odissey.protecciones.utils.UUIDCache;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ArgBuySell implements PSCommandArg {
    @Override
    public List<String> getNames() {
        return Arrays.asList("buy", "sell");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("odisseyprotecciones.buysell");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        if (!p.hasPermission("odisseyprotecciones.buysell")) {
            PSL.msg(p, PSL.NO_PERMISSION_BUYSELL.msg());
            return true;
        }

        if (!OdisseyProtecciones.getInstance().isVaultSupportEnabled()) {
            Bukkit.getLogger().info(ChatColor.RED + "Se requiere Vault, pero no esta habilitado en este servidor. Contacta a un administrador.");
            s.sendMessage(ChatColor.RED + "Se requiere Vault, pero no esta habilitado en este servidor. Contacta a un administrador.");
            return true;
        }

        PSRegion r = PSRegion.fromLocationGroup(p.getLocation());
        if (r == null)
            return PSL.msg(p, PSL.NOT_IN_REGION.msg());

        if (args[0].equals("buy")) { // buying

            if (!r.forSale())
                return PSL.msg(p, PSL.BUY_NOT_FOR_SALE.msg());

            // stale listing: for sale but no landlord
            if (r.getLandlord() == null) {
                r.setSellable(false, null, 0);
                return PSL.msg(p, PSL.BUY_NOT_FOR_SALE.msg());
            }

            if ((!r.getTypeOptions().permission.equals("") && !p.hasPermission(r.getTypeOptions().permission)))
                return PSL.msg(p, PSL.NO_PERMISSION_REGION_TYPE.msg());

            // check if player reached region limit
            if (!LimitUtil.check(p, r.getTypeOptions()))
                return PSL.msg(p, PSL.REACHED_REGION_LIMIT.msg().replace("%limit%", "" + PSPlayer.fromPlayer(p).getGlobalRegionLimits()));

            if (!PSPlayer.fromPlayer(p).hasAmount(r.getPrice()))
                return PSL.msg(p, PSL.NOT_ENOUGH_MONEY.msg().replace("%price%", new DecimalFormat("#.##").format(r.getPrice())));

            PSL.msg(p, PSL.BUY_SOLD_BUYER.msg()
                    .replace("%region%", r.getName() == null ? r.getId() : r.getName())
                    .replace("%price%", String.format("%.2f", r.getPrice()))
                    .replace("%player%", UUIDCache.getNameFromUUID(r.getLandlord())));

            if (Bukkit.getPlayer(r.getLandlord()) != null) {
                PSL.msg(Bukkit.getPlayer(r.getLandlord()), PSL.BUY_SOLD_SELLER.msg()
                        .replace("%region%", r.getName() == null ? r.getId() : r.getName())
                        .replace("%price%", String.format("%.2f", r.getPrice()))
                        .replace("%player%", p.getName()));
            }

            r.sell(p.getUniqueId());

        } else if (args[0].equals("sell")) { // selling

            if (!r.isOwner(p.getUniqueId()))
                return PSL.msg(p, PSL.NOT_OWNER.msg());

            if (args.length != 2)
                return PSL.msg(p, PSL.SELL_HELP.msg());

            if (r.getRentStage() != PSRegion.RentStage.NOT_RENTING)
                return PSL.msg(p, PSL.SELL_RENTED_OUT.msg());

            if (args[1].equals("stop")) {
                r.setSellable(false, null, 0);
                PSL.msg(p, PSL.BUY_STOP_SELL.msg());
            } else {
                if (!NumberUtils.isNumber(args[1]))
                    return PSL.msg(p, PSL.SELL_HELP.msg());

                PSL.msg(p, PSL.SELL_FOR_SALE.msg().replace("%price%", String.format("%.2f", Double.parseDouble(args[1]))));
                r.setSellable(true, p.getUniqueId(), Double.parseDouble(args[1]));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
