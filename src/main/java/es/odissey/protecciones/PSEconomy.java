package es.odissey.protecciones;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import es.odissey.protecciones.utils.MiscUtil;
import es.odissey.protecciones.utils.WGUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Handler for OdisseyProtecciones economy related tasks.
 */

public class PSEconomy {
    private List<PSRegion> rentedList = new CopyOnWriteArrayList<>();
    private static int rentRunner = -1, taxRunner = -1;
    private static final Map<String, Long> rentWarnings = new ConcurrentHashMap<>();

    public PSEconomy() {
        if (!OdisseyProtecciones.getInstance().isVaultSupportEnabled()) {
            OdisseyProtecciones.getInstance().getLogger().warning("Vault no esta habilitado! Las funciones de economia (alquiler y compra) no funcionaran!");
            return;
        }
        // find regions that are being rented out (called on startup or reload)
        loadRentList();

        // start rent
        rentRunner = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), this::updateRents, 0, 200).getTaskId();

        // start taxes
        if (OdisseyProtecciones.getInstance().getConfigOptions().taxEnabled)
            taxRunner = Bukkit.getScheduler().runTaskTimer(OdisseyProtecciones.getInstance(), this::updateTaxes, 0, 200).getTaskId();
    }

    private synchronized void updateRents() {
        // iterate over a snapshot; removeRenting() may remove entries from the list
        for (PSRegion r : new ArrayList<>(rentedList)) {
            if (r.getTypeOptions() == null || r.getRentStage() != PSRegion.RentStage.RENTING) {
                rentedList.remove(r);
                rentWarnings.remove(r.getId());
                continue;
            }
            try {
                Duration rentPeriod = MiscUtil.parseRentPeriod(r.getRentPeriod());
                long end = r.getRentLastPaid() + rentPeriod.getSeconds();
                long remaining = end - Instant.now().getEpochSecond();
                // if tenant needs to pay
                if (remaining <= 0) {
                    doRentPayment(r);
                } else {
                    checkRentWarning(r, remaining);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void updateTaxes() {
        // Este metodo ya corre en el hilo principal: procesa los impuestos en linea en vez de
        // programar una tarea nueva por cada region (que saturaba la cola del hilo principal).
        WGUtils.getAllRegionManagers()
                .forEach((w, rgm) -> {
                    for (ProtectedRegion r : rgm.getRegions().values()) {
                        if (OdisseyProtecciones.isPSRegion(r)) {
                            PSRegion psr = PSRegion.fromWGRegion(w, r);
                            processTaxesSync(psr);
                        }
                    }
                });
    }

    /**
     * Stops the economy cycle. Used for reloads when creating a new PSEconomy.
     */
    public void stop() {
        if (rentRunner != -1) {
            Bukkit.getScheduler().cancelTask(rentRunner);
            rentRunner = -1;
        }
        if (taxRunner != -1) {
            Bukkit.getScheduler().cancelTask(taxRunner);
            taxRunner = -1;
        }
    }

    /**
     * Load list of regions that are rented into memory.
     */

    public void loadRentList() {
        rentedList = new CopyOnWriteArrayList<>();

        HashMap<World, RegionManager> managers = WGUtils.getAllRegionManagers();

        for (World w : managers.keySet()) {
            RegionManager rgm = managers.get(w);
            for (ProtectedRegion pr : rgm.getRegions().values()) {
                if (OdisseyProtecciones.isPSRegion(pr)) {
                    rentedList.add(PSRegion.fromWGRegion(w, pr));
                }
            }
        }
    }

    /**
     * Process taxes for a region.
     *
     * @param r the region to process taxes for
     */
    public static void processTaxes(PSRegion r) {
        // si los impuestos no estan activos para esta region, no se hace nada
        if (r.getTypeOptions() == null || r.getTypeOptions().taxPeriod == -1) return;
        Bukkit.getScheduler().runTask(OdisseyProtecciones.getInstance(), () -> processTaxesSync(r));
    }

    /**
     * Procesa los impuestos de una region. Debe llamarse en el hilo principal (accede a WorldGuard).
     *
     * @param r la region a procesar
     */
    public static void processTaxesSync(PSRegion r) {
        if (r.getTypeOptions() == null || r.getTypeOptions().taxPeriod == -1) return;

        // update tax payments due
        r.updateTaxPayments();

        // check if a player is set to auto-pay
        if (!r.getTaxPaymentsDue().isEmpty() && r.getTaxAutopayer() != null) {
            PSPlayer psp = PSPlayer.fromUUID(r.getTaxAutopayer());
            EconomyResponse res = r.payTax(psp, psp.getBalance());

            if (res != null && res.transactionSuccess() && psp.getPlayer() != null && res.amount != 0) {
                PSL.msg(psp.getPlayer(), PSL.TAX_PAID.msg()
                        .replace("%amount%", String.format("%.2f", res.amount))
                        .replace("%region%", r.getName() == null ? r.getId() : r.getName() + " (" + r.getId() + ")"));
            }
        }

        // late tax payment punishment
        if (r.isTaxPaymentLate()) {
            deleteRegionForTaxes(r);
        }
    }

    /**
     * Borra la region por impago devolviendo el bloque al suelo (antes se borraba sin
     * devolver nada, lo que hacia perder la proteccion al jugador).
     */
    private static void deleteRegionForTaxes(PSRegion r) {
        PSProtectBlock options = r.getTypeOptions();
        List<ItemStack> drops = new ArrayList<>();
        List<Location> dropLocations = new ArrayList<>();

        Location root = blockLocation(r);
        if (root != null) {
            dropLocations.add(root);
            if (options != null && !options.noDrop) drops.add(options.createItem());
        }

        if (r instanceof PSGroupRegion group) {
            for (PSMergedRegion merged : group.getMergedRegions()) {
                if (merged.getId().equals(r.getId())) continue;
                PSProtectBlock mergedOptions = merged.getTypeOptions();
                Location loc = blockLocation(merged);
                if (loc != null) {
                    dropLocations.add(loc);
                    if (mergedOptions != null && !mergedOptions.noDrop) drops.add(mergedOptions.createItem());
                }
            }
        }

        if (!r.deleteRegion(true)) return;

        for (int i = 0; i < drops.size(); i++) {
            Location where = i < dropLocations.size() ? dropLocations.get(i) : root;
            if (where == null || where.getWorld() == null) continue;
            where.getWorld().dropItemNaturally(where.clone().add(0.5, 0.5, 0.5), drops.get(i));
        }
    }

    // posicion del bloque sin cargar chunks (se calcula desde el id de la region)
    private static Location blockLocation(PSRegion r) {
        try {
            PSLocation psl = WGUtils.parsePSRegionToLocation(r.getId());
            if (psl == null) return null;
            return new Location(r.getWorld(), psl.x, psl.y, psl.z);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Avisa al inquilino (y al propietario) cuando queda poco para el siguiente cobro.
     */
    private static void checkRentWarning(PSRegion r, long remainingSeconds) {
        if (r.getTenant() == null || r.getLandlord() == null) return;

        PSConfig c = OdisseyProtecciones.getInstance().getConfigOptions();
        List<Integer> thresholds = c.economyRentWarningSeconds == null
                ? Arrays.asList(3600, 86400) : new ArrayList<>(c.economyRentWarningSeconds);
        thresholds.sort(Integer::compareTo);

        long level = 0;
        for (int threshold : thresholds) {
            if (remainingSeconds <= threshold) {
                level = threshold;
                break;
            }
        }
        if (level <= 0) {
            rentWarnings.remove(r.getId());
            return;
        }
        Long previous = rentWarnings.get(r.getId());
        if (previous != null && previous == level) return;
        rentWarnings.put(r.getId(), level);

        String time = formatDuration(remainingSeconds);
        String regionName = r.getName() == null ? r.getId() : r.getName();

        if (Bukkit.getPlayer(r.getTenant()) != null) {
            PSL.msg(Bukkit.getPlayer(r.getTenant()), PSL.RENT_EXPIRING_TENANT.msg()
                    .replace("%region%", regionName)
                    .replace("%time%", time)
                    .replace("%price%", String.format("%.2f", r.getPrice())));
        }
        if (Bukkit.getPlayer(r.getLandlord()) != null) {
            PSL.msg(Bukkit.getPlayer(r.getLandlord()), PSL.RENT_EXPIRING_LANDLORD.msg()
                    .replace("%region%", regionName)
                    .replace("%time%", time)
                    .replace("%tenant%", PSPlayer.fromUUID(r.getTenant()).getName()));
        }
    }

    private static String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return Math.max(1, minutes) + "m";
    }

    /**
     * Process a rent payment for a region.
     * It does not do any checks, it is expected to check if the rent time has passed before this function is called.
     *
     * @param r the region to perform the rent payment
     */
    public static void doRentPayment(PSRegion r) {
        rentWarnings.remove(r.getId());
        PSPlayer tenant = PSPlayer.fromPlayer(Bukkit.getOfflinePlayer(r.getTenant()));
        PSPlayer landlord = PSPlayer.fromPlayer(Bukkit.getOfflinePlayer(r.getLandlord()));

        // not enough money for rent or the payment failed
        if (!tenant.hasAmount(r.getPrice()) || !tenant.pay(landlord, r.getPrice())) {
            if (tenant.getOfflinePlayer().isOnline()) {
                PSL.msg(Bukkit.getPlayer(r.getTenant()), PSL.RENT_EVICT_NO_MONEY_TENANT.msg()
                        .replace("%region%", r.getName() != null ? r.getName() : r.getId())
                        .replace("%price%", String.format("%.2f", r.getPrice())));
            }
            if (landlord.getOfflinePlayer().isOnline()) {
                PSL.msg(Bukkit.getPlayer(r.getLandlord()), PSL.RENT_EVICT_NO_MONEY_LANDLORD.msg()
                        .replace("%region%", r.getName() != null ? r.getName() : r.getId())
                        .replace("%tenant%", tenant.getName()));
            }
            r.removeRenting();
            return;
        }

        // send payment messages
        if (tenant.getOfflinePlayer().isOnline()) {
            PSL.msg(Bukkit.getPlayer(r.getTenant()), PSL.RENT_PAID_TENANT.msg()
                    .replace("%price%", String.format("%.2f", r.getPrice()))
                    .replace("%landlord%", landlord.getName())
                    .replace("%region%", r.getName() != null ? r.getName() : r.getId()));
        }
        if (landlord.getOfflinePlayer().isOnline()) {
            PSL.msg(Bukkit.getPlayer(r.getLandlord()), PSL.RENT_PAID_LANDLORD.msg()
                    .replace("%price%", String.format("%.2f", r.getPrice()))
                    .replace("%tenant%", tenant.getName())
                    .replace("%region%", r.getName() != null ? r.getName() : r.getId()));
        }

        r.setRentLastPaid(Instant.now().getEpochSecond());
        try { // must save region to persist last paid
            r.getWGRegionManager().saveChanges();
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get list of rented regions.
     *
     * @return the list of rented regions
     */
    public List<PSRegion> getRentedList() {
        return rentedList;
    }
}